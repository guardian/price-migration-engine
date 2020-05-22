package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.CohortTableFilter.EstimationComplete
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.clock.Clock
import zio.console.Console
import zio.{App, IO, Runtime, ZEnv, ZIO, ZLayer, clock, console}



object SalesforcePriceRiseCreationHandler extends App with RequestHandler[Unit, Unit] {

  val main: ZIO[Logging with CohortTable with SalesforceClient with Clock, Failure, Unit] =
    for {
      cohortItems <- CohortTable.fetch(EstimationComplete)
      _ <- cohortItems.foreach(createSalesforcePriceRise)
    } yield ()

  private def createSalesforcePriceRise(
    item: CohortItem
  ): ZIO[Logging with CohortTable with SalesforceClient with Clock, Failure, Unit] =
    for {
      updateResponse <- updateSalesforce(item)
        .tapBoth(
          e => Logging.error(s"Failed to write create Price_Rise in salesforce: $e"),
          result => Logging.info(s"SalesforcePriceRise result: $result")
        )
      time <- clock.currentDateTime
        .mapError { error =>
          SalesforcePriceRiseCreationFailure(s"Failed to get currentTime: $error")
        }
      salesforcePriceRiseCreationDetails = SalesforcePriceRiseCreationDetails(updateResponse.id, time.toInstant)
      _ <- CohortTable
        .update(item.subscriptionName, salesforcePriceRiseCreationDetails)
        .tapBoth(
          e => Logging.error(s"Failed to update Cohort table: $e"),
          _ => Logging.info(s"Wrote $salesforcePriceRiseCreationDetails to Cohort table")
        )
    } yield ()

  private def updateSalesforce(cohortItem: CohortItem): ZIO[SalesforceClient, Failure, SalesforcePriceRiseCreationResponse] = {
    for {
      subscription <- SalesforceClient.getSubscriptionByName(cohortItem.subscriptionName)
      priceRise <- buildPriceRise(cohortItem, subscription)
      result <- SalesforceClient.createPriceRise(priceRise)
    } yield result
  }

  def buildPriceRise(
    cohortItem: CohortItem,
    subscription: SalesforceSubscription
  ): IO[SalesforcePriceRiseCreationFailure, SalesforcePriceRise] = {
    for {
      currentPrice <-
        ZIO.fromOption(cohortItem.oldPrice).mapError(_ => SalesforcePriceRiseCreationFailure(s"$cohortItem does not have an oldPrice"))
      newPrice <-
        ZIO.fromOption(cohortItem.estimatedNewPrice).mapError(_ => SalesforcePriceRiseCreationFailure(s"$cohortItem does not have an estimatedNewPrice"))
      priceRiseDate <-
        ZIO.fromOption(cohortItem.expectedStartDate).mapError(_ => SalesforcePriceRiseCreationFailure(s"$cohortItem does not have an expectedStartDate"))
    } yield SalesforcePriceRise(
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
      DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.cohortTableImp ++ EnvConfiguration.salesforceImp >>>
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
