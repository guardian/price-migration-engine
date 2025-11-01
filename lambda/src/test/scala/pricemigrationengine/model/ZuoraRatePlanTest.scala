package pricemigrationengine.model

import java.time.LocalDate

class ZuoraRatePlanTest extends munit.FunSuite {

  test("ZuoraRatePlan.ratePlanMaxExpirationDateFromCharges (1)") {
    val ratePlan = ZuoraRatePlan(
      id = "8a12865b96d3500b0196e182a5685157",
      productName = "Guardian Weekly - Domestic",
      productRatePlanId = "2c92a0fe6619b4b301661aa494392ee2",
      ratePlanName = "GW Oct 18 - Quarterly - Domestic",
      ratePlanCharges = List(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fe6619b4b601661aa8b74e623f",
          name = "GW Oct 18 - Quarterly - Domestic",
          number = "C-05719965",
          currency = "USD",
          price = Some(BigDecimal(90.0)),
          billingPeriod = Some("Quarter"),
          chargedThroughDate = Some(LocalDate.of(2025, 8, 18)),
          processedThroughDate = Some(LocalDate.of(2025, 5, 18)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 6, 30)),
          effectiveStartDate = Some(LocalDate.of(2024, 8, 18)),
          effectiveEndDate = Some(LocalDate.of(2026, 5, 18))
        )
      ),
      lastChangeType = Some("Add")
    )
    assertEquals(
      ZuoraRatePlan.ratePlanMaxExpirationDateFromCharges(ratePlan: ZuoraRatePlan),
      Right(LocalDate.of(2026, 5, 18))
    )
  }

  test("ZuoraRatePlan.ratePlanMaxExpirationDateFromCharges (2)") {
    val ratePlan = ZuoraRatePlan(
      id = "8a12865b96d3500b0196e182a5685157",
      productName = "Guardian Weekly - Domestic",
      productRatePlanId = "2c92a0fe6619b4b301661aa494392ee2",
      ratePlanName = "GW Oct 18 - Quarterly - Domestic",
      ratePlanCharges = List(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fe6619b4b601661aa8b74e623f",
          name = "GW Oct 18 - Quarterly - Domestic",
          number = "C-05719965",
          currency = "USD",
          price = Some(BigDecimal(90.0)),
          billingPeriod = Some("Quarter"),
          chargedThroughDate = Some(LocalDate.of(2025, 8, 18)),
          processedThroughDate = Some(LocalDate.of(2025, 5, 18)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 6, 30)),
          effectiveStartDate = None,
          effectiveEndDate = None
        )
      ),
      lastChangeType = Some("Add")
    )
    assertEquals(
      ZuoraRatePlan.ratePlanMaxExpirationDateFromCharges(ratePlan: ZuoraRatePlan).isLeft,
      true
    )
  }

  test("ZuoraRatePlan.ratePlanMaxExpirationDateFromCharges (3)") {
    val ratePlan = ZuoraRatePlan(
      id = "8a12865b96d3500b0196e182a5685157",
      productName = "Guardian Weekly - Domestic",
      productRatePlanId = "2c92a0fe6619b4b301661aa494392ee2",
      ratePlanName = "GW Oct 18 - Quarterly - Domestic",
      ratePlanCharges = List(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fe6619b4b601661aa8b74e623f",
          name = "GW Oct 18 - Quarterly - Domestic",
          number = "C-05719965",
          currency = "USD",
          price = Some(BigDecimal(90.0)),
          billingPeriod = Some("Quarter"),
          chargedThroughDate = Some(LocalDate.of(2025, 8, 18)),
          processedThroughDate = Some(LocalDate.of(2025, 5, 18)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 6, 30)),
          effectiveStartDate = Some(LocalDate.of(2024, 8, 18)),
          effectiveEndDate = Some(LocalDate.of(2027, 6, 19))
        ),
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fe6619b4b601661aa8b74e623f",
          name = "GW Oct 18 - Quarterly - Domestic",
          number = "C-05719965",
          currency = "USD",
          price = Some(BigDecimal(90.0)),
          billingPeriod = Some("Quarter"),
          chargedThroughDate = Some(LocalDate.of(2025, 8, 18)),
          processedThroughDate = Some(LocalDate.of(2025, 5, 18)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 6, 30)),
          effectiveStartDate = Some(LocalDate.of(2024, 8, 18)),
          effectiveEndDate = Some(LocalDate.of(2026, 5, 18))
        )
      ),
      lastChangeType = Some("Add")
    )
    assertEquals(
      ZuoraRatePlan.ratePlanMaxExpirationDateFromCharges(ratePlan: ZuoraRatePlan),
      Right(LocalDate.of(2027, 6, 19))
    )
  }

  test("ZuoraRatePlan.ratePlanIsActiveAndNotExpired (1)") {
    val ratePlan = ZuoraRatePlan(
      id = "8a12865b96d3500b0196e182a5685157",
      productName = "Guardian Weekly - Domestic",
      productRatePlanId = "2c92a0fe6619b4b301661aa494392ee2",
      ratePlanName = "GW Oct 18 - Quarterly - Domestic",
      ratePlanCharges = List(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fe6619b4b601661aa8b74e623f",
          name = "GW Oct 18 - Quarterly - Domestic",
          number = "C-05719965",
          currency = "USD",
          price = Some(BigDecimal(90.0)),
          billingPeriod = Some("Quarter"),
          chargedThroughDate = Some(LocalDate.of(2025, 8, 18)),
          processedThroughDate = Some(LocalDate.of(2025, 5, 18)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 6, 30)),
          effectiveStartDate = Some(LocalDate.of(2024, 8, 18)),
          effectiveEndDate = Some(LocalDate.of(2026, 5, 18))
        )
      ),
      lastChangeType = Some("Remove") // to make it inactive
    )
    assertEquals(
      ZuoraRatePlan.ratePlanIsActiveAndNotExpired(ratePlan: ZuoraRatePlan, LocalDate.of(2025, 11, 1)),
      Right(false) // it's not active because of the `lastChangeType = Some("Remove")`
    )
  }

  test("ZuoraRatePlan.ratePlanIsActiveAndNotExpired (2)") {
    val ratePlan = ZuoraRatePlan(
      id = "8a12865b96d3500b0196e182a5685157",
      productName = "Guardian Weekly - Domestic",
      productRatePlanId = "2c92a0fe6619b4b301661aa494392ee2",
      ratePlanName = "GW Oct 18 - Quarterly - Domestic",
      ratePlanCharges = List(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fe6619b4b601661aa8b74e623f",
          name = "GW Oct 18 - Quarterly - Domestic",
          number = "C-05719965",
          currency = "USD",
          price = Some(BigDecimal(90.0)),
          billingPeriod = Some("Quarter"),
          chargedThroughDate = Some(LocalDate.of(2025, 8, 18)),
          processedThroughDate = Some(LocalDate.of(2025, 5, 18)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 6, 30)),
          effectiveStartDate = Some(LocalDate.of(2024, 8, 18)),
          effectiveEndDate = Some(LocalDate.of(2026, 5, 18))
        )
      ),
      lastChangeType = None // it's active
    )
    assertEquals(
      ZuoraRatePlan.ratePlanIsActiveAndNotExpired(ratePlan: ZuoraRatePlan, LocalDate.of(2025, 11, 1)),
      Right(true) // it's active because of `lastChangeType = None` (that's one of the weirdness with Zuora)
    )
  }

  test("ZuoraRatePlan.ratePlanIsActiveAndNotExpired (3)") {
    val ratePlan = ZuoraRatePlan(
      id = "8a12865b96d3500b0196e182a5685157",
      productName = "Guardian Weekly - Domestic",
      productRatePlanId = "2c92a0fe6619b4b301661aa494392ee2",
      ratePlanName = "GW Oct 18 - Quarterly - Domestic",
      ratePlanCharges = List(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fe6619b4b601661aa8b74e623f",
          name = "GW Oct 18 - Quarterly - Domestic",
          number = "C-05719965",
          currency = "USD",
          price = Some(BigDecimal(90.0)),
          billingPeriod = Some("Quarter"),
          chargedThroughDate = Some(LocalDate.of(2025, 8, 18)),
          processedThroughDate = Some(LocalDate.of(2025, 5, 18)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 6, 30)),
          effectiveStartDate = Some(LocalDate.of(2024, 8, 18)),
          effectiveEndDate = Some(LocalDate.of(2026, 5, 18))
        )
      ),
      lastChangeType = Some("Add") // active
    )
    assertEquals(
      ZuoraRatePlan.ratePlanIsActiveAndNotExpired(ratePlan: ZuoraRatePlan, LocalDate.of(2021, 11, 1)),
      Right(true) // it's active and expiring after today
    )
  }

  test("ZuoraRatePlan.ratePlanIsActiveAndNotExpired (4)") {
    val ratePlan = ZuoraRatePlan(
      id = "8a12865b96d3500b0196e182a5685157",
      productName = "Guardian Weekly - Domestic",
      productRatePlanId = "2c92a0fe6619b4b301661aa494392ee2",
      ratePlanName = "GW Oct 18 - Quarterly - Domestic",
      ratePlanCharges = List(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fe6619b4b601661aa8b74e623f",
          name = "GW Oct 18 - Quarterly - Domestic",
          number = "C-05719965",
          currency = "USD",
          price = Some(BigDecimal(90.0)),
          billingPeriod = Some("Quarter"),
          chargedThroughDate = Some(LocalDate.of(2025, 8, 18)),
          processedThroughDate = Some(LocalDate.of(2025, 5, 18)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 6, 30)),
          effectiveStartDate = Some(LocalDate.of(2024, 8, 18)),
          effectiveEndDate = Some(LocalDate.of(2026, 5, 18))
        )
      ),
      lastChangeType = Some("Add") // active
    )
    assertEquals(
      ZuoraRatePlan.ratePlanIsActiveAndNotExpired(ratePlan: ZuoraRatePlan, LocalDate.of(2027, 11, 1)),
      Right(false) // it's active and expired before today
    )
  }
}
