package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter.{AmendmentComplete, AmendmentWrittenToSalesforce}
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.{Clock, ZIO}

/** Updates Salesforce with evidence of the price-rise amendment that was applied in Zuora.
  */
object SalesforceAmendmentUpdateHandler extends CohortHandler {

  private val batchSize = 2000
  private def main(
      cohortSpec: CohortSpec
  ): ZIO[CohortTable with SalesforceClient with Logging, Failure, HandlerOutput] =
    for {
      count <- CohortTable
        .fetch(AmendmentComplete, None)
        .take(batchSize)
        .mapZIO(item =>
          updateSfWithNewSubscriptionId(cohortSpec, item).tapBoth(
            e => Logging.error(s"Failed to update price rise record for ${item.subscriptionName}: $e"),
            _ => Logging.info(s"Amendment of ${item.subscriptionName} recorded in Salesforce")
          )
        )
        .runCount
    } yield HandlerOutput(isComplete = count < batchSize)

  private def updateSfWithNewSubscriptionId(
      cohortSpec: CohortSpec,
      item: CohortItem
  ): ZIO[CohortTable with SalesforceClient with Logging, Failure, Unit] =
    for {
      priceRise <- ZIO.fromEither(buildPriceRise(cohortSpec, item))
      salesforcePriceRiseId <-
        ZIO
          .fromOption(item.salesforcePriceRiseId)
          .orElseFail(SalesforcePriceRiseWriteFailure("salesforcePriceRiseId is required to update Salesforce"))
      _ <- SalesforceClient.getPriceRise(salesforcePriceRiseId)
      _ <- SalesforceClient.updatePriceRise(salesforcePriceRiseId, priceRise)
      now <- Clock.instant
      _ <-
        CohortTable
          .update(
            CohortItem(
              subscriptionName = item.subscriptionName,
              processingStage = AmendmentWrittenToSalesforce,
              whenAmendmentWrittenToSalesforce = Some(now)
            )
          )
    } yield ()

  private def buildPriceRise(
      cohortSpec: CohortSpec,
      cohortItem: CohortItem
  ): Either[SalesforcePriceRiseWriteFailure, SalesforcePriceRise] =
    cohortItem.newSubscriptionId
      .map(newSubscriptionId =>
        SalesforcePriceRise(
          Amended_Zuora_Subscription_Id__c = Some(newSubscriptionId),
          Migration_Name__c = Some(cohortSpec.cohortName),
          Migration_Status__c = Some("AmendmentComplete"),
          Cancellation_Reason__c = None
        )
      )
      .toRight(SalesforcePriceRiseWriteFailure(s"$cohortItem does not have a newSubscriptionId field"))

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] = {
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
