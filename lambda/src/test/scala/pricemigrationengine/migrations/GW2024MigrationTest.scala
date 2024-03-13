package pricemigrationengine.migrations

import pricemigrationengine.model._

import java.time.LocalDate
import pricemigrationengine.Fixtures
import pricemigrationengine.migrations.GW2024Migration
import pricemigrationengine.migrations.GW2024Migration
import pricemigrationengine.util.StartDates

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

  // ------------------------------------

  test("last price rise date") {
    val subscription = Fixtures.subscriptionFromJson("GW2024/standard/subscription.json")
    assertEquals(
      GW2024Migration.subscriptionToLastPriceMigrationDate(subscription),
      Some(LocalDate.of(2020, 6, 8))
    )
  }

  test("last price rise date") {
    val subscription = Fixtures.subscriptionFromJson("GW2024/NotTwoPriceRisesWithinAYear/subscription.json")
    assertEquals(
      GW2024Migration.subscriptionToLastPriceMigrationDate(subscription),
      Some(LocalDate.of(2023, 7, 5))
    )
  }

  // ------------------------------------

  test("StartDate Policy (trivial case)") {
    val subscription = Fixtures.subscriptionFromJson("GW2024/standard/subscription.json")
    // The last price rise date for this subscription is LocalDate.of(2020, 6, 8)
    // That's more that a year ago at the time these lines are written (2024-03-13)
    // Therefore the StartDates policy function is not going to increase the lower bound if
    // we set the lower bound to today: 2024-03-13.

    val cohortSpec = CohortSpec("GW2024", "", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1))
    assertEquals(
      StartDates.noPriceRiseWithinAYearOfLastPriceRisePolicyUpdate(cohortSpec, subscription, LocalDate.of(2024, 3, 13)),
      LocalDate.of(2024, 3, 13)
    )
  }

  test("StartDate Policy (non trivial case)") {
    val subscription = Fixtures.subscriptionFromJson("GW2024/NotTwoPriceRisesWithinAYear/subscription.json")
    // The last price rise date for this subscription is LocalDate.of(2023, 7, 5)
    // That's less than a year ago at the time these lines are written (2024-03-13)
    // Therefore the StartDates policy function is going to increase the lower bound to 2024-07-05 (one year after
    // last price rise) if we present it with a previous lower bound of 2024-03-13.

    val cohortSpec = CohortSpec("GW2024", "", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1))
    assertEquals(
      StartDates.noPriceRiseWithinAYearOfLastPriceRisePolicyUpdate(cohortSpec, subscription, LocalDate.of(2024, 3, 13)),
      LocalDate.of(2024, 7, 5)
    )
  }
}
