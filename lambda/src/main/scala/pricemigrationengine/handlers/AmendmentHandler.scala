package pricemigrationengine.handlers

import pricemigrationengine.migrations.DigiSubs2023Migration
import pricemigrationengine.model.CohortTableFilter.NotificationSendDateWrittenToSalesforce
import pricemigrationengine.model._
import pricemigrationengine.migrations._
import pricemigrationengine.services._
import zio.{Clock, ZIO}
import java.time.{LocalDate, LocalDateTime, ZoneOffset}
import pricemigrationengine.migrations.newspaper2024Migration

/** Carries out price-rise amendments in Zuora.
  */
object AmendmentHandler extends CohortHandler {

  private val batchSize = 50

  private def main(cohortSpec: CohortSpec): ZIO[Logging with CohortTable with Zuora, Failure, HandlerOutput] = {
    for {
      catalogue <- Zuora.fetchProductCatalogue
      count <- CohortTable
        .fetch(NotificationSendDateWrittenToSalesforce, None)
        .take(batchSize)
        .mapZIO(item => amend(cohortSpec, catalogue, item).tapBoth(Logging.logFailure(item), Logging.logSuccess(item)))
        .runCount
    } yield HandlerOutput(isComplete = count < batchSize)
  }

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
          CohortTable
            .update(
              CohortItem.fromCancelledAmendmentResult(result, "(cause: 99727bf9) subscription was cancelled in Zuora")
            )
            .as(result)
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

  private def renewSubscriptionIfNeeded(
      subscription: ZuoraSubscription,
      startDate: LocalDate
  ): ZIO[Zuora, Failure, Unit] = {
    if (subscription.termEndDate.isBefore(startDate)) {
      Zuora.renewSubscription(subscription.subscriptionNumber)
    } else {
      ZIO.succeed(())
    }
  }

  private def shouldPerformFinalPriceCheck(cohortSpec: CohortSpec): Boolean = {
    // Date: 8 Sept 2023
    // This function is introduced as part of a multi stage update of the
    // engine to detect and deal with situations where the final price is higher than the
    // estimated new price, which has happened with Quarterly Guardian Weekly subscriptions
    // The ultimate aim is to update the engine to deal with those situations automatically,
    // but in this step we simply error and will alarm at the next occurrence of this situation.
    // (see the code in `doAmendment`)

    // When that situation occurs, a good course of action will be to
    //    1. Revert the amendment in Zuora
    //    2. Reset the cohort item in the dynamo table
    //    3. Update the code to perform a negative charge back
    //    4. Rerun the engine and check the item in Zuora

    // Note that we do not apply the check to the SupporterPlus2023V1V2 migration
    // where due to the way the prices are computed, the new price can be higher than the
    // estimated price (which wasn't including the extra contribution).

    MigrationType(cohortSpec) match {
      case SupporterPlus2023V1V2MA => false
      case _                       => true
    }
  }

  private def doAmendment(
      cohortSpec: CohortSpec,
      catalogue: ZuoraProductCatalogue,
      item: CohortItem
  ): ZIO[Zuora, Failure, SuccessfulAmendmentResult] = {

    for {
      subscriptionBeforeUpdate <- fetchSubscription(item)

      startDate <- ZIO.fromOption(item.startDate).orElseFail(AmendmentDataFailure(s"No start date in $item"))

      oldPrice <- ZIO.fromOption(item.oldPrice).orElseFail(AmendmentDataFailure(s"No old price in $item"))

      estimatedNewPrice <-
        ZIO
          .fromOption(item.estimatedNewPrice)
          .orElseFail(AmendmentDataFailure(s"No estimated new price in $item"))

      invoicePreviewTargetDate = startDate.plusMonths(13)

      _ <- renewSubscriptionIfNeeded(subscriptionBeforeUpdate, startDate)

      account <- Zuora.fetchAccount(subscriptionBeforeUpdate.accountNumber, subscriptionBeforeUpdate.subscriptionNumber)

      invoicePreviewBeforeUpdate <-
        Zuora.fetchInvoicePreview(subscriptionBeforeUpdate.accountId, invoicePreviewTargetDate)

      update <- MigrationType(cohortSpec) match {
        case Membership2023Monthlies =>
          ZIO.fromEither(
            Membership2023Migration
              .zuoraUpdate_Monthlies(
                subscriptionBeforeUpdate,
                invoicePreviewBeforeUpdate,
                startDate
              )
          )
        case Membership2023Annuals =>
          ZIO.fromEither(
            Membership2023Migration
              .zuoraUpdate_Annuals(
                subscriptionBeforeUpdate,
                invoicePreviewBeforeUpdate,
                startDate
              )
          )
        case SupporterPlus2023V1V2MA =>
          ZIO.fromEither(
            SupporterPlus2023V1V2Migration
              .zuoraUpdate(
                item,
                subscriptionBeforeUpdate,
                invoicePreviewBeforeUpdate,
                startDate
              )
          )
        case DigiSubs2023 =>
          ZIO.fromEither(
            DigiSubs2023Migration.zuoraUpdate(
              subscriptionBeforeUpdate,
              startDate,
            )
          )
        case Newspaper2024 =>
          ZIO.fromEither(
            newspaper2024Migration.Amendment.zuoraUpdate(
              subscriptionBeforeUpdate,
              startDate,
            )
          )
        case GW2024 =>
          ZIO.fromEither(
            GW2024Migration.zuoraUpdate(
              subscriptionBeforeUpdate,
              startDate,
            )
          )
        case Legacy =>
          ZIO.fromEither(
            ZuoraSubscriptionUpdate
              .zuoraUpdate(
                account,
                catalogue,
                subscriptionBeforeUpdate,
                invoicePreviewBeforeUpdate,
                startDate,
                Some(PriceCap.priceCapLegacy(oldPrice, estimatedNewPrice))
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

      _ <-
        if (shouldPerformFinalPriceCheck(cohortSpec: CohortSpec) && (newPrice > estimatedNewPrice)) {
          ZIO.fail(
            AmendmentDataFailure(
              s"[e9054daa] Item ${item} has gone through the amendment step but has failed the final price check. Estimated price was ${estimatedNewPrice}, but the final price was ${newPrice}"
            )
          )
        } else {
          ZIO.succeed(())
        }

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
      case GW2024 => ZIO.succeed(HandlerOutput(isComplete = true))
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
