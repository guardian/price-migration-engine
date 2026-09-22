package pricemigrationengine.model

class T6xLegChargeOverridesTest extends munit.FunSuite {
  test("decideT6xLegChargeOverrides") {
    // Test of decideT6xLegChargeOverrides which demonstrates how the arguments combine to make
    // the result. Also refer to comments in the code of that function for more information about
    // the parameters.

    val distribution =
      T5xDistribution(BigDecimal(60.5), BigDecimal(39.5)) // Stole this one from (Monthly, "GBP", Domestic)

    val productRatePlanChargeIdMapping = Map(
      T7xGuardianWeekly -> "5f4afe4e-588b-4f75-9e56-8ffec47bc4a4",
      T7xDigitalPack -> "c7be8c0f-52e4-4375-aa02-75490f9c1acd"
    )

    val billingPeriod = Monthly;

    val targetPrice = BigDecimal(50.1)

    assertEquals(
      T6xLegChargeOverrides
        .decideT6xLegChargeOverrides(distribution, productRatePlanChargeIdMapping, billingPeriod, targetPrice),
      Some(
        List(
          T6xLegChargeOverrides(
            productRatePlanChargeId = "5f4afe4e-588b-4f75-9e56-8ffec47bc4a4",
            price = 30.32,
            billingPeriod = Monthly
          ),
          T6xLegChargeOverrides(
            productRatePlanChargeId = "c7be8c0f-52e4-4375-aa02-75490f9c1acd",
            price = 19.78,
            billingPeriod = Monthly
          )
        )
      )
    )
  }
  test("decideT6xLegChargeOverrides") {

    val legs = List(
      T6xLegChargeOverrides(
        productRatePlanChargeId = "5f4afe4e-588b-4f75-9e56-8ffec47bc4a4",
        price = 30.29,
        billingPeriod = Monthly
      ),
      T6xLegChargeOverrides(
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
      T6xLegChargeOverrides.ensureTotal(legs, targetPrice),
      List(
        T6xLegChargeOverrides(
          productRatePlanChargeId = "5f4afe4e-588b-4f75-9e56-8ffec47bc4a4",
          price = 31.92,
          billingPeriod = Monthly
        ),
        T6xLegChargeOverrides(
          productRatePlanChargeId = "c7be8c0f-52e4-4375-aa02-75490f9c1acd",
          price = 19.78,
          billingPeriod = Monthly
        )
      )
    )
  }
}
