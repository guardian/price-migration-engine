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
    cancellationReason: Option[String] = None
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
