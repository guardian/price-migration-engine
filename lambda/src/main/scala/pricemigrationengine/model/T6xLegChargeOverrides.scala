package pricemigrationengine.model

// T6xLegChargeOverrides carries the information needed to build a charge overrides as part
// of making the amendment payload of the Orders API. It can be thought of as an intermediary
// representation between the Guardian Weekly (GuardianWeeklyLegPercentageDistribution) and the
// Newspaper (NewspaperLegPercentageDistribution) pricing percentage distributions,
// and the Value used in the JSON payload

case class T6xLegChargeOverrides(productRatePlanChargeId: String, price: BigDecimal, billingPeriod: BillingPeriod)
