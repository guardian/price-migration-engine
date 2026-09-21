package pricemigrationengine.model

// Here we define "GuardianWeeklyDeliverySchedule" instead of using
// a billing frequency because the finance data we are basing
// ourselves on doesn't have a clear mapping to billing periods. We have
// - Month
// - Quarter
// - Semi Annual
// - Annual
// - "6 for 6"
sealed trait T4xGuardianWeeklyPaymentSchedule
object T4xMonth extends T4xGuardianWeeklyPaymentSchedule
object T4xQuarter extends T4xGuardianWeeklyPaymentSchedule
object T4xSemiAnnual extends T4xGuardianWeeklyPaymentSchedule
object T4xAnnual extends T4xGuardianWeeklyPaymentSchedule
object T4x6For6 extends T4xGuardianWeeklyPaymentSchedule

case class T5xDistribution(gardianWeeklyLeg: BigDecimal, digitalPackLeg: BigDecimal)

object GuardianWeeklyLegPercentageDistribution {
  val monthDistributions: Map[(Currency, PricingLocalisation), T5xDistribution] = Map(
    ("GBP", Domestic) -> T5xDistribution(BigDecimal(60.5), BigDecimal(39.5)),
    ("EUR", Domestic) -> T5xDistribution(BigDecimal(67.9), BigDecimal(32.1)),
    ("USD", RestOfWorld) -> T5xDistribution(BigDecimal(57.7), BigDecimal(42.3)),
    ("GBP", RestOfWorld) -> T5xDistribution(BigDecimal(58.4), BigDecimal(41.6)),
    ("USD", Domestic) -> T5xDistribution(BigDecimal(65.1), BigDecimal(34.9)),
    ("CAD", Domestic) -> T5xDistribution(BigDecimal(60.9), BigDecimal(39.1)),
    ("AUD", Domestic) -> T5xDistribution(BigDecimal(66.5), BigDecimal(33.5)),
    ("NZD", Domestic) -> T5xDistribution(BigDecimal(68.7), BigDecimal(31.3))
  )
  val quarterlyDistributions: Map[(Currency, PricingLocalisation), T5xDistribution] = Map(
    ("GBP", Domestic) -> T5xDistribution(BigDecimal(60.5), BigDecimal(39.5)),
    ("EUR", Domestic) -> T5xDistribution(BigDecimal(67.9), BigDecimal(32.1)),
    ("USD", RestOfWorld) -> T5xDistribution(BigDecimal(57.7), BigDecimal(42.3)),
    ("GBP", RestOfWorld) -> T5xDistribution(BigDecimal(58.4), BigDecimal(41.6)),
    ("USD", Domestic) -> T5xDistribution(BigDecimal(65.1), BigDecimal(34.9)),
    ("CAD", Domestic) -> T5xDistribution(BigDecimal(60.9), BigDecimal(39.1)),
    ("AUD", Domestic) -> T5xDistribution(BigDecimal(66.5), BigDecimal(33.5)),
    ("NZD", Domestic) -> T5xDistribution(BigDecimal(68.7), BigDecimal(31.3))
  )
  val semiAnnualDistributions: Map[(Currency, PricingLocalisation), T5xDistribution] = Map(
    ("GBP", Domestic) -> T5xDistribution(BigDecimal(60.5), BigDecimal(39.5)),
    ("EUR", Domestic) -> T5xDistribution(BigDecimal(67.9), BigDecimal(32.1)),
    ("USD", RestOfWorld) -> T5xDistribution(BigDecimal(57.7), BigDecimal(42.3)),
    ("GBP", RestOfWorld) -> T5xDistribution(BigDecimal(58.4), BigDecimal(41.6)),
    ("USD", Domestic) -> T5xDistribution(BigDecimal(65.1), BigDecimal(34.9)),
    ("CAD", Domestic) -> T5xDistribution(BigDecimal(60.9), BigDecimal(39.1)),
    ("AUD", Domestic) -> T5xDistribution(BigDecimal(66.5), BigDecimal(33.5)),
    ("NZD", Domestic) -> T5xDistribution(BigDecimal(68.7), BigDecimal(31.3))
  )
  val annualDistributions: Map[(Currency, PricingLocalisation), T5xDistribution] = Map(
    ("GBP", Domestic) -> T5xDistribution(BigDecimal(64.8), BigDecimal(35.2)),
    ("EUR", Domestic) -> T5xDistribution(BigDecimal(71.7), BigDecimal(28.3)),
    ("USD", RestOfWorld) -> T5xDistribution(BigDecimal(62.1), BigDecimal(37.9)),
    ("GBP", RestOfWorld) -> T5xDistribution(BigDecimal(62.8), BigDecimal(37.2)),
    ("USD", Domestic) -> T5xDistribution(BigDecimal(69.2), BigDecimal(30.8)),
    ("CAD", Domestic) -> T5xDistribution(BigDecimal(65.1), BigDecimal(34.9)),
    ("AUD", Domestic) -> T5xDistribution(BigDecimal(70.4), BigDecimal(29.6)),
    ("NZD", Domestic) -> T5xDistribution(BigDecimal(72.5), BigDecimal(27.5))
  )
  val SixForSixDistributions: Map[(Currency, PricingLocalisation), T5xDistribution] = Map(
    ("GBP", Domestic) -> T5xDistribution(BigDecimal(60.5), BigDecimal(39.5)),
    ("EUR", Domestic) -> T5xDistribution(BigDecimal(67.9), BigDecimal(32.1)),
    ("USD", RestOfWorld) -> T5xDistribution(BigDecimal(57.7), BigDecimal(42.3)),
    ("GBP", RestOfWorld) -> T5xDistribution(BigDecimal(58.4), BigDecimal(41.6)),
    ("USD", Domestic) -> T5xDistribution(BigDecimal(65.1), BigDecimal(34.9)),
    ("CAD", Domestic) -> T5xDistribution(BigDecimal(60.9), BigDecimal(39.1)),
    ("AUD", Domestic) -> T5xDistribution(BigDecimal(66.5), BigDecimal(33.5)),
    ("NZD", Domestic) -> T5xDistribution(BigDecimal(68.7), BigDecimal(31.3))
  )
}
