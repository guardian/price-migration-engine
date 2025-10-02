package pricemigrationengine.model

import pricemigrationengine.model.CohortTableFilter._
import zio.{Clock, UIO}

import java.time.{Instant, LocalDate}

case class CohortItem(
    subscriptionName: String,
    processingStage: CohortTableFilter,
    currency: Option[Currency] = None,
    oldPrice: Option[BigDecimal] = None,
    estimatedNewPrice: Option[BigDecimal] = None,
    newPrice: Option[BigDecimal] = None,
    billingPeriod: Option[String] = None,
    amendmentEffectiveDate: Option[LocalDate] = None,
    salesforcePriceRiseId: Option[String] = None,
    newSubscriptionId: Option[ZuoraSubscriptionId] = None,

    //
    doNotProcessUntil: Option[LocalDate] = None, // [18]
    migrationExtraAttributes: Option[String] = None, // [19]
    cancellationReason: Option[String] = None,

    // timestamps
    whenEstimationDone: Option[Instant] = None,
    whenAmendmentDone: Option[Instant] = None,
    whenNotificationSent: Option[Instant] = None,
    whenNotificationSentWrittenToSalesforce: Option[Instant] = None,
    whenAmendmentWrittenToSalesforce: Option[Instant] = None,
    whenSfShowEstimate: Option[Instant] = None,

    // ------------------------------------------------------
    // For migration specific extensions, see docs/cohort-items.md

    // ProductMigration2025N4
    ex_2025N4_label: Option[String] = None,
    ex_2025N4_group: Option[String] = None,
    ex_2025N4_canvas: Option[String] = None,
    ex_2025N4_rateplan_current: Option[String] = None,
    ex_2025N4_rateplan_target: Option[String] = None,
)

// [18]
//
// Date: July 2024
// Author: Pascal
// comment group: 6157ec78
//
// `doNotProcessUntil` was introduced in July 2024 as a simple way to support
// the "cancellation saves" feature that has been introduced this month and affecting the
// cancellation journey of Supporter Plus subscriptions.
// The default value is `None`, and if a none trivial value is present it represents
// the date until when the item should be left alone and not being processed.

// [19]
//
// Date: June 2025
// Author: Pascal
// Comment: migrationExtraAttributes was introduced to allow a cohort item to hold
// extra attributes that are migration dependent (specifically for the
// Guardian Weekly 2025 migration), for if and when we need to perform
// operations using parameters that are not hold into the Zuora subscription.
// For more details about when and how to use that attribute, see the documentation.

object CohortItem {

  def fromSuccessfulEstimationResult(result: EstimationData): UIO[CohortItem] =
    for {
      thisInstant <- Clock.instant
    } yield CohortItem(
      result.subscriptionName,
      processingStage = EstimationComplete,
      oldPrice = Some(result.oldPrice),
      estimatedNewPrice = Some(result.estimatedNewPrice),
      currency = Some(result.currency),
      amendmentEffectiveDate = Some(result.amendmentEffectiveDate),
      billingPeriod = Some(result.billingPeriod),
      whenEstimationDone = Some(thisInstant)
    )

  def fromNoPriceIncreaseEstimationResult(result: EstimationData): UIO[CohortItem] =
    fromSuccessfulEstimationResult(result).map(_.copy(processingStage = NoPriceIncrease))

  def fromSuccessfulAmendmentResult(result: SuccessfulAmendmentResult): CohortItem =
    CohortItem(
      result.subscriptionNumber,
      processingStage = AmendmentComplete,
      amendmentEffectiveDate = Some(result.amendmentEffectiveDate),
      newPrice = Some(result.newPrice),
      newSubscriptionId = Some(result.newSubscriptionId),
      whenAmendmentDone = Some(result.whenDone)
    )

  def isProcessable(item: CohortItem, today: LocalDate): Boolean = {
    // This function return a boolean indicating whether the item is processable
    // defined as either doNotProcessUntil is None or is a date equal to today or in the past.
    (item.processingStage != DoNotProcessUntil) || {
      item.doNotProcessUntil match {
        case None =>
          throw new Exception(
            s"(error: 588b7698) cohort item: ${item} is in DoNotProcessUntil stage but doesn't have a doNotProcessUntil attribute"
          )
        case Some(date) => date == today || today.isAfter(date)
      }
    }
  }

  def billingPeriodToInt(period: String): Int = {
    // This function is used to convert a CohortItem's billingPeriod in to the number of months
    // that the billing period represents.
    BillingPeriod.fromString(period) match {
      case Monthly    => 1
      case Quarterly  => 3
      case SemiAnnual => 6
      case Annual     => 12
    }
  }
}
