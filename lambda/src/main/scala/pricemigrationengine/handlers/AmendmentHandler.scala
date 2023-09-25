package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter.NotificationSendDateWrittenToSalesforce
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.{Clock, ZIO}

import java.time.{LocalDate, LocalDateTime, ZoneOffset}

/** Carries out price-rise amendments in Zuora.
  */
object AmendmentHandler extends CohortHandler {

  // TODO: move to config
  private val batchSize = 100

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
        case _: IncompatibleAmendmentHistory => {
          // See the preambule of the ZuoraSubscriptionAmendment case class for context about
          // IncompatibleAmendmentHistory
          // Note: At the moment we only have one CancelledAmendmentResult, it would be great one day
          //       To indicate several cancellation conditions, for instance discriminate between
          //       cancellations because the zuora subscription has been cancelled and cancellations
          //       because of incompatible amendment history.s
          val result = CancelledAmendmentResult(item.subscriptionName)
          CohortTable.update(CohortItem.fromCancelledAmendmentResult(result)).as(result)
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

  private def checkFinalPriceVersusEstimatedNewPrice(
      cohortSpec: CohortSpec,
      item: CohortItem,
      estimatedNewPrice: BigDecimal,
      newPrice: BigDecimal
  ): ZIO[Zuora, Failure, Unit] = {
    // Date: 8 Sept 2023
    // This function is introduced as part of a multi stage update of the
    // engine to detect and deal with situations where the final price is higher than the
    // estimated new price, which has happened with Quarterly Guardian Weekly subscriptions
    // The ultimate aim is to update the engine to deal with those situations automatically,
    // but in this step we simply error and will alarm at the next occurrence of this situation.
    // When that situation occurs, a good course of action will be to
    //    1. Revert the amendment in Zuora
    //    2. Reset the cohort item in the dynamo table
    //    3. Update the code to perform a negative charge back
    //    4. Rerun the engine and check the item in Zuora

    // Note that we do not apply the check to the SupporterPlus2023V1V2 migration
    // where due to the way the prices are computed, the new price can be higher than the
    // estimated price (which wasn't including the extra contribution).

    MigrationType(cohortSpec) match {
      case SupporterPlus2023V1V2MA => ZIO.succeed(())
      case _ => {
        if (newPrice > estimatedNewPrice) {
          ZIO.fail(
            AmendmentDataFailure(
              s"[e9054daa] Item $item has gone through the amendment step but has failed the final price check. Estimated price was ${estimatedNewPrice}, but the final price was ${newPrice}"
            )
          )
        } else {
          ZIO.succeed(())
        }
      }
    }
  }

  def amendmentIsBeforeInstant(amendment: ZuoraSubscriptionAmendment, instant: java.time.Instant): Boolean = {
    val amendmentDate = LocalDate.parse(amendment.bookingDate)
    val estimationDate = LocalDateTime.ofInstant(instant, ZoneOffset.UTC).toLocalDate
    println(s"amendmentDate: ${amendmentDate}")
    println(s"estimationDate: ${estimationDate}")
    amendmentDate.isBefore(estimationDate)
  }

  private def checkMigrationRelevanceBasedOnLastAmendment(item: CohortItem): ZIO[Zuora, Failure, Unit] = {
    // See the preambule of the ZuoraSubscriptionAmendment case class for context

    // $1:
    // The Zuora documentation
    // https://www.zuora.com/developer/api-references/older-api/operation/GET_AmendmentsBySubscriptionID/
    // specifies that a subscriptionId is to be provided, but it also works with a subscription number
    // (aka subscription name for a cohort item).

    for {
      amendment <- Zuora.fetchLastSubscriptionAmendment(item.subscriptionName).debug("amendment") // $1
      estimationInstant <- ZIO
        .fromOption(item.whenEstimationDone)
        .mapError(ex => AmendmentDataFailure(s"[3026515c] Could not extract whenEstimationDone from item ${item}"))
        .debug("estimationInstant")
    } yield
      if (amendmentIsBeforeInstant(amendment, estimationInstant)) {
        ZIO.succeed(())
      } else {

        // In this temporary version of the code, we are writing it with a general failure,
        // Once we observe this happen we will use the IncompatibleAmendmentHistory failure, which results in a cancellation

        // ZIO.fail(
        //  IncompatibleAmendmentHistory(
        //    s"[4f7589ea] Cohort item ${item} is being written for cancellation, during scheduled amendment, due to last amendment check failing"
        //  )

        ZIO.fail(
          AmendmentDataFailure(
            s"[77c13996] Cohort item ${item} is being written for cancellation, during scheduled amendment, due to last amendment check failing; amendment: ${amendment}"
          )
        )
      }
  }

  private def doAmendment(
      cohortSpec: CohortSpec,
      catalogue: ZuoraProductCatalogue,
      item: CohortItem
  ): ZIO[Zuora, Failure, SuccessfulAmendmentResult] = {

    for {

      _ <- checkMigrationRelevanceBasedOnLastAmendment(item).debug("check relevance")

      startDate <- ZIO.fromOption(item.startDate).orElseFail(AmendmentDataFailure(s"No start date in $item"))

      oldPrice <- ZIO.fromOption(item.oldPrice).orElseFail(AmendmentDataFailure(s"No old price in $item"))

      estimatedNewPrice <-
        ZIO
          .fromOption(item.estimatedNewPrice)
          .orElseFail(AmendmentDataFailure(s"No estimated new price in $item"))

      invoicePreviewTargetDate = startDate.plusMonths(13)

      subscriptionBeforeUpdate <- fetchSubscription(item)

      _ <- renewSubscriptionIfNeeded(subscriptionBeforeUpdate, startDate)

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
        case SupporterPlus2023V1V2MA =>
          ZIO.fromEither(
            SupporterPlus2023V1V2
              .updateOfRatePlansToCurrent(
                item,
                subscriptionBeforeUpdate,
                invoicePreviewBeforeUpdate,
                startDate
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
                Some(PriceCap(oldPrice, estimatedNewPrice))
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

      _ <- checkFinalPriceVersusEstimatedNewPrice(cohortSpec, item, estimatedNewPrice, newPrice)

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
