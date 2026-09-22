package pricemigrationengine.model

class NewspaperLegPercentageDistributionTest extends munit.FunSuite {
  test("getDistribution") {
    assertEquals(
      GuardianWeeklyLegPercentageDistribution.getDistribution(T4xAnnual, "GBP", RestOfWorld),
      Some(T5xDistribution(BigDecimal(62.8), BigDecimal(37.2)))
    )
  }
  test("Percentages sums to 100%, newspaperNationalDeliveryLegPercentageMapping") {
    // This check ensures that in all cases the sum of `percentage` from a List[T4xLegPercentage] is 100
    assertEquals(
      NewspaperLegPercentageDistribution.newspaperNationalDeliveryLegPercentageMapping.values.forall(l =>
        l.map(lp => lp.percentage).sum == BigDecimal(100)
      ),
      true
    )

    assertEquals(
      NewspaperLegPercentageDistribution.newspaperDeliveryLegPercentageMapping.values.forall(l =>
        l.map(lp => lp.percentage).sum == BigDecimal(100)
      ),
      true
    )

    assertEquals(
      NewspaperLegPercentageDistribution.newspaperDigitalVoucherLegPercentageMapping.values.forall(l =>
        l.map(lp => lp.percentage).sum == BigDecimal(100)
      ),
      true
    )

    assertEquals(
      NewspaperLegPercentageDistribution.newspaperVoucherLegPercentageMapping.values.forall(l =>
        l.map(lp => lp.percentage).sum == BigDecimal(100)
      ),
      true
    )
  }
}
