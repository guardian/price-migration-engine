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
