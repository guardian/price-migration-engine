package pricemigrationengine.migrations

import pricemigrationengine.model._

import java.time.LocalDate
import pricemigrationengine.Fixtures
import pricemigrationengine.migrations.SupporterPlus2024Migration

class SupporterPlus2024MigrationTest extends munit.FunSuite {
  test("isInCancellationSave") {
    val subscriptionNo =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-with-cancellation-save/subscription-no.json")
    val subscriptionYes =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-with-cancellation-save/subscription-yes.json")
    assertEquals(
      SupporterPlus2024Migration.isInCancellationSave(subscriptionNo),
      false
    )
    assertEquals(
      SupporterPlus2024Migration.isInCancellationSave(subscriptionYes),
      true
    )
  }
  test("cancellationSaveEffectiveDate") {
    val subscriptionNo =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-with-cancellation-save/subscription-no.json")
    val subscriptionYes =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-with-cancellation-save/subscription-yes.json")
    assertEquals(
      SupporterPlus2024Migration.cancellationSaveEffectiveDate(subscriptionNo),
      None
    )
    assertEquals(
      SupporterPlus2024Migration.cancellationSaveEffectiveDate(subscriptionYes),
      Some(LocalDate.of(2024, 7, 5))
    )
  }
  test("Price Grid (Old)") {
    assertEquals(
      SupporterPlus2024Migration.getOldPrice(Monthly, "USD"),
      Some(13.0)
    )
    assertEquals(
      SupporterPlus2024Migration.getOldPrice(Annual, "EUR"),
      Some(95.0)
    )
  }
  test("Price Grid (New)") {
    assertEquals(
      SupporterPlus2024Migration.getNewPrice(Monthly, "USD"),
      Some(15.0)
    )
    assertEquals(
      SupporterPlus2024Migration.getNewPrice(Annual, "EUR"),
      Some(120.0)
    )
  }

  // The monthly is GBP without a contribution [10, 0]
  // The annual is a AUD with contribution [160, 340]

