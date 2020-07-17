package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter.NotificationSendDateWrittenToSalesforce
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.Schedule.{exponential, recurs}
import zio.clock.Clock
import zio.duration._
import zio.{ZEnv, ZIO, ZLayer}

/**
  * Carries out price-rise amendments in Zuora.
  */
object AmendmentHandler extends CohortHandler {

  // TODO: move to config
  private val batchSize = 150

  val main: ZIO[Logging with CohortTable with Zuora with Clock, Failure, HandlerOutput] =
    for {
      newProductPricing <- Zuora.fetchProductCatalogue.map(ZuoraProductCatalogue.productPricingMap)
      cohortItems <- CohortTable.fetch(NotificationSendDateWrittenToSalesforce, None)
      count <-
        cohortItems
          .take(batchSize)
          .mapM(item => amend(newProductPricing, item).tapBoth(Logging.logFailure(item), Logging.logSuccess(item)))
          .runCount
    } yield HandlerOutput(isComplete = count < batchSize)

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
        Zuora
          .fetchInvoicePreview(subscriptionAfterUpdate.accountId, invoicePreviewTargetDate)
          .retry(exponential(1.second) && recurs(5)) // to work around StaleObjectStateException in Zuora response
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
      cohortSpec: CohortSpec
  ): ZLayer[Logging, ConfigurationFailure, CohortTable with Zuora with Logging] = {
    (LiveLayer.cohortTable(cohortSpec.tableName) and LiveLayer.zuora and LiveLayer.logging)
      .tapError(e => Logging.error(s"Failed to create service environment: $e"))
  }

  def handle(input: CohortSpec): ZIO[ZEnv with Logging, Failure, HandlerOutput] =
    main.provideSomeLayer[ZEnv with Logging](env(input))
}
