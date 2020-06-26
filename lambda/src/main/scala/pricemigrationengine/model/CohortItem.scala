package pricemigrationengine.model

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
    whenAmendmentWrittenToSalesforce: Option[Instant] = None
)
