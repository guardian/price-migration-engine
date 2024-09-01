package pricemigrationengine.migrations

import pricemigrationengine.model._

import java.time.LocalDate
import pricemigrationengine.Fixtures
import pricemigrationengine.migrations.SupporterPlus2024Migration

class SupporterPlus2024MigrationTest extends munit.FunSuite {
  test("isInCancellationSave") {
    val subscriptionYes =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-with-cancellation-save/subscription-yes.json")
    val subscriptionNo =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-with-cancellation-save/subscription-no.json")
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
    val subscriptionYes =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-with-cancellation-save/subscription-yes.json")
    val subscriptionNo =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-with-cancellation-save/subscription-no.json")
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
  test("extracting `Supporter Plus V2` rate plan") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/annual/subscription.json")

    val ratePlanCharge1 = ZuoraRatePlanCharge(
      productRatePlanChargeId = "8a128ed885fc6ded01860228f7cb3d5f",
      name = "Supporter Plus Annual Charge",
      number = "C-04674417",
      currency = "AUD",
      price = Some(160.0),
      billingPeriod = Some("Annual"),
      chargedThroughDate = Some(LocalDate.of(2023, 11, 11)),
      processedThroughDate = Some(LocalDate.of(2023, 11, 11)),
      specificBillingPeriod = None,
      endDateCondition = Some("Subscription_End"),
      upToPeriodsType = None,
      upToPeriods = None,
      billingDay = Some("ChargeTriggerDay"),
      triggerEvent = Some("CustomerAcceptance"),
      triggerDate = None,
      discountPercentage = None,
      originalOrderDate = Some(LocalDate.of(2023, 10, 9)),
      effectiveStartDate = Some(LocalDate.of(2023, 11, 11)),
      effectiveEndDate = Some(LocalDate.of(2023, 11, 11))
    )
    val ratePlanCharge2 = ZuoraRatePlanCharge(
      productRatePlanChargeId = "8a12892d85fc6df4018602451322287f",
      name = "Contribution",
      number = "C-04674416",
      currency = "AUD",
      price = Some(0.0),
      billingPeriod = Some("Annual"),
      chargedThroughDate = Some(LocalDate.of(2023, 11, 11)),
      processedThroughDate = Some(LocalDate.of(2023, 11, 11)),
      specificBillingPeriod = None,
      endDateCondition = Some("Subscription_End"),
      upToPeriodsType = None,
      upToPeriods = None,
      billingDay = Some("ChargeTriggerDay"),
      triggerEvent = Some("CustomerAcceptance"),
      triggerDate = None,
      discountPercentage = None,
      originalOrderDate = Some(LocalDate.of(2023, 10, 9)),
      effectiveStartDate = Some(LocalDate.of(2023, 11, 11)),
      effectiveEndDate = Some(LocalDate.of(2023, 11, 11))
    )
    assertEquals(
      SupporterPlus2024Migration.subscriptionRatePlan(subscription),
      Right(
        ZuoraRatePlan(
          id = "8a12820a8c0ff963018c2504b9b75b25",
          productName = "Supporter Plus",
          productRatePlanId = "8a128ed885fc6ded01860228f77e3d5a",
          ratePlanName = "Supporter Plus V2 - Annual",
          ratePlanCharges = List(ratePlanCharge1, ratePlanCharge2),
          lastChangeType = Some("Remove")
        )
      )
    )
  }
  test("priceData") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/annual/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.priceData(subscription),
      Right(PriceData("AUD", 160.0, 200.0, "Annual"))
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
              ratePlanId = "8a12820a8c0ff963018c2504b9b75b25",
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