  test("extracting `Supporter Plus V2` rate plan (monthly)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/monthly/subscription.json")
    val ratePlanCharge1 = ZuoraRatePlanCharge(
      productRatePlanChargeId = "8a128ed885fc6ded018602296af13eba",
      name = "Supporter Plus Monthly Charge",
      number = "C-04648407",
      currency = "GBP",
      price = Some(10.0),
      billingPeriod = Some("Month"),
      chargedThroughDate = Some(LocalDate.of(2024, 9, 30)),
      processedThroughDate = Some(LocalDate.of(2024, 8, 30)),
      specificBillingPeriod = None,
      endDateCondition = Some("Subscription_End"),
      upToPeriodsType = None,
      upToPeriods = None,
      billingDay = Some("ChargeTriggerDay"),
      triggerEvent = Some("CustomerAcceptance"),
      triggerDate = None,
      discountPercentage = None,
      originalOrderDate = Some(LocalDate.of(2023, 10, 1)),
      effectiveStartDate = Some(LocalDate.of(2023, 9, 30)),
      effectiveEndDate = Some(LocalDate.of(2025, 2, 27))
    )
    val ratePlanCharge2 = ZuoraRatePlanCharge(
      productRatePlanChargeId = "8a128d7085fc6dec01860234cd075270",
      name = "Contribution",
      number = "C-04648406",
      currency = "GBP",
      price = Some(0.0),
      billingPeriod = Some("Month"),
      chargedThroughDate = Some(LocalDate.of(2024, 9, 30)),
      processedThroughDate = Some(LocalDate.of(2024, 8, 30)),
      specificBillingPeriod = None,
      endDateCondition = Some("Subscription_End"),
      upToPeriodsType = None,
      upToPeriods = None,
      billingDay = Some("ChargeTriggerDay"),
      triggerEvent = Some("CustomerAcceptance"),
      triggerDate = None,
      discountPercentage = None,
      originalOrderDate = Some(LocalDate.of(2023, 10, 1)),
      effectiveStartDate = Some(LocalDate.of(2023, 9, 30)),
      effectiveEndDate = Some(LocalDate.of(2025, 2, 27))
    )
    assertEquals(
      SupporterPlus2024Migration.supporterPlusV2RatePlan(subscription),
      Right(
        ZuoraRatePlan(
          id = "8a12908b8dd07f56018de8f4950923b8",
          productName = "Supporter Plus",
          productRatePlanId = "8a128ed885fc6ded018602296ace3eb8",
          ratePlanName = "Supporter Plus V2 - Monthly",
          ratePlanCharges = List(ratePlanCharge1, ratePlanCharge2),
          lastChangeType = Some("Add")
        )
      )
    )
  }
  test("extracting `Supporter Plus V2` rate plan (annual)") {
    // The original subscription price is 160, the normal old price,
    // but I edited the annual/subscription.json it to 150 to make sure we
    // read the right price from the subscription.

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/annual/subscription.json")
    val ratePlanCharge1 = ZuoraRatePlanCharge(
      productRatePlanChargeId = "8a128ed885fc6ded01860228f7cb3d5f",
      name = "Supporter Plus Annual Charge",
      number = "C-04819663",
      currency = "AUD",
      price = Some(150.0),
      billingPeriod = Some("Annual"),
      chargedThroughDate = Some(LocalDate.of(2024, 11, 11)),
      processedThroughDate = Some(LocalDate.of(2023, 11, 11)),
      specificBillingPeriod = None,
      endDateCondition = Some("Subscription_End"),
      upToPeriodsType = None,
      upToPeriods = None,
      billingDay = Some("ChargeTriggerDay"),
      triggerEvent = Some("CustomerAcceptance"),
      triggerDate = None,
      discountPercentage = None,
      originalOrderDate = Some(LocalDate.of(2023, 11, 26)),
      effectiveStartDate = Some(LocalDate.of(2023, 11, 11)),
      effectiveEndDate = Some(LocalDate.of(2024, 11, 11))
    )
    val ratePlanCharge2 = ZuoraRatePlanCharge(
      productRatePlanChargeId = "8a12892d85fc6df4018602451322287f",
      name = "Contribution",
      number = "C-04819662",
      currency = "AUD",
      price = Some(340.0),
      billingPeriod = Some("Annual"),
      chargedThroughDate = Some(LocalDate.of(2024, 11, 11)),
      processedThroughDate = Some(LocalDate.of(2023, 11, 11)),
      specificBillingPeriod = None,
      endDateCondition = Some("Subscription_End"),
      upToPeriodsType = None,
      upToPeriods = None,
      billingDay = Some("ChargeTriggerDay"),
      triggerEvent = Some("CustomerAcceptance"),
      triggerDate = None,
      discountPercentage = None,
      originalOrderDate = Some(LocalDate.of(2023, 11, 26)),
      effectiveStartDate = Some(LocalDate.of(2023, 11, 11)),
      effectiveEndDate = Some(LocalDate.of(2024, 11, 11))
    )
    assertEquals(
      SupporterPlus2024Migration.supporterPlusV2RatePlan(subscription),
      Right(
        ZuoraRatePlan(
          id = "8a12820a8c0ff963018c2504ba045b2f",
          productName = "Supporter Plus",
          productRatePlanId = "8a128ed885fc6ded01860228f77e3d5a",
          ratePlanName = "Supporter Plus V2 - Annual",
          ratePlanCharges = List(ratePlanCharge1, ratePlanCharge2),
          lastChangeType = Some("Add")
        )
      )
    )
  }
  test("priceData (monthly)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/monthly/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.priceData(subscription),
      Right(PriceData("GBP", 10.0, 12.0, "Month"))
    )
  }
  test("priceData (annual)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/annual/subscription.json")

    // The original subscription price is 160, the normal old price,
    // but I edited the annual/subscription.json it to 150 to make sure we
    // read the right price from the subscription.

    // In this case we have the price from the subscription and the target price.

    assertEquals(
      SupporterPlus2024Migration.priceData(subscription),
      Right(PriceData("AUD", 150.0, 200.0, "Annual"))
    )
  }
  test("zuoraUpdate") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/annual/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.zuoraUpdate(subscription, LocalDate.of(2019, 1, 1)),
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "8a128ed885fc6ded01860228f77e3d5a",
              contractEffectiveDate = LocalDate.of(2019, 1, 1),
              chargeOverrides = List(
                ChargeOverride(
                  productRatePlanChargeId = "8a128ed885fc6ded01860228f7cb3d5f",
                  billingPeriod = "Annual",
                  price = 120.0
                )
              )
            )
          ),
          remove = List(
            RemoveZuoraRatePlan(
              ratePlanId = "8a12820a8c0ff963018c2504ba045b2f",
              contractEffectiveDate = LocalDate.of(2019, 1, 1)
            )
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }
}
