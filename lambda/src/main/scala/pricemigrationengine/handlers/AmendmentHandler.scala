package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter.NotificationSendDateWrittenToSalesforce
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.{Clock, ZIO}

import java.time.LocalDate

/** Carries out price-rise amendments in Zuora.
  */
object AmendmentHandler extends CohortHandler {

  // TODO: move to config
  private val batchSize = 150

  private def main(cohortSpec: CohortSpec): ZIO[Logging with CohortTable with Zuora, Failure, HandlerOutput] =
    for {
      catalogue <- Zuora.fetchProductCatalogue
      count <- CohortTable
        .fetch(NotificationSendDateWrittenToSalesforce, None)
        .take(batchSize)
        .mapZIO(item => amend(cohortSpec, catalogue, item).tapBoth(Logging.logFailure(item), Logging.logSuccess(item)))
        .runCount
    } yield HandlerOutput(isComplete = count < batchSize)

  private def amend(
      cohortSpec: CohortSpec,
      catalogue: ZuoraProductCatalogue,
      item: CohortItem
  ): ZIO[CohortTable with Zuora, Failure, AmendmentResult] =
    doAmendment(cohortSpec, catalogue, item).foldZIO(
      failure = {
        case _: CancelledSubscriptionFailure => {
          // `CancelledSubscriptionFailure` happens when the subscription was cancelled in Zuora
          // in which case we simply update the processing state for this item in the database
          // Although it was given to us as a failure of `doAmendment`, the only effect of the database update, if it
          // is not recorded as a failure of `amend`, is to allow the processing to continue.
          val result = CancelledAmendmentResult(item.subscriptionName)
          CohortTable.update(CohortItem.fromCancelledAmendmentResult(result)).as(result)
        }
        case _: ExpiringSubscriptionFailure => {
          // `ExpiringSubscriptionFailure` happens when the subscription's end of effective period is before the
          // intended startDate (the price increase date). Alike `CancelledSubscriptionFailure` we cancel the amendment
          // and the only effect is an updated cohort item in the database
          val result = ExpiringSubscriptionResult(item.subscriptionName)
          CohortTable.update(CohortItem.fromExpiringSubscriptionResult(result)).as(result)
        }
        case e => ZIO.fail(e)
      },
      success = { result =>
        CohortTable.update(CohortItem.fromSuccessfulAmendmentResult(result)).as(result)
      }
    )

  private def fetchSubscription(item: CohortItem): ZIO[Zuora, Failure, ZuoraSubscription] =
    Zuora
      .fetchSubscription(item.subscriptionName)
      .filterOrFail(_.status != "Cancelled")(CancelledSubscriptionFailure(item.subscriptionName))

  def checkExpirationTiming(
      item: CohortItem,
      subscription: ZuoraSubscription
  ): Either[Failure, Unit] = {
    // We check that the subscription's end of effective period is after the startDate, to avoid Zuora's error:
    // ```
    // The Contract effective date should not be later than the term end date of the basic subscription
    // ```
    // Note that this check will be duplicated in the estimation handler (this check in the Amendment handler came first)

    item.startDate match {
      case None =>
        Left(
          AmendmentDataFailure(
            s"Cohort item: ${item.subscriptionName}. Cohort Item doesn't have a startDate."
          )
        ) // This case won't really happen in practice, but item.startDate is an option
      case Some(startDate) => {
        if (subscription.termEndDate.isAfter(startDate)) {
          Right(())
        } else {
          Left(
            ExpiringSubscriptionFailure(
              s"Cohort item: ${item.subscriptionName}. The item startDate (price increase date), ${item.startDate}, is after the subscription's end of effective period (${subscription.termEndDate.toString})"
            )
          )
        }
      }
    }
  }

  private def doAmendment(
      cohortSpec: CohortSpec,
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

      _ <- ZIO.fromEither(checkExpirationTiming(item, subscriptionBeforeUpdate))

      account <- Zuora.fetchAccount(subscriptionBeforeUpdate.accountNumber, subscriptionBeforeUpdate.subscriptionNumber)

      invoicePreviewBeforeUpdate <-
        Zuora.fetchInvoicePreview(subscriptionBeforeUpdate.accountId, invoicePreviewTargetDate)

      update <- MigrationType(cohortSpec) match {
        case Membership2023Monthlies =>
          ZIO.fromEither(
            Membership2023
              .updateOfRatePlansToCurrent_Monthlies(
                subscriptionBeforeUpdate,
                invoicePreviewBeforeUpdate,
                startDate
              )
          )
        case Membership2023Annuals =>
          ZIO.fromEither(
            Membership2023
              .updateOfRatePlansToCurrent_Annuals(
                subscriptionBeforeUpdate,
                invoicePreviewBeforeUpdate,
                startDate
              )
          )
        case SupporterPlus2023V1V2 =>
          ZIO.fromEither(
            ZuoraSubscriptionUpdate
              .updateOfRatePlansToCurrent(
                account,
                catalogue,
                subscriptionBeforeUpdate,
                invoicePreviewBeforeUpdate,
                startDate,
                PriceCap.priceCorrectionFactorForPriceCap(oldPrice, estimatedNewPrice)
              )
          )
        case Legacy =>
          ZIO.fromEither(
            ZuoraSubscriptionUpdate
              .updateOfRatePlansToCurrent(
                account,
                catalogue,
                subscriptionBeforeUpdate,
                invoicePreviewBeforeUpdate,
                startDate,
                PriceCap.priceCorrectionFactorForPriceCap(oldPrice, estimatedNewPrice)
              )
          )
      }

      newSubscriptionId <- Zuora.updateSubscription(subscriptionBeforeUpdate, update)

      subscriptionAfterUpdate <- fetchSubscription(item)

      invoicePreviewAfterUpdate <-
        Zuora.fetchInvoicePreview(subscriptionAfterUpdate.accountId, invoicePreviewTargetDate)

      newPrice <-
        ZIO.fromEither(
          AmendmentData.totalChargeAmount(
            subscriptionAfterUpdate,
            invoicePreviewAfterUpdate,
            startDate
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

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] = {
    MigrationType(input) match {
      case Membership2023Annuals => ZIO.succeed(HandlerOutput(isComplete = true))
      case _ =>
        main(input).provideSome[Logging](
          EnvConfig.cohortTable.layer,
          EnvConfig.zuora.layer,
          EnvConfig.stage.layer,
          DynamoDBZIOLive.impl,
          DynamoDBClientLive.impl,
          CohortTableLive.impl(input),
          ZuoraLive.impl
        )
    }
  }
}
