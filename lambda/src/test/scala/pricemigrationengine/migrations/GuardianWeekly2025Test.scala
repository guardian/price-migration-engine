package pricemigrationengine.migrations

import pricemigrationengine.model._

import java.time.LocalDate

// Subscription fixture: GBP-monthly1
// val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/subscription.json")
// val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/invoice-preview.json")

// Subscription fixture: EUR-annual1
// val subscription = Fixtures.subscriptionFromJson("Migrations/EUR-annual1/GBP-monthly1/subscription.json")
// val account = Fixtures.accountFromJson("Migrations/EUR-annual1/GBP-monthly1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("Migrations/EUR-annual1/GBP-monthly1/invoice-preview.json")

class GuardianWeekly2025Test extends munit.FunSuite {

  test("priceLookUp") {
    assertEquals(
      1,
      1
    )
  }
}
