package pricemigrationengine.model

import java.time.{Instant, LocalDate}

case class CohortItem(
  subscriptionName: String,
  processingStage: CohortTableFilter,
  expectedStartDate: Option[LocalDate] = None,
  currency: Option[String] = None,
  oldPrice: Option[BigDecimal] = None,
  estimatedNewPrice: Option[BigDecimal] = None,
  billingPeriod: Option[String] = None,
  whenEstimationDone: Option[Instant] = None,
  salesforcePriceRiseId: Option[String] = None,
  whenSfShowEstimate: Option[Instant] = None,
  startDate: Option[LocalDate] = None,
  newPrice: Option[BigDecimal] = None,
  newSubscriptionId: Option[ZuoraSubscriptionId] = None,
  whenAmendmentDone: Option[Instant] = None
)
