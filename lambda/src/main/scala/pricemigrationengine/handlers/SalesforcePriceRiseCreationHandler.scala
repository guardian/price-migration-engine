package pricemigrationengine.handlers

import pricemigrationengine.libs.PriceCap
import pricemigrationengine.migrations.{
  GuardianWeekly2025Migration,
  HomeDelivery2025Migration,
  Newspaper2025P1Migration
}
import pricemigrationengine.model.CohortTableFilter.{EstimationComplete, SalesforcePriceRiseCreationComplete}
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
        processingStage = SalesforcePriceRiseCreationComplete,
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
        case Test1             => estimatedNewPrice // default value
        case SupporterPlus2024 => estimatedNewPrice // [1]
        case GuardianWeekly2025 =>
          PriceCap.cappedPrice(oldPrice, estimatedNewPrice, GuardianWeekly2025Migration.priceCap)
        case Newspaper2025P1 =>
          PriceCap.cappedPrice(oldPrice, estimatedNewPrice, Newspaper2025P1Migration.priceCap)
        case HomeDelivery2025 =>
          PriceCap.cappedPrice(oldPrice, estimatedNewPrice, HomeDelivery2025Migration.priceCap)
      }
      // [1]
      // (Comment group: 7992fa98)

      // This value wasn't actually used because we did that step using a Ruby script (we did not run the
      // SalesforcePriceRiseCreationHandler from the AWS step function).
      // The problem was that from the CohortItem we only had the old base price and the new base price
      // but not the contribution component, and therefore we could not compute the total which is what
      // we need to send to Salesforce.

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
    // [1]
    // (Comment group: 7992fa98)
    // We are not running this lambda for SupporterPlus2024, because we, instead, used a Ruby script
    // to perform the Salesforce price rise creation, due to the extra computation required to compute
    // the correct price.
    MigrationType(input) match {
      case SupporterPlus2024 => ZIO.succeed(HandlerOutput(isComplete = true)) // See [1] above
      case Newspaper2025P1   => ZIO.succeed(HandlerOutput(isComplete = true))
      case HomeDelivery2025  => ZIO.succeed(HandlerOutput(isComplete = true))
      case _ =>
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
