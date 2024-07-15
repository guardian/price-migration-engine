package pricemigrationengine.model

import pricemigrationengine.model.CohortTableFilter._
import zio.{Clock, UIO}

import java.time.{Instant, LocalDate}

case class CohortItem(
    subscriptionName: String,
    processingStage: CohortTableFilter,
    startDate: Option[LocalDate] = None,
    currency: Option[Currency] = None,
    oldPrice: Option[BigDecimal] = None,
    estimatedNewPrice: Option[BigDecimal] = None,
    billingPeriod: Option[String] = None,
    whenEstimationDone: Option[Instant] = None,
    salesforcePriceRiseId: Option[String] = None,
    whenSfShowEstimate: Option[Instant] = None,
    newPrice: Option[BigDecimal] = None,
    newSubscriptionId: Option[ZuoraSubscriptionId] = None,
    whenAmendmentDone: Option[Instant] = None,
    whenNotificationSent: Option[Instant] = None,
    whenNotificationSentWrittenToSalesforce: Option[Instant] = None,
    whenAmendmentWrittenToSalesforce: Option[Instant] = None,
    cancellationReason: Option[String] = None,
    doNotProcessUntil: Option[Instant] = None // [18]
)

// [18]
//
// Date: July 2024
// Author: Pascal
//
// `doNotProcessUntil` was introduced in July 2024 as a simple way to support
// the "cancellation saves" feature that has been introduced this month and affecting the
// cancellation journey of Supporter Plus subscriptions.
// The default value is `None`, and if a none trivial value is present it represents
// the instant until when the item should be left alone and not being processed.

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
      startDate = Some(result.startDate),
      billingPeriod = Some(result.billingPeriod),
      whenEstimationDone = Some(thisInstant)
    )

  def fromNoPriceIncreaseEstimationResult(result: EstimationData): UIO[CohortItem] =
    fromSuccessfulEstimationResult(result).map(_.copy(processingStage = NoPriceIncrease))

  def fromFailedEstimationResult(result: FailedEstimationResult): CohortItem =
    CohortItem(result.subscriptionNumber, EstimationFailed)

  def fromCancelledEstimationResult(result: CancelledEstimationResult, reason: String): CohortItem =
    CohortItem(result.subscriptionNumber, processingStage = Cancelled, cancellationReason = Some(reason))

  def fromSuccessfulAmendmentResult(result: SuccessfulAmendmentResult): CohortItem =
    CohortItem(
      result.subscriptionNumber,
      processingStage = AmendmentComplete,
      startDate = Some(result.startDate),
      newPrice = Some(result.newPrice),
      newSubscriptionId = Some(result.newSubscriptionId),
      whenAmendmentDone = Some(result.whenDone)
    )

  def fromCancelledAmendmentResult(result: CancelledAmendmentResult, reason: String): CohortItem =
    CohortItem(
      result.subscriptionNumber,
      processingStage = Cancelled,
      cancellationReason = Some(reason)
    )

  def fromExpiringSubscriptionResult(result: ExpiringSubscriptionResult): CohortItem =
    CohortItem(result.subscriptionNumber, Cancelled)
}
