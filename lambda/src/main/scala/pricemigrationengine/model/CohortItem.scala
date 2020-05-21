package pricemigrationengine.model

import java.time.LocalDate

case class CohortItem(
  subscriptionName: String,
  expectedStartDate: Option[LocalDate] = None,
  currency: Option[String] = None,
  oldPrice: Option[BigDecimal] = None,
  estimatedNewPrice: Option[BigDecimal] = None,
  billingPeriod: Option[String] = None
)
