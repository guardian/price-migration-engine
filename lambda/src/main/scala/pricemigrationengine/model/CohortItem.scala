package pricemigrationengine.model

import pricemigrationengine.model.CohortTableFilter._
import zio.{Clock, UIO}

import java.time.{Instant, LocalDate}

case class CohortItem(
    subscriptionName: String,
    processingStage: CohortTableFilter,
    currency: Option[Currency] = None,

    // Pre migration price
    oldPrice: Option[BigDecimal] = None,

    // Price derived from the Estimation step, without capping
    estimatedNewPrice: Option[BigDecimal] = None,

    // Price (with possible capping) used in the communication to the user and sent to Salesforce
    commsPrice: Option[BigDecimal] = None,

    // Price read from the post amendment subscription
    newPrice: Option[BigDecimal] = None,

    //
    billingPeriod: Option[String] = None,
    amendmentEffectiveDate: Option[LocalDate] = None,
    salesforcePriceRiseId: Option[String] = None,
    newSubscriptionId: Option[ZuoraSubscriptionId] = None,

    // comment group: 6157ec78
    // `doNotProcessUntil` was introduced in July 2024 as a simple way to support
    // the "cancellation saves" feature that has been introduced this month and affecting the
    // cancellation journey of Supporter Plus subscriptions.
    // The default value is `None`, and if a none trivial value is present it represents
    // the date until when the item should be left alone and not being processed.
    doNotProcessUntil: Option[LocalDate] = None,

    // migrationExtraAttributes was introduced to allow a cohort item to hold
    // extra attributes that are migration dependent (specifically for the
    // Guardian Weekly 2025 migration), for if and when we need to perform
    // operations using parameters that are not hold into the Zuora subscription.
    // For more details about when and how to use that attribute, see the documentation.
    migrationExtraAttributes: Option[String] = None,

    //
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

object CohortItem {

  def fromSuccessfulEstimationResult(result: EstimationData): UIO[CohortItem] =
    for {
      thisInstant <- Clock.instant
    } yield CohortItem(
      result.subscriptionName,
      processingStage = EstimationComplete,
      oldPrice = Some(result.oldPrice),
      estimatedNewPrice = Some(result.estimatedNewPrice),
      commsPrice = Some(result.commsPrice),
      currency = Some(result.currency),
      amendmentEffectiveDate = Some(result.amendmentEffectiveDate),
      billingPeriod = Some(result.billingPeriod),
      whenEstimationDone = Some(thisInstant)
    )

  def fromNoPriceIncreaseEstimationResult(result: EstimationData): UIO[CohortItem] =
    fromSuccessfulEstimationResult(result).map(_.copy(processingStage = NoPriceIncrease))

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
