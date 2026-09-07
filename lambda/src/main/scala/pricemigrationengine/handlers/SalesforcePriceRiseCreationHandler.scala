package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter.{EstimationComplete, SalesforcePriceRiseCreationComplete}
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.{Clock, IO, ZIO}

object SalesforcePriceRiseCreationHandler extends CohortHandler {

  private val batchSize = 1000

  private[handlers] def main(
      cohortSpec: CohortSpec
  ): ZIO[Logging with CohortTable with Salesforce, Failure, HandlerOutput] =
    for {
      count <- CohortTable
        .fetch(EstimationComplete, None)
        .take(batchSize)
        .mapZIO(i => createSalesforcePriceRise(cohortSpec, i))
        .runCount
    } yield HandlerOutput(isComplete = count < batchSize)

  private def createSalesforcePriceRise(
      cohortSpec: CohortSpec,
      item: CohortItem
  ): ZIO[Logging with CohortTable with Salesforce, Failure, Unit] =
    for {
      optionalNewPriceRiseId <- updateSalesforce(cohortSpec, item)
        .tapBoth(
          e => Logging.error(s"Failed to write create Price_Rise in salesforce: $e"),
          result => Logging.info(s"SalesforcePriceRise result: $result")
        )
      now <- Clock.instant
      salesforcePriceRiseCreationDetails = CohortItem(
        subscriptionName = item.subscriptionName,
        processingStage = SalesforcePriceRiseCreationComplete,
        salesforcePriceRiseId = optionalNewPriceRiseId,
        whenSfShowEstimate = Some(now)
      )
      _ <- CohortTable.update(salesforcePriceRiseCreationDetails)
    } yield ()

  private def updateSalesforce(
      cohortSpec: CohortSpec,
      cohortItem: CohortItem
  ): ZIO[Salesforce, Failure, Option[String]] = {
    for {
      subscription <- Salesforce.getSubscriptionByName(cohortItem.subscriptionName)
      priceRise <- buildPriceRise(cohortSpec, cohortItem, subscription)
      result <-
        cohortItem.salesforcePriceRiseId
          .fold(
            Salesforce
              .createPriceRise(priceRise)
              .map[Option[String]](response => Some(response.id))
          ) { priceRiseId =>
            Salesforce
              .updatePriceRise(priceRiseId, priceRise)
              .as(None)
          }
    } yield result
  }

  def buildPriceRise(
      cohortSpec: CohortSpec,
      cohortItem: CohortItem,
      subscription: SalesforceSubscription
  ): IO[SalesforcePriceRiseWriteFailure, SalesforcePriceRise] = {
    for {
      oldPrice <-
        ZIO
          .fromOption(cohortItem.oldPrice)
          .orElseFail(SalesforcePriceRiseWriteFailure(s"$cohortItem does not have an oldPrice"))
      commsPrice <-
        ZIO
          .fromOption(cohortItem.commsPrice)
          .orElseFail(SalesforcePriceRiseWriteFailure(s"$cohortItem does not have an estimatedNewPrice"))
      priceRiseDate <-
        ZIO
          .fromOption(cohortItem.amendmentEffectiveDate)
          .orElseFail(SalesforcePriceRiseWriteFailure(s"$cohortItem does not have a startDate"))
    } yield {
      SalesforcePriceRise(
        Some(subscription.Name),
        Some(subscription.Buyer__c),
        Some(oldPrice),
        Some(commsPrice),
        Some(priceRiseDate),
        Some(subscription.Id),
        Migration_Name__c = Some(cohortSpec.cohortName),
        Migration_Status__c = Some("EstimationComplete"),
        Cancellation_Reason__c = None
      )
    }
  }

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] = {
    main(input).provideSome[Logging](
      EnvConfig.salesforce.layer,
      EnvConfig.stage.layer,
      DynamoDBZIOLive.impl,
      DynamoDBClientLive.impl,
      CohortTableLive.impl(input),
      SalesforceLive.impl,
    )
  }
}
