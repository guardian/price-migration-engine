package pricemigrationengine.handlers

import pricemigrationengine.model.CohortSpec.isMembershipPriceRiseBatch1
import pricemigrationengine.model.CohortTableFilter.{EstimationComplete, SalesforcePriceRiceCreationComplete}
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.{Clock, IO, ZIO}

object SalesforcePriceRiseCreationHandler extends CohortHandler {

  // TODO: move to config
  private val batchSize = 1000

  private[handlers] def main(
      cohortSpec: CohortSpec
  ): ZIO[Logging with CohortTable with SalesforceClient, Failure, HandlerOutput] =
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
  ): ZIO[Logging with CohortTable with SalesforceClient, Failure, Unit] =
    for {
      optionalNewPriceRiseId <- updateSalesforce(cohortSpec, item)
        .tapBoth(
          e => Logging.error(s"Failed to write create Price_Rise in salesforce: $e"),
          result => Logging.info(s"SalesforcePriceRise result: $result")
        )
      now <- Clock.instant
      salesforcePriceRiseCreationDetails = CohortItem(
        subscriptionName = item.subscriptionName,
        processingStage = SalesforcePriceRiceCreationComplete,
        salesforcePriceRiseId = optionalNewPriceRiseId,
        whenSfShowEstimate = Some(now)
      )
      _ <- CohortTable.update(salesforcePriceRiseCreationDetails)
    } yield ()

  private def updateSalesforce(
      cohortSpec: CohortSpec,
      cohortItem: CohortItem
  ): ZIO[SalesforceClient, Failure, Option[String]] = {
    for {
      subscription <- SalesforceClient.getSubscriptionByName(cohortItem.subscriptionName)
      priceRise <- buildPriceRise(cohortSpec, cohortItem, subscription)
      result <-
        cohortItem.salesforcePriceRiseId
          .fold(
            SalesforceClient
              .createPriceRise(priceRise)
              .map[Option[String]](response => Some(response.id))
          ) { priceRiseId =>
            SalesforceClient
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
      estimatedNewPrice <-
        ZIO
          .fromOption(cohortItem.estimatedNewPrice)
          .orElseFail(SalesforcePriceRiseWriteFailure(s"$cohortItem does not have an estimatedNewPrice"))
      priceRiseDate <-
        ZIO
          .fromOption(cohortItem.startDate)
          .orElseFail(SalesforcePriceRiseWriteFailure(s"$cohortItem does not have a startDate"))
    } yield SalesforcePriceRise(
      Some(subscription.Name),
      Some(subscription.Buyer__c),
      Some(oldPrice),
      Some(
        PriceCap.cappedPrice(oldPrice, estimatedNewPrice, CohortSpec.isMembershipPriceRiseBatch1(cohortSpec))
      ), // In case of membership price rise, we override the capping
      Some(priceRiseDate),
      Some(subscription.Id)
    )
  }

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] =
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
