package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter.NotificationSendDateWrittenToSalesforce
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.{Clock, ZIO}

/** Carries out price-rise amendments in Zuora.
  */
object AmendmentHandler extends CohortHandler {

  // TODO: move to config
  private val batchSize = 150
  private val priceCappingMultiplier = 1.2

  val main: ZIO[Logging with CohortTable with Zuora, Failure, HandlerOutput] =
    for {
      catalogue <- Zuora.fetchProductCatalogue
      count <- CohortTable
        .fetch(NotificationSendDateWrittenToSalesforce, None)
        .take(batchSize)
        .mapZIO(item => amend(catalogue, item).tapBoth(Logging.logFailure(item), Logging.logSuccess(item)))
        .runCount
    } yield HandlerOutput(isComplete = count < batchSize)

  private def amend(
      catalogue: ZuoraProductCatalogue,
      item: CohortItem
  ): ZIO[CohortTable with Zuora, Failure, AmendmentResult] =
    doAmendment(catalogue, item).foldZIO(
      failure = {
        case _: CancelledSubscriptionFailure =>
          val result = CancelledAmendmentResult(item.subscriptionName)
          CohortTable.update(CohortItem.fromCancelledAmendmentResult(result)).as(result)
        case e => ZIO.fail(e)
      },
      success = { result =>
        CohortTable.update(CohortItem.fromSuccessfulAmendmentResult(result)).as(result)
      }
    )

  private def doAmendment(
      catalogue: ZuoraProductCatalogue,
      item: CohortItem
  ): ZIO[Zuora, Failure, SuccessfulAmendmentResult] = {

    for {
      startDate <- ZIO.fromOption(item.startDate).orElseFail(AmendmentDataFailure(s"No start date in $item"))
      oldPrice <- ZIO.fromOption(item.oldPrice).orElseFail(AmendmentDataFailure(s"No old price in $item"))
      estimatedNewPrice <-
        ZIO
          .fromOption(item.estimatedNewPrice)
          .orElseFail(AmendmentDataFailure(s"No estimated new price in $item"))
      invoicePreviewTargetDate = startDate.plusMonths(13)
      subscriptionBeforeUpdate <- fetchSubscription(item)

      account <- Zuora.fetchAccount(subscriptionBeforeUpdate.accountNumber, subscriptionBeforeUpdate.subscriptionNumber)

      invoicePreviewBeforeUpdate <-
        Zuora.fetchInvoicePreview(subscriptionBeforeUpdate.accountId, invoicePreviewTargetDate)

      update <- ZIO.fromEither(
        ZuoraSubscriptionUpdate
          .updateOfRatePlansToCurrent(
            account,
            catalogue,
            subscriptionBeforeUpdate,
            invoicePreviewBeforeUpdate,
            startDate,
            Some(
              ChargeCap(
                Some(item),
                oldPrice * priceCappingMultiplier
              ) // ChargeCap here is used to apply the correct rate plan charges
            )
          )
      )
      newSubscriptionId <- Zuora.updateSubscription(subscriptionBeforeUpdate, update)
      subscriptionAfterUpdate <- fetchSubscription(item)
      invoicePreviewAfterUpdate <-
        Zuora.fetchInvoicePreview(subscriptionAfterUpdate.accountId, invoicePreviewTargetDate)
      newPrice <-
        ZIO.fromEither(
          AmendmentData.totalChargeAmount(
            subscriptionAfterUpdate,
            invoicePreviewAfterUpdate,
            startDate,
            Some(
              ChargeCap(
                Some(item),
                oldPrice * priceCappingMultiplier
              ) // ChargeCap here is used to check that the price computed after amendment is within parameters
            )
          )
        )
      whenDone <- Clock.instant
    } yield SuccessfulAmendmentResult(
      item.subscriptionName,
      startDate,
      oldPrice,
      newPrice,
      estimatedNewPrice,
      newSubscriptionId,
      whenDone
    )
  }

  private def fetchSubscription(item: CohortItem): ZIO[Zuora, Failure, ZuoraSubscription] =
    Zuora
      .fetchSubscription(item.subscriptionName)
      .filterOrFail(_.status != "Cancelled")(CancelledSubscriptionFailure(item.subscriptionName))

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] =
    main.provideSome[Logging](
      EnvConfig.cohortTable.layer,
      EnvConfig.zuora.layer,
      EnvConfig.stage.layer,
      DynamoDBZIOLive.impl,
      DynamoDBClientLive.impl,
      CohortTableLive.impl(input),
      ZuoraLive.impl
    )
}
