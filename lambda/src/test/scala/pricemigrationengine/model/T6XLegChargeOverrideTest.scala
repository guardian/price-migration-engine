package pricemigrationengine.model

import pricemigrationengine.migrations.T9xGWRatePlanIds

class T6XLegChargeOverrideTest extends munit.FunSuite {
  test("ensureTotal") {

    val legs = List(
      T6xLegChargeOverride(
        productRatePlanChargeId = "5f4afe4e-588b-4f75-9e56-8ffec47bc4a4",
        price = 30.29,
        billingPeriod = Monthly
      ),
      T6xLegChargeOverride(
        productRatePlanChargeId = "c7be8c0f-52e4-4375-aa02-75490f9c1acd",
        price = 19.78,
        billingPeriod = Monthly
      )
    )

    val targetPrice = BigDecimal(51.7)

    // The target price is at 51.7, but the current sum from the two legs is
    // 30.29 + 19.78 = 50.07
    // We are missing 51.7 - 50.07 = 1.63
    // Adding that to the first leg we get 30.29 + 1.63 = 31.92
    // The other legs remains unchanged.

    assertEquals(
      T6xLegChargeOverride.ensureTotal(legs, targetPrice),
      List(
        T6xLegChargeOverride(
          productRatePlanChargeId = "5f4afe4e-588b-4f75-9e56-8ffec47bc4a4",
          price = 31.92,
          billingPeriod = Monthly
        ),
        T6xLegChargeOverride(
          productRatePlanChargeId = "c7be8c0f-52e4-4375-aa02-75490f9c1acd",
          price = 19.78,
          billingPeriod = Monthly
        )
      )
    )
  }
  test("decideT6xLegChargeOverrides (Guardian Weekly variant)") {
    // Test of decideT6xLegChargeOverrides in the Guardian Weekly case,
    // which demonstrates how the arguments combine to make the result.
    // For more details, also refer to comments in the code of that function.

    // Stole this one from GuardianWeeklyLegPercentageDistribution (Monthly, "GBP", Domestic)
    val distribution: T5xFinanceAllocation =
      T5xFinanceAllocation(BigDecimal(60.5), BigDecimal(39.5))

    val t9xGWRatePlanIds = T9xGWRatePlanIds(
      "8a128e57a0e6b38d01a0e798347978b0",
      "8a128e57a0e6b38d01a0e79834fd78ba", // GW
      "8a128e57a0e6b38d01a0e79834c778b2" // Digital Pack
    )

    val billingPeriod = Monthly;

    val targetPrice = BigDecimal(50.1)

    assertEquals(
      T6xLegChargeOverride
        .decideT6xLegChargeOverridesGuardianWeekly(
          distribution,
          t9xGWRatePlanIds,
          billingPeriod,
          targetPrice
        ),
      List(
        T6xLegChargeOverride(
          productRatePlanChargeId = "8a128e57a0e6b38d01a0e79834fd78ba", // GW
          price = 30.32,
          billingPeriod = Monthly
        ),
        T6xLegChargeOverride(
          productRatePlanChargeId = "8a128e57a0e6b38d01a0e79834c778b2", // Digital Pack
          price = 19.78,
          billingPeriod = Monthly
        )
      )
    )
  }
  test("decideT6xLegChargeOverrides (Newspaper variant)") {
    // Test of decideT6xLegChargeOverrides in the Newspaper case,
    // which demonstrates how the arguments combine to make the result.
    // For more details, also refer to comments in the code of that function.

    // Stole this one from NewspaperLegPercentageDistribution
    // newspaperNationalDeliveryLegPercentageMapping, T2xWeekendPlus
    val distribution: List[T4xLeg] =
      List(
        T4xLeg(T1xSaturday, BigDecimal(34.2)),
        T4xLeg(T1xSunday, BigDecimal(34.2)),
        T4xLeg(T1xDigitalPack, BigDecimal(31.6)),
      )

    val productRatePlanChargeIdMapping: Map[T1xNewspaperLegType, String] = Map(
      T1xSaturday -> "5f4afe4e-588b-4f75-9e56-8ffec47bc4a4",
      T1xSunday -> "c7be8c0f-52e4-4375-aa02-75490f9c1acd",
      T1xDigitalPack -> "6b0ddb38-aea0-4c3a-b247-3a0b8be23939"
    )

    val billingPeriod = Annual

    val targetPrice = BigDecimal(230.8)

    assertEquals(
      T6xLegChargeOverride
        .decideT6xLegChargeOverridesNewspaper(distribution, productRatePlanChargeIdMapping, billingPeriod, targetPrice),
      Some(
        List(
          T6xLegChargeOverride(
            productRatePlanChargeId = "5f4afe4e-588b-4f75-9e56-8ffec47bc4a4",
            price = 78.94,
            billingPeriod = Annual
          ),
          T6xLegChargeOverride(
            productRatePlanChargeId = "c7be8c0f-52e4-4375-aa02-75490f9c1acd",
            price = 78.93,
            billingPeriod = Annual
          ),
          T6xLegChargeOverride(
            productRatePlanChargeId = "6b0ddb38-aea0-4c3a-b247-3a0b8be23939",
            price = 72.93,
            billingPeriod = Annual
          )
        )
      )
    )
  }
  test("decideT6xLegChargeOverrides (Newspaper variant)") {
    // This test is like the previous one, but we use an incomplete mapping and get a None

    val distribution: List[T4xLeg] =
      List(
        T4xLeg(T1xSaturday, BigDecimal(34.2)),
        T4xLeg(T1xSunday, BigDecimal(34.2)),
        T4xLeg(T1xDigitalPack, BigDecimal(31.6)),
      )

    val productRatePlanChargeIdMapping: Map[T1xNewspaperLegType, String] = Map(
      T1xSaturday -> "5f4afe4e-588b-4f75-9e56-8ffec47bc4a4",
      // missing T1xSunday
      // missing T1xDigitalPack
    )

    val billingPeriod = Annual

    val targetPrice = BigDecimal(230.8)

    assertEquals(
      T6xLegChargeOverride
        .decideT6xLegChargeOverridesNewspaper(distribution, productRatePlanChargeIdMapping, billingPeriod, targetPrice),
      None
    )
  }
}
