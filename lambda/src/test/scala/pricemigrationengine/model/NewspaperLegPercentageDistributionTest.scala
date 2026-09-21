package pricemigrationengine.model

class NewspaperLegPercentageDistributionTest extends munit.FunSuite {
  test("getDistribution") {
    assertEquals(
      GuardianWeeklyLegPercentageDistribution.getDistribution(T4xAnnual, "GBP", RestOfWorld),
      Some(T5xDistribution(BigDecimal(62.8), BigDecimal(37.2)))
    )
  }
}
