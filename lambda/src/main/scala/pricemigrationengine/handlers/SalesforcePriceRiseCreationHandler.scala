package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.CohortTableFilter.{EstimationComplete, SalesforcePriceRiceCreationComplete}
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.clock.Clock
import zio.console.Console
import zio.{App, IO, Runtime, ZEnv, ZIO, ZLayer, clock, console}

object SalesforcePriceRiseCreationHandler extends App with RequestHandler[Unit, Unit] {

  val main: ZIO[Logging with CohortTable with SalesforceClient with Clock, Failure, Unit] =
    for {
      cohortItems <- CohortTable.fetch(EstimationComplete, None)
      _ <- cohortItems.foreach(createSalesforcePriceRise)
    } yield ()

  private def createSalesforcePriceRise(
      item: CohortItem
  ): ZIO[Logging with CohortTable with SalesforceClient with Clock, Failure, Unit] =
    for {
      optionalNewPriceRiseId <- updateSalesforce(item)
        .tapBoth(
          e => Logging.error(s"Failed to write create Price_Rise in salesforce: $e"),
          result => Logging.info(s"SalesforcePriceRise result: $result")
        )
      time <- clock.currentDateTime
        .mapError { error =>
          SalesforcePriceRiseCreationFailure(s"Failed to get currentTime: $error")
        }
      salesforcePriceRiseCreationDetails = CohortItem(
        subscriptionName = item.subscriptionName,
        processingStage = SalesforcePriceRiceCreationComplete,
        salesforcePriceRiseId = optionalNewPriceRiseId,
        whenSfShowEstimate = Some(time.toInstant)
      )
      _ <- CohortTable
        .update(salesforcePriceRiseCreationDetails)
        .tapBoth(
          e => Logging.error(s"Failed to update Cohort table: $e"),
          _ => Logging.info(s"Wrote $salesforcePriceRiseCreationDetails to Cohort table")
        )
    } yield ()

  private def updateSalesforce(
    cohortItem: CohortItem
  ): ZIO[SalesforceClient, Failure, Option[String]] = {
    for {
      subscription <- SalesforceClient.getSubscriptionByName(cohortItem.subscriptionName)
      priceRise <- buildPriceRise(cohortItem, subscription)
      result <- cohortItem
        .salesforcePriceRiseId
        .fold(
          SalesforceClient
            .createPriceRise(priceRise)
            .map[Option[String]](response => Some(response.id))
        ) { priceRiseId =>
          SalesforceClient
            .updatePriceRise(priceRiseId, priceRise)
            .map(_ => None)
        }
    } yield result
  }

  def buildPriceRise(
      cohortItem: CohortItem,
      subscription: SalesforceSubscription
  ): IO[SalesforcePriceRiseCreationFailure, SalesforcePriceRise] = {
    for {
      currentPrice <- ZIO
        .fromOption(cohortItem.oldPrice)
        .orElseFail(SalesforcePriceRiseCreationFailure(s"$cohortItem does not have an oldPrice"))
      newPrice <- ZIO
        .fromOption(cohortItem.estimatedNewPrice)
        .orElseFail(SalesforcePriceRiseCreationFailure(s"$cohortItem does not have an estimatedNewPrice"))
      priceRiseDate <- ZIO
        .fromOption(cohortItem.startDate)
        .orElseFail(SalesforcePriceRiseCreationFailure(s"$cohortItem does not have a startDate"))
    } yield
      SalesforcePriceRise(
        subscription.Buyer__c,
        currentPrice,
        newPrice,
        priceRiseDate,
        subscription.Id
      )
  }

  private def env(
      loggingLayer: ZLayer[Any, Nothing, Logging]
  ): ZLayer[Any, Any, Logging with CohortTable with SalesforceClient with Clock] = {
    loggingLayer ++ EnvConfiguration.dynamoDbImpl >>>
      DynamoDBClient.dynamoDB ++ loggingLayer >>>
      DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.cohortTableImp ++ EnvConfiguration.stageImp ++ EnvConfiguration.salesforceImp >>>
      loggingLayer ++ CohortTableLive.impl ++ SalesforceClientLive.impl ++ Clock.live
  }

  private val runtime = Runtime.default

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    main
      .provideSomeLayer(
        env(Console.live >>> ConsoleLogging.impl)
      )
      .foldM(
        e => console.putStrLn(s"Failed: $e") *> ZIO.succeed(1),
        _ => console.putStrLn("Succeeded!") *> ZIO.succeed(0)
      )

  def handleRequest(unused: Unit, context: Context): Unit =
    runtime.unsafeRun(
      main.provideSomeLayer(
        env(LambdaLogging.impl(context))
      )
    )
}
