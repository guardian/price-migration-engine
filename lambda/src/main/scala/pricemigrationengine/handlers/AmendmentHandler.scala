package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter.NotificationSendDateWrittenToSalesforce
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.clock.Clock
import zio.{ZEnv, ZIO, ZLayer}

/**
  * Carries out price-rise amendments in Zuora.
  */
object AmendmentHandler extends CohortHandler {

  // TODO: move to config
  private val batchSize = 100

  def main(cohortSpec: CohortSpec): ZIO[Logging with CohortTable with Zuora with Clock, Failure, HandlerOutput] =
    for {
      newProductPricing <- Zuora.fetchProductCatalogue.map(ZuoraProductCatalogue.productPricingMap)
      cohortItems <- fetchFromCohortTable
      _ <-
        cohortItems
          .take(batchSize)
          .foreach(item => amend(newProductPricing, item).tapBoth(Logging.logFailure(item), Logging.logSuccess(item)))
      itemsToGo <- fetchFromCohortTable
      numItemsToGo <- itemsToGo.take(1).runCount
    } yield HandlerOutput(isComplete = numItemsToGo == 0)

  private def fetchFromCohortTable = CohortTable.fetch(NotificationSendDateWrittenToSalesforce, None)

  private def amend(
      newProductPricing: ZuoraPricingData,
      item: CohortItem
  ): ZIO[CohortTable with Zuora with Clock, Failure, AmendmentResult] =
    for {
      result <- doAmendment(newProductPricing, item).foldM(
        failure = {
          case _: CancelledSubscriptionFailure =>
            val result = CancelledAmendmentResult(item.subscriptionName)
            CohortTable.update(CohortItem.fromCancelledAmendmentResult(result)) zipRight ZIO.succeed(result)
          case e => ZIO.fail(e)
        },
        success = { result =>
          CohortTable.update(
            CohortItem.fromSuccessfulAmendmentResult(result)
          ) zipRight ZIO.succeed(result)
        }
      )
    } yield result

  private def doAmendment(
      newProductPricing: ZuoraPricingData,
      item: CohortItem
  ): ZIO[Zuora with Clock, Failure, SuccessfulAmendmentResult] =
    for {
      startDate <- ZIO.fromOption(item.startDate).orElseFail(AmendmentDataFailure(s"No start date in $item"))
      oldPrice <- ZIO.fromOption(item.oldPrice).orElseFail(AmendmentDataFailure(s"No old price in $item"))
      estimatedNewPrice <-
        ZIO
          .fromOption(item.estimatedNewPrice)
          .orElseFail(AmendmentDataFailure(s"No estimated new price in $item"))
      invoicePreviewTargetDate = startDate.plusMonths(13)
      subscriptionBeforeUpdate <- fetchSubscription(item)
      invoicePreviewBeforeUpdate <-
        Zuora.fetchInvoicePreview(subscriptionBeforeUpdate.accountId, invoicePreviewTargetDate)
      update <- ZIO.fromEither(
        ZuoraSubscriptionUpdate
          .updateOfRatePlansToCurrent(
            newProductPricing,
            subscriptionBeforeUpdate,
            invoicePreviewBeforeUpdate,
            startDate
          )
      )
      newSubscriptionId <- Zuora.updateSubscription(subscriptionBeforeUpdate, update)
      subscriptionAfterUpdate <- fetchSubscription(item)
      invoicePreviewAfterUpdate <-
        Zuora.fetchInvoicePreview(subscriptionAfterUpdate.accountId, invoicePreviewTargetDate)
      newPrice <-
        ZIO.fromEither(AmendmentData.totalChargeAmount(subscriptionAfterUpdate, invoicePreviewAfterUpdate, startDate))
      whenDone <- Time.thisInstant
    } yield SuccessfulAmendmentResult(
      item.subscriptionName,
      startDate,
      oldPrice,
      newPrice,
      estimatedNewPrice,
      newSubscriptionId,
      whenDone
    )

  private def fetchSubscription(item: CohortItem): ZIO[Zuora, Failure, ZuoraSubscription] =
    Zuora
      .fetchSubscription(item.subscriptionName)
      .filterOrFail(_.status != "Cancelled")(CancelledSubscriptionFailure(item.subscriptionName))

  private def env(
      loggingService: Logging.Service
  ): ZLayer[Any, ConfigurationFailure, Logging with CohortTable with Zuora] = {
    val loggingLayer = ZLayer.succeed(loggingService)
    val cohortTableLayer =
      loggingLayer ++ EnvConfiguration.dynamoDbImpl >>>
        DynamoDBClient.dynamoDB ++ loggingLayer >>>
        DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.cohortTableImp ++ EnvConfiguration.stageImp >>>
        CohortTableLive.impl
    val zuoraLayer =
      EnvConfiguration.zuoraImpl ++ loggingLayer >>>
        ZuoraLive.impl
    (loggingLayer ++ cohortTableLayer ++ zuoraLayer)
      .tapError(e => loggingService.error(s"Failed to create service environment: $e"))
  }

  def handle(input: CohortSpec, loggingService: Logging.Service): ZIO[ZEnv, Failure, HandlerOutput] =
    main(input).provideCustomLayer(env(loggingService))
}
