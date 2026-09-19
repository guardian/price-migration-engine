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
  val monthDistributions: Map[(Currency, SubscriptionLocalisation), GWDistribution] = Map(
    ("GPB", Domestic) -> GWDistribution(BigDecimal(60.5), BigDecimal(39.5)),
    ("EUR", Domestic) -> GWDistribution(BigDecimal(67.9), BigDecimal(32.1)),
    ("USD", RestOfWorld) -> GWDistribution(BigDecimal(57.7), BigDecimal(42.3))
  )

}
