package pricemigrationengine.model

class NewspaperLegPercentageDistributionTest extends munit.FunSuite {
  test("getDistribution") {
    assertEquals(
      NewspaperLegPercentageDistribution.getDistribution(T3xNewspaperNationalDelivery, T2xSixDay),
      Some(
        List(
          T4xLegPercentage(T1xMonday, BigDecimal(15.9)),
          T4xLegPercentage(T1xTuesday, BigDecimal(15.9)),
          T4xLegPercentage(T1xWednesday, BigDecimal(15.9)),
          T4xLegPercentage(T1xThursday, BigDecimal(15.9)),
          T4xLegPercentage(T1xFriday, BigDecimal(15.9)),
          T4xLegPercentage(T1xSaturday, BigDecimal(20.5)),
        )
      )
    )
    assertEquals(
      NewspaperLegPercentageDistribution.getDistribution(T3xNewspaperVoucher, T2xSunday),
      Some(
        List(
          T4xLegPercentage(T1xSunday, BigDecimal(100.0))
        )
      )
    )
  }
  test("Ratios sums to 100%, newspaperNationalDeliveryLegPercentageMapping") {
    assertEquals(
      NewspaperLegPercentageDistribution.newspaperNationalDeliveryLegPercentageMapping.values.forall(l =>
        l.map(lp => lp.percentage).sum == BigDecimal(100)
      ),
      true
    )
  }
  test("Ratios sums to 100%, newspaperDeliveryLegPercentageMapping") {
    assertEquals(
      NewspaperLegPercentageDistribution.newspaperDeliveryLegPercentageMapping.values.forall(l =>
        l.map(lp => lp.percentage).sum == BigDecimal(100)
      ),
      true
    )
  }
  test("Ratios sums to 100%, newspaperDigitalVoucherLegPercentageMapping") {
    assertEquals(
      NewspaperLegPercentageDistribution.newspaperDigitalVoucherLegPercentageMapping.values.forall(l =>
        l.map(lp => lp.percentage).sum == BigDecimal(100)
      ),
      true
    )
  }
  test("Ratios sums to 100%, newspaperVoucherLegPercentageMapping") {
    assertEquals(
      NewspaperLegPercentageDistribution.newspaperVoucherLegPercentageMapping.values.forall(l =>
        l.map(lp => lp.percentage).sum == BigDecimal(100)
      ),
      true
    )
  }
}
