package pricemigrationengine.model

class GuardianWeeklyLegPercentageDistributionTest extends munit.FunSuite {
  test("getDistribution") {
    assertEquals(
      GuardianWeeklyLegPercentageDistribution.getDistribution(T4xAnnual, "GBP", RestOfWorld),
      Some(T5xDistribution(BigDecimal(62.8), BigDecimal(37.2)))
    )
  }
  test("getDistribution") {
    assertEquals(
      GuardianWeeklyLegPercentageDistribution.getDistribution(T4xMonth, "USD", Domestic),
      Some(T5xDistribution(BigDecimal(65.1), BigDecimal(34.9)))
    )
    assertEquals(
      GuardianWeeklyLegPercentageDistribution.getDistribution(T4xQuarter, "CAD", Domestic),
      Some(T5xDistribution(BigDecimal(60.9), BigDecimal(39.1)))
    )
    assertEquals(
      GuardianWeeklyLegPercentageDistribution.getDistribution(T4xAnnual, "CAD", Domestic),
      Some(T5xDistribution(BigDecimal(65.1), BigDecimal(34.9)))
    )
    assertEquals(
      GuardianWeeklyLegPercentageDistribution.getDistribution(T4x6For6, "CAD", Domestic),
      Some(T5xDistribution(BigDecimal(60.9), BigDecimal(39.1)))
    )
  }
}
