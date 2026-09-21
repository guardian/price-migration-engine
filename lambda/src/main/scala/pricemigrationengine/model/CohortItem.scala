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

    // Membership2025
    ex_membership2025_country: Option[String] = None,
)

object CohortItem {
  def fromSuccessfulEstimationData(result: EstimationData): UIO[CohortItem] =
    for {
      thisInstant <- Clock.instant
    } yield CohortItem(
      result.subscriptionName,
      processingStage = EstimationComplete,
      oldPrice = Some(result.oldPrice),
      estimatedNewPrice = Some(result.newPriceFull),
      commsPrice = Some(result.commsPrice),
      currency = Some(result.currency),
      amendmentEffectiveDate = Some(result.amendmentEffectiveDate),
      billingPeriod = Some(result.billingPeriod),
      whenEstimationDone = Some(thisInstant)
    )

  def fromNoPriceIncreaseEstimationData(result: EstimationData): UIO[CohortItem] =
    fromSuccessfulEstimationData(result).map(_.copy(processingStage = NoPriceIncrease))
}
