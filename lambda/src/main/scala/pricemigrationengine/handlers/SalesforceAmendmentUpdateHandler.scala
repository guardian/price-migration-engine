package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter.{AmendmentComplete, AmendmentWrittenToSalesforce}
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.Clock
import zio.{IO, ZEnv, ZIO, ZLayer}

/** Updates Salesforce with evidence of the price-rise amendment that was applied in Zuora.
  */
object SalesforceAmendmentUpdateHandler extends CohortHandler {

  // TODO: move to config
  private val batchSize = 2000

  private val main: ZIO[CohortTable with SalesforceClient with Logging, Failure, HandlerOutput] =
    for {
      cohortItems <- CohortTable.fetch(AmendmentComplete, None)
      count <-
        cohortItems
          .take(batchSize)
          .mapZIO(item =>
            updateSfWithNewSubscriptionId(item).tapBoth(
              e => Logging.error(s"Failed to update price rise record for ${item.subscriptionName}: $e"),
              _ => Logging.info(s"Amendment of ${item.subscriptionName} recorded in Salesforce")
            )
          )
          .runCount
    } yield HandlerOutput(isComplete = count < batchSize)

  private def updateSfWithNewSubscriptionId(
      item: CohortItem
  ): ZIO[CohortTable with SalesforceClient with Logging, Failure, Unit] =
    for {
      priceRise <- ZIO.fromEither(buildPriceRise(item))
      salesforcePriceRiseId <-
        IO
          .fromOption(item.salesforcePriceRiseId)
          .orElseFail(SalesforcePriceRiseWriteFailure("salesforcePriceRiseId is required to update Salesforce"))
      _ <- SalesforceClient.updatePriceRise(salesforcePriceRiseId, priceRise)
      now <- Time.thisInstant
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
      cohortItem: CohortItem
  ): Either[SalesforcePriceRiseWriteFailure, SalesforcePriceRise] =
    cohortItem.newSubscriptionId
      .map(newSubscriptionId => SalesforcePriceRise(Amended_Zuora_Subscription_Id__c = Some(newSubscriptionId)))
      .toRight(SalesforcePriceRiseWriteFailure(s"$cohortItem does not have a newSubscriptionId field"))

  private def env(cohortSpec: CohortSpec): ZLayer[Logging, Failure, CohortTable with SalesforceClient with Logging] =
    (LiveLayer.cohortTable(cohortSpec) and LiveLayer.salesforce and LiveLayer.logging)
      .tapError(e => Logging.error(s"Failed to create service environment: $e"))

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] =
    main.provideSomeLayer[Logging](env(input))
}
