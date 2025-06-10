package pricemigrationengine.migrations

import pricemigrationengine.Fixtures
import pricemigrationengine.model._
import pricemigrationengine.libs._

import java.time.LocalDate

// Subscription fixture: GBP-monthly1
// val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/subscription.json")
// val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/invoice-preview.json")

// Subscription fixture: EUR-annual1
// val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/EUR-annual1/subscription.json")
// val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2025/EUR-annual1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/EUR-annual1/invoice-preview.json")

class GuardianWeekly2025MigrationTest extends munit.FunSuite {

  // For this migration we are using the (new) SubscriptionIntrospection2025, which
  // performs some heavy lifting, consequently we can focus on the migration function
  // themselves.

  test("GuardianWeekly2025Migration.priceLookUp") {
    assertEquals(
      GuardianWeekly2025Migration.priceLookUp(Domestic, Annual, "CAD"),
      Some(BigDecimal(432))
    )

    assertEquals(
      GuardianWeekly2025Migration.priceLookUp(RestOfWorld, Quarterly, "GBP"),
      Some(BigDecimal(83.9))
    )
  }

  test("GuardianWeekly2025Migration.subscriptionToLastPriceMigrationDate (GBP-monthly1)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/subscription.json")

    // Here we only have one active rate plan
    // "GW Oct 18 - Monthly - Domestic",
    // "originalOrderDate": "2024-04-13"

    assertEquals(
      GuardianWeekly2025Migration.subscriptionToLastPriceMigrationDate(subscription),
      Some(LocalDate.of(2024, 4, 13))
    )
  }

  test("GuardianWeekly2025Migration.subscriptionToLastPriceMigrationDate (EUR-annual1)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/EUR-annual1/subscription.json")

    // Here we only have one active rate plan (note that there also is an active discount)
    // "ratePlanName": "GW Oct 18 - Annual - Domestic",
    // "originalOrderDate": "2024-11-05",

    assertEquals(
      GuardianWeekly2025Migration.subscriptionToLastPriceMigrationDate(subscription),
      Some(LocalDate.of(2024, 11, 5))
    )
  }

  test("GuardianWeekly2025Migration.priceData (GBP-monthly1)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/invoice-preview.json")

    // Currency from subscription: "currency": "GBP"
    // Old price from currency: active rate plan, one single rate plan charge: "price": 15.0
    // price lookup (new price): (Monthly, "GBP") -> BigDecimal(16.5)
    // Billing frequency from currency: active rate plan, rate plan charge: "billingPeriod": "Month"

    assertEquals(
      GuardianWeekly2025Migration.priceData(subscription, invoicePreview, account),
      Right(PriceData("GBP", BigDecimal(15.0), BigDecimal(16.5), "Month"))
    )
  }

}
