package pricemigrationengine.model

// Here we define "GuardianWeeklyDeliverySchedule" instead of using
// a billing frequency because the finance data we are basing
// ourselves on doesn't have a clear mapping to billing periods. We have
// - Month
// - Quarter
// - Semi Annual
// - Annual
// - "6 for 6"
sealed trait GuardianWeeklyDeliverySchedule
object GWDSMonth extends GuardianWeeklyDeliverySchedule
object GWDSQuarter extends GuardianWeeklyDeliverySchedule
object GWDSemiAnnual extends GuardianWeeklyDeliverySchedule
object GWDAnnual extends GuardianWeeklyDeliverySchedule
object GWD6For6 extends GuardianWeeklyDeliverySchedule

case class GWDistribution(gardianWeeklyLeg: BigDecimal, digitalPackLeg: BigDecimal)

object GuardianWeeklyLegPercentageDistribution {
  val monthDistributions: Map[(Currency, PricingLocalisation), GWDistribution] = Map(
    ("GPB", Domestic) -> GWDistribution(BigDecimal(60.5), BigDecimal(39.5)),
    ("EUR", Domestic) -> GWDistribution(BigDecimal(67.9), BigDecimal(32.1)),
    ("USD", RestOfWorld) -> GWDistribution(BigDecimal(57.7), BigDecimal(42.3)),
    ("GBP", RestOfWorld) -> GWDistribution(BigDecimal(58.4), BigDecimal(41.6)),
    ("USD", Domestic) -> GWDistribution(BigDecimal(65.1), BigDecimal(34.9)),
    ("CAD", Domestic) -> GWDistribution(BigDecimal(60.9), BigDecimal(39.1)),
    ("AUD", Domestic) -> GWDistribution(BigDecimal(66.5), BigDecimal(33.5)),
    ("NZD", Domestic) -> GWDistribution(BigDecimal(68.7), BigDecimal(31.3))
  )
  val quarterlyDistributions: Map[(Currency, PricingLocalisation), GWDistribution] = Map(
    ("GPB", Domestic) -> GWDistribution(BigDecimal(60.5), BigDecimal(39.5)),
    ("EUR", Domestic) -> GWDistribution(BigDecimal(67.9), BigDecimal(32.1)),
    ("USD", RestOfWorld) -> GWDistribution(BigDecimal(57.7), BigDecimal(42.3)),
    ("GBP", RestOfWorld) -> GWDistribution(BigDecimal(58.4), BigDecimal(41.6)),
    ("USD", Domestic) -> GWDistribution(BigDecimal(65.1), BigDecimal(34.9)),
    ("CAD", Domestic) -> GWDistribution(BigDecimal(60.9), BigDecimal(39.1)),
    ("AUD", Domestic) -> GWDistribution(BigDecimal(66.5), BigDecimal(33.5)),
    ("NZD", Domestic) -> GWDistribution(BigDecimal(68.7), BigDecimal(31.3))
  )
  val semiAnnualDistributions: Map[(Currency, PricingLocalisation), GWDistribution] = Map(
    ("GPB", Domestic) -> GWDistribution(BigDecimal(60.5), BigDecimal(39.5)),
    ("EUR", Domestic) -> GWDistribution(BigDecimal(67.9), BigDecimal(32.1)),
    ("USD", RestOfWorld) -> GWDistribution(BigDecimal(57.7), BigDecimal(42.3)),
    ("GBP", RestOfWorld) -> GWDistribution(BigDecimal(58.4), BigDecimal(41.6)),
    ("USD", Domestic) -> GWDistribution(BigDecimal(65.1), BigDecimal(34.9)),
    ("CAD", Domestic) -> GWDistribution(BigDecimal(60.9), BigDecimal(39.1)),
    ("AUD", Domestic) -> GWDistribution(BigDecimal(66.5), BigDecimal(33.5)),
    ("NZD", Domestic) -> GWDistribution(BigDecimal(68.7), BigDecimal(31.3))
  )
  val annualDistributions: Map[(Currency, PricingLocalisation), GWDistribution] = Map(
    ("GPB", Domestic) -> GWDistribution(BigDecimal(64.8), BigDecimal(35.2)),
    ("EUR", Domestic) -> GWDistribution(BigDecimal(71.7), BigDecimal(28.3)),
    ("USD", RestOfWorld) -> GWDistribution(BigDecimal(62.1), BigDecimal(37.9)),
    ("GBP", RestOfWorld) -> GWDistribution(BigDecimal(62.8), BigDecimal(37.2)),
    ("USD", Domestic) -> GWDistribution(BigDecimal(69.2), BigDecimal(30.8)),
    ("CAD", Domestic) -> GWDistribution(BigDecimal(65.1), BigDecimal(34.9)),
    ("AUD", Domestic) -> GWDistribution(BigDecimal(70.4), BigDecimal(29.6)),
    ("NZD", Domestic) -> GWDistribution(BigDecimal(72.5), BigDecimal(27.5))
  )
  val SixForSixDistributions: Map[(Currency, PricingLocalisation), GWDistribution] = Map(
    ("GPB", Domestic) -> GWDistribution(BigDecimal(60.5), BigDecimal(39.5)),
    ("EUR", Domestic) -> GWDistribution(BigDecimal(67.9), BigDecimal(32.1)),
    ("USD", RestOfWorld) -> GWDistribution(BigDecimal(57.7), BigDecimal(42.3)),
    ("GBP", RestOfWorld) -> GWDistribution(BigDecimal(58.4), BigDecimal(41.6)),
    ("USD", Domestic) -> GWDistribution(BigDecimal(65.1), BigDecimal(34.9)),
    ("CAD", Domestic) -> GWDistribution(BigDecimal(60.9), BigDecimal(39.1)),
    ("AUD", Domestic) -> GWDistribution(BigDecimal(66.5), BigDecimal(33.5)),
    ("NZD", Domestic) -> GWDistribution(BigDecimal(68.7), BigDecimal(31.3))
  )
}
