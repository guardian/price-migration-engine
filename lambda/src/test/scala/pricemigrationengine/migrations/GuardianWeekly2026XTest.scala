package pricemigrationengine.migrations

import pricemigrationengine.Fixtures
import pricemigrationengine.model._
import pricemigrationengine.model.CohortTableFilter.{NotificationSendDateWrittenToSalesforce, ReadyForEstimation}

import java.time.{Instant, LocalDate}

// sub1: "Guardian Weekly - Domestic"  "GW Oct 18 - Quarterly - Domestic"  "GBP"  "Quarter"
// val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2026X/sub1/subscription.json")
// val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2026X/sub1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2026X/sub1/invoice-preview.json")

// sub2: "Guardian Weekly - Domestic"  "GW Oct 18 - Quarterly - Domestic"  "AUD"  "Quarter"
// sub3: "Guardian Weekly - Domestic"  "GW Oct 18 - Quarterly - Domestic"  "EUR"  "Quarter"
// sub4: "Guardian Weekly - Domestic"  "GW Oct 18 - Monthly - Domestic"    "GBP"  "Month"
// sub5: "Guardian Weekly - Domestic"  "GW Oct 18 - Annual - Domestic"     "GBP"  "Annual"

class GuardianWeekly2026XTest extends munit.FunSuite {
  test("getNewPrice") {
    assertEquals(GuardianWeekly2026X.getNewPrice(Monthly, "GBP", Domestic), Some(BigDecimal(17.50)))
    assertEquals(GuardianWeekly2026X.getNewPrice(Monthly, "AUD", Domestic), Some(BigDecimal(48.00)))
    assertEquals(GuardianWeekly2026X.getNewPrice(Quarterly, "USD", RestOfWorld), Some(BigDecimal(114)))
    assertEquals(GuardianWeekly2026X.getNewPrice(Annual, "NZD", Domestic), Some(BigDecimal(720)))
  }
  test("getNewPrice") {
    // sub1: "Guardian Weekly - Domestic"  "GW Oct 18 - Quarterly - Domestic"  "GBP"  "Quarter"
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2026X/sub1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2026X/sub1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2026X/sub1/invoice-preview.json")
    // Quarter, GPB, Domestic, we are expecting 52
    assertEquals(GuardianWeekly2026X.getNewPrice(subscription, invoicePreview, account), Some(BigDecimal(52)))
  }
  test("getNewPrice") {
    // sub2: "Guardian Weekly - Domestic"  "GW Oct 18 - Quarterly - Domestic"  "AUD"  "Quarter"
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2026X/sub2/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2026X/sub2/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2026X/sub2/invoice-preview.json")
    // Quarter, AUD, Domestic, we are expecting 114
    assertEquals(GuardianWeekly2026X.getNewPrice(subscription, invoicePreview, account), Some(BigDecimal(144)))
  }
  test("getNewPrice") {
    // sub4: "Guardian Weekly - Domestic"  "GW Oct 18 - Monthly - Domestic"    "GBP"  "Month"
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2026X/sub4/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2026X/sub4/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2026X/sub4/invoice-preview.json")
    // Month, GBP, Domestic, we are expecting 17.50
    assertEquals(GuardianWeekly2026X.getNewPrice(subscription, invoicePreview, account), Some(BigDecimal(17.50)))
  }
}
