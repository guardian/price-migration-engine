package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter.{EstimationComplete, SalesforcePriceRiceCreationComplete}
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.{Clock, IO, ZIO}

object SalesforcePriceRiseCreationHandler extends CohortHandler {

  // TODO: move to config
  private val batchSize = 1000

  private[handlers] val main: ZIO[Logging with CohortTable with SalesforceClient, Failure, HandlerOutput] =
    for {
      count <- CohortTable.fetch(EstimationComplete, None).take(batchSize).mapZIO(createSalesforcePriceRise).runCount
    } yield HandlerOutput(isComplete = count < batchSize)

  private def createSalesforcePriceRise(
      item: CohortItem
  ): ZIO[Logging with CohortTable with SalesforceClient, Failure, Unit] =
    for {
      optionalNewPriceRiseId <- updateSalesforce(item)
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
      cohortItem: CohortItem
  ): ZIO[SalesforceClient, Failure, Option[String]] = {
    for {
      subscription <- SalesforceClient.getSubscriptionByName(cohortItem.subscriptionName)
      priceRise <- buildPriceRise(cohortItem, subscription)
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
      cohortItem: CohortItem,
      subscription: SalesforceSubscription
  ): IO[SalesforcePriceRiseWriteFailure, SalesforcePriceRise] = {
    for {
      currentPrice <-
        ZIO
          .fromOption(cohortItem.oldPrice)
          .orElseFail(SalesforcePriceRiseWriteFailure(s"$cohortItem does not have an oldPrice"))
      newPrice <-
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
      Some(currentPrice),
      Some(newPrice),
      Some(priceRiseDate),
      Some(subscription.Id)
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
