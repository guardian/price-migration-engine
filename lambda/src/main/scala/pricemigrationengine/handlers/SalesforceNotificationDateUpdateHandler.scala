package pricemigrationengine.handlers

import java.time.{LocalDate, ZoneOffset}

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.CohortTableFilter.{NotificationSendComplete, NotificationSendDateWrittenToSalesforce}
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.clock.Clock
import zio.console.Console
import zio.{App, ExitCode, IO, Runtime, ZEnv, ZIO, ZLayer, clock}

object SalesforceNotificationDateUpdateHandler extends App with RequestHandler[Unit, Unit] {

  val main: ZIO[Logging with CohortTable with SalesforceClient with Clock, Failure, Unit] =
    for {
      cohortItems <- CohortTable.fetch(NotificationSendComplete, None)
      _ <- cohortItems.foreach(updateDateLetterSentInSF)
    } yield ()

  private def updateDateLetterSentInSF(
      item: CohortItem
  ): ZIO[Logging with CohortTable with SalesforceClient with Clock, Failure, Unit] =
    for {
      _ <- updateSalesforce(item)
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
        processingStage = NotificationSendDateWrittenToSalesforce,
        whenNotificationSentWrittenToSalesforce = Some(time.toInstant)
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
      priceRise <- buildPriceRise(cohortItem)
      salesforcePriceRiseId <- IO
        .fromOption(cohortItem.salesforcePriceRiseId)
        .mapError { _ =>
          SalesforcePriceRiseCreationFailure(
            "CohortItem.salesforcePriceRiseId is required to update salesforce"
          )
        }
      result <- SalesforceClient
        .updatePriceRise(salesforcePriceRiseId, priceRise)
        .as(None)
    } yield result
  }

  def buildPriceRise(
      cohortItem: CohortItem
  ): IO[SalesforcePriceRiseCreationFailure, SalesforcePriceRise] = {
    for {
      notificationSendTimestamp <- ZIO
        .fromOption(cohortItem.whenNotificationSent)
        .orElseFail(SalesforcePriceRiseCreationFailure(s"$cohortItem does not have a whenEmailSent field"))
    } yield
      SalesforcePriceRise(
        Date_Letter_Sent__c = Some(LocalDate.from(notificationSendTimestamp.atOffset(ZoneOffset.UTC)))
      )
  }

  private def env(
      loggingService: Logging.Service
  ): ZLayer[Any, Any, Logging with CohortTable with SalesforceClient with Clock] = {
    val loggingLayer = ZLayer.succeed(loggingService)
    loggingLayer ++ EnvConfiguration.dynamoDbImpl >>>
      DynamoDBClient.dynamoDB ++ loggingLayer >>>
      DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.cohortTableImp ++ EnvConfiguration.stageImp ++ EnvConfiguration.salesforceImp >>>
      (loggingLayer ++ CohortTableLive.impl ++ SalesforceClientLive.impl ++ Clock.live)
        .tapError(e => loggingService.error(s"Failed to create service environment: $e"))
  }

  private val runtime = Runtime.default

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    main
      .provideSomeLayer(
        env(ConsoleLogging.service(Console.Service.live))
      )
      .exitCode

  def handleRequest(unused: Unit, context: Context): Unit =
    runtime.unsafeRun(
      main.provideSomeLayer(
        env(LambdaLogging.service(context))
      )
    )
}
