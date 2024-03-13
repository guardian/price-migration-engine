package pricemigrationengine.migrations

import pricemigrationengine.model._

import java.time.LocalDate
import pricemigrationengine.Fixtures
import pricemigrationengine.migrations.GW2024Migration
import pricemigrationengine.migrations.GW2024Migration

class GW2024MigrationTest extends munit.FunSuite {

  test("Price lookup (version 1) is correct") {
    assertEquals(
      GW2024Migration.getNewPrice(Monthly, "GBP"),
      Some(BigDecimal(15))
    )
    assertEquals(
      GW2024Migration.getNewPrice(Quarterly, "ROW (USD)"),
      Some(BigDecimal(99))
    )
  }

  // -------------------------------------

  test("Rate plan determination is correct (standard)") {
    val subscription = Fixtures.subscriptionFromJson("GW2024/standard/subscription.json")
    assertEquals(
      GW2024Migration.subscriptionToMigrationRatePlan(subscription),
      Some(
        ZuoraRatePlan(
          id = "8a128d6988b434050188d210ef897dc2",
          productName = "Guardian Weekly - Domestic",
          productRatePlanId = "2c92a0fe6619b4b901661aa8e66c1692",
          ratePlanName = "GW Oct 18 - Annual - Domestic",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "2c92a0fe6619b4b901661aa8e6811695",
              name = "GW Oct 18 - Annual - Domestic",
              number = "C-02169680",
              currency = "USD",
              price = Some(BigDecimal(300.0)),
              billingPeriod = Some("Annual"),
              chargedThroughDate = Some(LocalDate.of(2024, 6, 19)),
              processedThroughDate = Some(LocalDate.of(2023, 6, 19)),
              specificBillingPeriod = None,
              endDateCondition = Some("Subscription_End"),
              upToPeriodsType = None,
              upToPeriods = None,
              billingDay = Some("ChargeTriggerDay"),
              triggerEvent = Some("CustomerAcceptance"),
              triggerDate = None,
              discountPercentage = None,
              originalOrderDate = Some(LocalDate.of(2020, 6, 8))
            )
          ),
          lastChangeType = Some("Add")
        )
      )
    )
  }

  // -------------------------------------

  test("subscriptionToMigrationCurrency is correct (standard)") {
    val subscription = Fixtures.subscriptionFromJson("GW2024/standard/subscription.json")
    val account = Fixtures.accountFromJson("GW2024/standard/account.json")
    assertEquals(
      GW2024Migration.subscriptionToCurrency(subscription, account),
      Some("USD")
    )
  }

  test("subscriptionToMigrationCurrency is correct (ROW-DomesticRatePlan)") {
    val subscription = Fixtures.subscriptionFromJson("GW2024/ROW-DomesticRatePlan/subscription.json")
    val account = Fixtures.accountFromJson("GW2024/ROW-DomesticRatePlan/account.json")
    assertEquals(
      GW2024Migration.subscriptionToCurrency(subscription, account),
      Some("USD")
    )
  }

  // -------------------------------------

  test("isROW is correct (standard: USD paying from the US)") {
    val subscription = Fixtures.subscriptionFromJson("GW2024/standard/subscription.json")
    val account = Fixtures.accountFromJson("GW2024/standard/account.json")
    assertEquals(
      GW2024Migration.isROW(subscription, account),
      Some(false)
    )
  }

  test("isROW is correct (row: USD paying from Hong Kong)") {
    val subscription = Fixtures.subscriptionFromJson("GW2024/ROW-DomesticRatePlan/subscription.json")
    val account = Fixtures.accountFromJson("GW2024/ROW-DomesticRatePlan/account.json")
    assertEquals(
      GW2024Migration.isROW(subscription, account),
      Some(true)
    )
  }

  test("isROW is correct (row: USD paying from United Arab Emirates)") {
    val subscription = Fixtures.subscriptionFromJson("GW2024/ROW-DomesticRatePlan/subscription.json")
    val account = Fixtures.accountFromJson("GW2024/ROW-DomesticRatePlan/account.json")
    assertEquals(
      GW2024Migration.isROW(subscription, account),
      Some(true)
    )
  }

  // -------------------------------------

  test("subscriptionToExtendedCurrency (standard)") {
    val subscription = Fixtures.subscriptionFromJson("GW2024/standard/subscription.json")
    val account = Fixtures.accountFromJson("GW2024/standard/account.json")
    assertEquals(
      GW2024Migration.subscriptionToExtendedCurrency(subscription, account),
      Some("USD")
    )
  }

  test("subscriptionToExtendedCurrency (ROW-DomesticRatePlan)") {
    val subscription = Fixtures.subscriptionFromJson("GW2024/ROW-DomesticRatePlan/subscription.json")
    val account = Fixtures.accountFromJson("GW2024/ROW-DomesticRatePlan/account.json")
    assertEquals(
      GW2024Migration.subscriptionToExtendedCurrency(subscription, account),
      Some("ROW (USD)")
    )
  }

  // -------------------------------------

  test("subscriptionToBillingPeriod") {
    val subscription = Fixtures.subscriptionFromJson("GW2024/standard/subscription.json")
    assertEquals(
      GW2024Migration.subscriptionToBillingPeriod(subscription),
      Some(Annual)
    )
  }

  // -------------------------------------

  test("getNewPrice (version 2) (standard)") {
    val subscription = Fixtures.subscriptionFromJson("GW2024/standard/subscription.json")
    val account = Fixtures.accountFromJson("GW2024/standard/account.json")
    assertEquals(
      GW2024Migration.getNewPrice(subscription, account),
      Some(BigDecimal(360))
    )
  }

  test("getNewPrice (version 2) (ROW-DomesticRatePlan)") {
    val subscription = Fixtures.subscriptionFromJson("GW2024/ROW-DomesticRatePlan/subscription.json")
    val account = Fixtures.accountFromJson("GW2024/ROW-DomesticRatePlan/account.json")
    assertEquals(
      GW2024Migration.getNewPrice(subscription, account),
      Some(BigDecimal(396))
    )
  }

  // -------------------------------------

  test("priceData (standard)") {
    val subscription = Fixtures.subscriptionFromJson("GW2024/standard/subscription.json")
    val account = Fixtures.accountFromJson("GW2024/standard/account.json")
    assertEquals(
      GW2024Migration.priceData(subscription, account),
      Right(PriceData("USD", BigDecimal(300), BigDecimal(360), "Annual"))
    )
  }

  test("priceData (ROW-DomesticRatePlan)") {
    val subscription = Fixtures.subscriptionFromJson("GW2024/ROW-DomesticRatePlan/subscription.json")
    val account = Fixtures.accountFromJson("GW2024/ROW-DomesticRatePlan/account.json")
    assertEquals(
      GW2024Migration.priceData(subscription, account),
      Right(PriceData("USD", BigDecimal(300), BigDecimal(396), "Annual"))
    )
  }
}
