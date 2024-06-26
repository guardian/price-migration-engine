package pricemigrationengine.handlers

import pricemigrationengine.migrations.GW2024Migration
import pricemigrationengine.model.CohortTableFilter.{EstimationComplete, SalesforcePriceRiceCreationComplete}
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.{Clock, IO, ZIO}

object SalesforcePriceRiseCreationHandler extends CohortHandler {

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
    } yield {
      val estimatedPriceWithOptionalCapping = MigrationType(cohortSpec) match {
        case Membership2023Monthlies => estimatedNewPrice
        case Membership2023Annuals   => estimatedNewPrice
        case SupporterPlus2023V1V2MA => estimatedNewPrice
        case DigiSubs2023            => estimatedNewPrice
        case Newspaper2024           => estimatedNewPrice
        case GW2024 => PriceCap.priceCapForNotification(oldPrice, estimatedNewPrice, GW2024Migration.priceCap)
        case Legacy => PriceCap.priceCapLegacy(oldPrice, estimatedNewPrice)
      }
      SalesforcePriceRise(
        Some(subscription.Name),
        Some(subscription.Buyer__c),
        Some(oldPrice),
        Some(estimatedPriceWithOptionalCapping),
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
