package pricemigrationengine.migrations

import pricemigrationengine.Fixtures
import pricemigrationengine.model._
import pricemigrationengine.model.CohortTableFilter.{NotificationSendDateWrittenToSalesforce, ReadyForEstimation}

import java.time.{Instant, LocalDate}

// Subscription fixture: GBP-monthly1
// val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2026X/sub1/subscription.json")
// val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2026X/sub1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2026X/sub1/invoice-preview.json")

class GuardianWeekly2026XTest extends munit.FunSuite {
  test("getNewPrice") {
    assertEquals(GuardianWeekly2026X.getNewPrice(Monthly, "GBP", "UK"), Some(BigDecimal(17.50)))
    assertEquals(GuardianWeekly2026X.getNewPrice(Monthly, "AUD", "AU"), Some(BigDecimal(48.00)))
    assertEquals(GuardianWeekly2026X.getNewPrice(Quarterly, "USD", "ROW"), Some(BigDecimal(114)))
    assertEquals(GuardianWeekly2026X.getNewPrice(Annual, "NZD", "NZ"), Some(BigDecimal(720)))
  }
}
