package pricemigrationengine.model

class GuardianWeeklyLegPercentageDistributionTest extends munit.FunSuite {
  test("getDistribution") {
    assertEquals(
      GuardianWeeklyLegPercentageDistribution.getDistribution(T4xMonth, "USD", Domestic),
      Some(T5xDistribution(BigDecimal(65.1), BigDecimal(34.9)))
    )
    assertEquals(
      GuardianWeeklyLegPercentageDistribution.getDistribution(T4xMonth, "CAD", Domestic),
      Some(T5xDistribution(BigDecimal(60.9), BigDecimal(39.1)))
    )
  }
}
