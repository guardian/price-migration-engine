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
}
