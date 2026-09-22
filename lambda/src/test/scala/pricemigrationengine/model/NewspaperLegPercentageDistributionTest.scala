package pricemigrationengine.model

class NewspaperLegPercentageDistributionTest extends munit.FunSuite {
  test("getDistribution") {
    assertEquals(
      NewspaperLegPercentageDistribution.getDistribution(T3xNewspaperNationalDelivery, T2xSixDay),
      Some(
        List(
          T4xLeg(T1xMonday, BigDecimal(15.9)),
          T4xLeg(T1xTuesday, BigDecimal(15.9)),
          T4xLeg(T1xWednesday, BigDecimal(15.9)),
          T4xLeg(T1xThursday, BigDecimal(15.9)),
          T4xLeg(T1xFriday, BigDecimal(15.9)),
          T4xLeg(T1xSaturday, BigDecimal(20.5)),
        )
      )
    )

    test("getDistribution") {
      assertEquals(
        NewspaperLegPercentageDistribution.getDistribution(T3xNewspaperDelivery, T2xSixDayPlus),
        Some(
          List(
            T4xLeg(T1xMonday, BigDecimal(13.4)),
            T4xLeg(T1xTuesday, BigDecimal(13.4)),
            T4xLeg(T1xWednesday, BigDecimal(13.4)),
            T4xLeg(T1xThursday, BigDecimal(13.4)),
            T4xLeg(T1xFriday, BigDecimal(13.4)),
            T4xLeg(T1xSaturday, BigDecimal(17.2)),
            T4xLeg(T1xDigitalPack, BigDecimal(15.8)),
          )
        )
      )
    }
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
