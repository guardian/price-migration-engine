package pricemigrationengine.migrations

import pricemigrationengine.Fixtures
import pricemigrationengine.model._
import pricemigrationengine.model.CohortTableFilter.{NotificationSendDateWrittenToSalesforce, ReadyForEstimation}

import java.time.{Instant, LocalDate}

//Newspaper:
// sub1: "Newspaper Voucher"          "Everyday+"
// val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub1/subscription.json")
// val account = Fixtures.accountFromJson("Migrations/Newspaper2026X/sub1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2026X/sub1/invoice-preview.json")

// sub2: "Newspaper Digital Voucher"  "Everyday+"
// sub3: "Newspaper Delivery"         "Everyday+"
// sub4: "Newspaper Voucher"          "Sixday+"
// sub5: "Newspaper Voucher"          "Weekend+"    "GBP"   "Month"
// sub6: "Newspaper Voucher"          "Everyday"
// sub7: "Newspaper Voucher"          "Sixday"
// sub8: "Newspaper Voucher"          "Sixday+"     "GBP"   "Quarter"
// sub9: "Newspaper Voucher"          "Everyday+"   "GBP"   "Annual"

class Newspaper2026XTest extends munit.FunSuite {
  test("getNewPrice") {
    assertEquals(Newspaper2026X.getNewPrice(Monthly, Voucher, EverydayBasicAndPlus), Some(BigDecimal(72.99)))
    assertEquals(Newspaper2026X.getNewPrice(Quarterly, HomeDelivery, WeekendBasicAndPlus), Some(BigDecimal(110.97)))
  }
  test("decideFulfillment") {
    // sub1: "Newspaper Voucher"          "Everyday+"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub1/subscription.json")
    // Product name is "Newspaper Voucher", so we expect `Voucher`
    assertEquals(
      Newspaper2026X.decideFulfillment(subscription, LocalDate.of(2026, 8, 3)),
      Some(Voucher)
    )
  }
  test("decideFulfillment") {
    // sub3: "Newspaper Delivery"         "Everyday+"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub3/subscription.json")
    // Product name is "Newspaper Delivery", so we expect `Voucher`
    assertEquals(
      Newspaper2026X.decideFulfillment(subscription, LocalDate.of(2026, 8, 3)),
      Some(HomeDelivery)
    )
  }
}
