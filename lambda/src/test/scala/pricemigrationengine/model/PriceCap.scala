package pricemigrationengine.model

import pricemigrationengine.model.PriceCap

import java.time.LocalDate

class LegacyMigrationsTest extends munit.FunSuite {

  test("The price legacy capping function works correctly (default)") {
    val oldPrice = BigDecimal(100)
    val cappedPrice = BigDecimal(120)
    val uncappedPrice = BigDecimal(156)
    // Note the implicit price capping at 20%
    assertEquals(cappedPrice, PriceCap.priceCapLegacy(oldPrice, uncappedPrice))
  }

  test("The legacy price capping works correctly in case of force estimated") {
    val oldPrice = BigDecimal(100)
    val uncappedPrice = BigDecimal(156)
    // Note the implicit price capping at 20%
    assertEquals(uncappedPrice, PriceCap.priceCapLegacy(oldPrice, uncappedPrice, true))
  }

  test("priceCapNotification (no need to apply)") {
    val oldPrice = BigDecimal(100)
    val newPrice = BigDecimal(110)
    val cap = 1.25
    assertEquals(
      PriceCap.priceCapForNotification(oldPrice, newPrice, cap),
      BigDecimal(110)
    )
  }

  test("priceCapNotification (need to apply)") {
    val oldPrice = BigDecimal(100)
    val newPrice = BigDecimal(250)
    val cap = 1.25
    assertEquals(
      PriceCap.priceCapForNotification(oldPrice, newPrice, cap),
      BigDecimal(125)
    )
  }

  test("priceCorrectionFactor (trivial case)") {
    // The new price is lower than the old price multiplied by the price cap multiplier.
    // We expect a correction factor equal to 1, for price invariance
    assertEquals(
      PriceCap.priceCorrectionFactor(BigDecimal(50), BigDecimal(55), BigDecimal(1.2)),
      1.0
    )
  }

  test("priceCorrectionFactor") {
    // The capped price is 50 * 1.2 = 60
    // The new price is 120, which wee need to multiply by 0.5 to get to the capped price
    // Therefore the price correction factor is 0.5
    assertEquals(
      PriceCap.priceCorrectionFactor(BigDecimal(50), BigDecimal(120), BigDecimal(1.2)),
      0.5
    )
  }

  test("updateChargeOverride") {
    val chargeOverride = ChargeOverride("productRatePlanChargeId", "Monthly", BigDecimal(200))
    val correctionFactor = 0.9
    assertEquals(
      PriceCap.updateChargeOverride(chargeOverride, correctionFactor),
      ChargeOverride("productRatePlanChargeId", "Monthly", BigDecimal(180))
    )
  }

  test("updateAddZuoraRatePlan (no ChargeOverrides)") {
    val chargeOverride = ChargeOverride("productRatePlanChargeId", "Monthly", BigDecimal(200))
    val addZuoraRatePlan = AddZuoraRatePlan("productRatePlanId", LocalDate.of(2024, 3, 11), Nil)
    val correctionFactor = 0.9

    // We do not actually expect any change here, because the chargeOverrides are not specified
    // This would be a mistake in the migration code, but nothing to do with the updateAddZuoraRatePlan
    // function itself which must be invariant in this case

    assertEquals(
      PriceCap.updateAddZuoraRatePlan(addZuoraRatePlan, correctionFactor),
      addZuoraRatePlan
    )
  }

  test("updateAddZuoraRatePlan (with two)") {
    val chargeOverride1 = ChargeOverride("productRatePlanChargeId", "Monthly", BigDecimal(200))
    val chargeOverride2 = ChargeOverride("productRatePlanChargeId", "Monthly", BigDecimal(100))
    val addZuoraRatePlan =
      AddZuoraRatePlan("productRatePlanId", LocalDate.of(2024, 3, 11), List(chargeOverride1, chargeOverride2))
    val correctionFactor = 0.9

    assertEquals(
      PriceCap.updateAddZuoraRatePlan(addZuoraRatePlan, correctionFactor),
      AddZuoraRatePlan(
        "productRatePlanId",
        LocalDate.of(2024, 3, 11),
        List(
          ChargeOverride("productRatePlanChargeId", "Monthly", BigDecimal(180)),
          ChargeOverride("productRatePlanChargeId", "Monthly", BigDecimal(90))
        )
      )
    )
  }

  test("priceCapForAmendment (without correction)") {

    val chargeOverride1 = ChargeOverride("productRatePlanChargeId", "Monthly", BigDecimal(25))
    val chargeOverride2 = ChargeOverride("productRatePlanChargeId", "Monthly", BigDecimal(30))
    val addZuoraRatePlan =
      AddZuoraRatePlan("productRatePlanId", LocalDate.of(2024, 3, 11), List(chargeOverride1, chargeOverride2))
    val zuoraUpdate = ZuoraSubscriptionUpdate(
      add = List(addZuoraRatePlan),
      remove = List(),
      currentTerm = None,
      currentTermPeriodType = None
    )

    // The charges in chargeOverride1, and chargeOverride2 were chosen to equal 55, the (uncapped) new price.

    val oldPrice = BigDecimal(50)
    val newPrice = BigDecimal(55)
    val cap = BigDecimal(1.2)

    // We expect a correction factor equal to 1, resulting in no change in the zuoraUpdate

    assertEquals(
      PriceCap.priceCorrectionFactor(oldPrice, newPrice, cap),
      1.0
    )

    // With a correction factor of 0.5, we have 45 and 15 as corrected prices in the charges

    assertEquals(
      PriceCap.priceCapForAmendment(oldPrice, newPrice, cap, zuoraUpdate),
      zuoraUpdate
    )
  }

  test("priceCapForAmendment (with correction)") {

    val chargeOverride1 = ChargeOverride("productRatePlanChargeId", "Monthly", BigDecimal(90))
    val chargeOverride2 = ChargeOverride("productRatePlanChargeId", "Monthly", BigDecimal(30))
    val addZuoraRatePlan =
      AddZuoraRatePlan("productRatePlanId", LocalDate.of(2024, 3, 11), List(chargeOverride1, chargeOverride2))
    val zuoraUpdate = ZuoraSubscriptionUpdate(
      add = List(addZuoraRatePlan),
      remove = List(),
      currentTerm = None,
      currentTermPeriodType = None
    )

    // The charges in chargeOverride1, and chargeOverride2 were chosen to equal 120, the (uncapped) new price.

    val oldPrice = BigDecimal(50)
    val newPrice = BigDecimal(120)
    val cap = BigDecimal(1.2)

    // We expect a correction factor equal to 0.5

    assertEquals(
      PriceCap.priceCorrectionFactor(oldPrice, newPrice, cap),
      0.5
    )

    // With a correction factor of 0.5, we have 45 and 15 as corrected prices in the charges

    assertEquals(
      PriceCap.priceCapForAmendment(oldPrice, newPrice, cap, zuoraUpdate),
      ZuoraSubscriptionUpdate(
        add = List(
          AddZuoraRatePlan(
            "productRatePlanId",
            LocalDate.of(2024, 3, 11),
            List(
              ChargeOverride("productRatePlanChargeId", "Monthly", BigDecimal(45)),
              ChargeOverride("productRatePlanChargeId", "Monthly", BigDecimal(15))
            )
          )
        ),
        remove = List(),
        currentTerm = None,
        currentTermPeriodType = None
      )
    )
  }

}
