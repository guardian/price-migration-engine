package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter.{NotificationSendComplete, NotificationSendDateWrittenToSalesforce}
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.{Clock, IO, ZIO}

import java.time.{LocalDate, ZoneOffset}

object SalesforceNotificationDateUpdateHandler extends CohortHandler {

  val main: ZIO[Logging with CohortTable with SalesforceClient, Failure, HandlerOutput] =
    for {
      _ <- CohortTable.fetch(NotificationSendComplete, None).foreach(updateDateLetterSentInSF)
    } yield HandlerOutput(isComplete = true)

  private def updateDateLetterSentInSF(
      item: CohortItem
  ): ZIO[Logging with CohortTable with SalesforceClient, Failure, Unit] =
    for {
      _ <- updateSalesforce(item)
        .tapBoth(
          e => Logging.error(s"Failed to write create Price_Rise in salesforce: $e"),
          result => Logging.info(s"SalesforcePriceRise result: $result")
        )
      now <- Clock.instant
      salesforcePriceRiseDetails = CohortItem(
        subscriptionName = item.subscriptionName,
        processingStage = NotificationSendDateWrittenToSalesforce,
        whenNotificationSentWrittenToSalesforce = Some(now)
      )
      _ <-
        CohortTable
          .update(salesforcePriceRiseDetails)
    } yield ()

  private def updateSalesforce(
      cohortItem: CohortItem
  ): ZIO[SalesforceClient, Failure, Option[String]] = {
    for {
      priceRise <- buildPriceRise(cohortItem)
      salesforcePriceRiseId <-
        ZIO
          .fromOption(cohortItem.salesforcePriceRiseId)
          .orElseFail(
            SalesforcePriceRiseWriteFailure(
              "CohortItem.salesforcePriceRiseId is required to update salesforce"
            )
          )
      result <-
        SalesforceClient
          .updatePriceRise(salesforcePriceRiseId, priceRise)
          .as(None)
    } yield result
  }

  def buildPriceRise(
      cohortItem: CohortItem
  ): IO[SalesforcePriceRiseWriteFailure, SalesforcePriceRise] = {
    for {
      notificationSendTimestamp <-
        ZIO
          .fromOption(cohortItem.whenNotificationSent)
          .orElseFail(SalesforcePriceRiseWriteFailure(s"$cohortItem does not have a whenEmailSent field"))
    } yield SalesforcePriceRise(
      Date_Letter_Sent__c = Some(LocalDate.from(notificationSendTimestamp.atOffset(ZoneOffset.UTC)))
    )
  }

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] =
    main.provideSome[Logging](
      EnvConfig.cohortTable.layer,
      EnvConfig.salesforce.layer,
      EnvConfig.stage.layer,
      DynamoDBZIOLive.impl,
      DynamoDBClientLive.impl,
      CohortTableLive.impl(input),
      SalesforceClientLive.impl
    )
}
