package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter.{NotificationSendComplete, NotificationSendDateWrittenToSalesforce}
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.{Clock, IO, ZIO}

import java.time.{LocalDate, ZoneOffset}

object SalesforceNotificationDateUpdateHandler extends CohortHandler {

  def main(cohortSpec: CohortSpec): ZIO[Logging with CohortTable with SalesforceClient, Failure, HandlerOutput] =
    for {
      _ <- CohortTable.fetch(NotificationSendComplete, None).foreach(item => updateDateLetterSentInSF(cohortSpec, item))
    } yield HandlerOutput(isComplete = true)

  private def updateDateLetterSentInSF(
      cohortSpec: CohortSpec,
      item: CohortItem
  ): ZIO[Logging with CohortTable with SalesforceClient, Failure, Unit] =
    for {
      _ <- updateSalesforce(cohortSpec, item)
        .tapBoth(
          e => Logging.error(s"Failed to write create Price_Rise in salesforce: $e"),
          result => Logging.info(s"SalesforcePriceRise result: $result")
        )
      now <- Clock.instant
      newCohortItem = CohortItem(
        subscriptionName = item.subscriptionName,
        processingStage = NotificationSendDateWrittenToSalesforce,
        whenNotificationSentWrittenToSalesforce = Some(now)
      )
      _ <-
        CohortTable
          .update(newCohortItem)
    } yield ()

  private def updateSalesforce(
      cohortSpec: CohortSpec,
      cohortItem: CohortItem
  ): ZIO[SalesforceClient, Failure, Option[String]] = {
    for {
      priceRise <- buildPriceRise(cohortSpec, cohortItem)
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
      cohortSpec: CohortSpec,
      cohortItem: CohortItem
  ): IO[SalesforcePriceRiseWriteFailure, SalesforcePriceRise] = {
    for {
      notificationSendTimestamp <-
        ZIO
          .fromOption(cohortItem.whenNotificationSent)
          .orElseFail(SalesforcePriceRiseWriteFailure(s"$cohortItem does not have a whenNotificationSent field"))
    } yield SalesforcePriceRise(
      Date_Letter_Sent__c = Some(LocalDate.from(notificationSendTimestamp.atOffset(ZoneOffset.UTC))),
      Migration_Name__c = Some(cohortSpec.cohortName),
      Migration_Status__c = Some("NotificationSendComplete"),
      Cancellation_Reason__c = None
    )
  }

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] = {
    MigrationType(input) match {
      case Newspaper2025P1  => ZIO.succeed(HandlerOutput(isComplete = true))
      case HomeDelivery2025 => ZIO.succeed(HandlerOutput(isComplete = true))
      case _ => {
        main(input).provideSome[Logging](
          EnvConfig.cohortTable.layer,
          EnvConfig.salesforce.layer,
          EnvConfig.stage.layer,
          DynamoDBZIOLive.impl,
          DynamoDBClientLive.impl,
          CohortTableLive.impl(input),
          SalesforceClientLive.impl
        )
      }
    }
  }
}
