package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter.{AmendmentComplete, AmendmentWrittenToSalesforce}
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.clock.Clock
import zio.{IO, ZEnv, ZIO, ZLayer}

/**
  * Updates Salesforce with evidence of the price-rise amendment that was applied in Zuora.
  */
object SalesforceAmendmentUpdateHandler extends CohortHandler {

  // TODO: move to config
  private val batchSize = 1000

  private val main: ZIO[CohortTable with SalesforceClient with Clock with Logging, Failure, HandlerOutput] =
    for {
      cohortItems <- CohortTable.fetch(AmendmentComplete, None)
      count <-
        cohortItems
          .take(batchSize)
          .mapM(item =>
            updateSfWithNewSubscriptionId(item).tapBoth(
              e => Logging.error(s"Failed to update price rise record for ${item.subscriptionName}: $e"),
              _ => Logging.info(s"Amendment of ${item.subscriptionName} recorded in Salesforce")
            )
          )
          .runCount
    } yield HandlerOutput(isComplete = count < batchSize)

  private def updateSfWithNewSubscriptionId(
      item: CohortItem
  ): ZIO[CohortTable with SalesforceClient with Clock with Logging, Failure, Unit] =
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

  private def env(
      cohortSpec: CohortSpec,
      loggingService: Logging.Service
  ): ZLayer[Any, Failure, Logging with CohortTable with SalesforceClient with Clock] = {
    val loggingLayer = ZLayer.succeed(loggingService)
    loggingLayer ++ EnvConfiguration.dynamoDbImpl >>>
      DynamoDBClient.dynamoDB ++ loggingLayer >>>
      DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.cohortTableImp ++ EnvConfiguration.stageImp ++ EnvConfiguration.salesforceImp >>>
      (loggingLayer ++ CohortTableLive.impl(cohortSpec.tableName) ++ SalesforceClientLive.impl ++ Clock.live)
        .tapError(e => loggingService.error(s"Failed to create service environment: $e"))
  }

  def handle(input: CohortSpec, loggingService: Logging.Service): ZIO[ZEnv, Failure, HandlerOutput] =
    main.provideCustomLayer(env(input, loggingService))
}
