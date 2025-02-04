package pricemigrationengine.handlers

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
  ): ZIO[CohortTable with Zuora with Logging, Failure, AmendmentResult] =
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
        case e: ZuoraUpdateFailure => {
          // We are only interested in the ZuoraUpdateFailures corresponding to message
          // "Operation failed due to a lock competition"
          // We succeed them without cohort item update to be done later.
          if (e.reason.contains("lock competition")) {
            ZIO.succeed(AmendmentPreventedDueToLockResult(subscriptionNumber = item.subscriptionName))
          } else {
            ZIO.fail(e)
          }
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

  private def renewSubscription(
      subscription: ZuoraSubscription,
      effectDate: LocalDate,
      account: ZuoraAccount
  ): ZIO[Zuora with Logging, Failure, Unit] = {
    val payload = ZuoraRenewOrderPayload(
      LocalDate.now(),
      subscription.subscriptionNumber,
      account.basicInfo.accountNumber,
      effectDate
    )
    for {
      _ <- Logging.info(s"Renewing subscription ${subscription.subscriptionNumber} with payload $payload")
      _ <- Zuora.renewSubscription(subscription.subscriptionNumber, payload)
    } yield ()
  }

  private def shouldPerformFinalPriceCheck(cohortSpec: CohortSpec): Boolean = {
    MigrationType(cohortSpec) match {
      case Newspaper2024     => true
      case GW2024            => true
      case SupporterPlus2024 => false // [1]
      case SPV1V2E2025       => false // [2]
      case Default           => true
    }
    // [1] SupporterPlus2024: We don't perform the final price check because due to the way the prices
    // are computed, the new price can be higher than the estimated price
    // (which wasn't including the extra contribution).

    // [2] SPV1V2E2025: Same reason as SupporterPlus2024
  }

  private def doAmendment(
      cohortSpec: CohortSpec,
      catalogue: ZuoraProductCatalogue,
      item: CohortItem
  ): ZIO[Zuora with Logging, Failure, SuccessfulAmendmentResult] = {

    MigrationType(cohortSpec) match {
      case SupporterPlus2024 => {
        for {
          subscriptionBeforeUpdate <- fetchSubscription(item)

          startDate <- ZIO.fromOption(item.startDate).orElseFail(DataExtractionFailure(s"No start date in $item"))

          oldPrice <- ZIO.fromOption(item.oldPrice).orElseFail(DataExtractionFailure(s"No old price in $item"))

          estimatedNewPrice <-
            ZIO
              .fromOption(item.estimatedNewPrice)
              .orElseFail(DataExtractionFailure(s"No estimated new price in $item"))

          invoicePreviewTargetDate = startDate.plusMonths(13)

          account <- Zuora.fetchAccount(
            subscriptionBeforeUpdate.accountNumber,
            subscriptionBeforeUpdate.subscriptionNumber
          )

          _ <- renewSubscription(subscriptionBeforeUpdate, subscriptionBeforeUpdate.termEndDate, account)

          order <- ZIO.fromEither(
            SupporterPlus2024Migration.amendmentOrderPayload(
              orderDate = LocalDate.now(),
              accountNumber = account.basicInfo.accountNumber,
              subscriptionNumber = subscriptionBeforeUpdate.subscriptionNumber,
              effectDate = startDate,
              subscription = subscriptionBeforeUpdate,
              oldPrice = oldPrice,
              estimatedNewPrice = estimatedNewPrice,
              priceCap = SupporterPlus2024Migration.priceCap
            )
          )

          _ <- Logging.info(
            s"Amending subscription ${subscriptionBeforeUpdate.subscriptionNumber} with order ${order}"
          )

          _ <- Zuora.applyAmendmentOrder(subscriptionBeforeUpdate, order)

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
          subscriptionAfterUpdate.id,
          whenDone
        )
      }
      case _ => {
        for {
          subscriptionBeforeUpdate <- fetchSubscription(item)

          startDate <- ZIO.fromOption(item.startDate).orElseFail(DataExtractionFailure(s"No start date in $item"))

          oldPrice <- ZIO.fromOption(item.oldPrice).orElseFail(DataExtractionFailure(s"No old price in $item"))

          estimatedNewPrice <-
            ZIO
              .fromOption(item.estimatedNewPrice)
              .orElseFail(DataExtractionFailure(s"No estimated new price in $item"))

          invoicePreviewTargetDate = startDate.plusMonths(13)

          account <- Zuora.fetchAccount(
            subscriptionBeforeUpdate.accountNumber,
            subscriptionBeforeUpdate.subscriptionNumber
          )

          _ <- renewSubscription(subscriptionBeforeUpdate, subscriptionBeforeUpdate.termEndDate, account)

          invoicePreviewBeforeUpdate <-
            Zuora.fetchInvoicePreview(subscriptionBeforeUpdate.accountId, invoicePreviewTargetDate)

          update <- MigrationType(cohortSpec) match {
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
                  oldPrice,
                  estimatedNewPrice,
                  GW2024Migration.priceCap
                )
              )
            case SupporterPlus2024 =>
              ZIO.fromEither(
                SupporterPlus2024Migration.zuoraUpdate(
                  subscriptionBeforeUpdate,
                  startDate,
                  oldPrice,
                  estimatedNewPrice,
                  SupporterPlus2024Migration.priceCap
                )
              )
            case SPV1V2E2025 => {
              ZIO.fromEither(
                SPV1V2E2025Migration.zuoraUpdate(
                  subscriptionBeforeUpdate,
                  startDate
                )
              )
            }
            case Default =>
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

          _ <- Logging.info(
            s"Amending subscription ${subscriptionBeforeUpdate.subscriptionNumber} with update ${update}"
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
                startDate
              )
            )

          _ <-
            if (shouldPerformFinalPriceCheck(cohortSpec: CohortSpec) && (newPrice > estimatedNewPrice)) {
              ZIO.fail(
                DataExtractionFailure(
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
          subscriptionAfterUpdate.id,
          whenDone
        )
      }
    }
  }

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] = {
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
