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
  test("decidePackage") {
    // sub1: "Newspaper Voucher"          "Everyday+"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub1/subscription.json")
    assertEquals(
      Newspaper2026X.decidePackage(subscription, LocalDate.of(2026, 8, 3)),
      Some(EverydayBasicAndPlus)
    )
  }
  test("decidePackage") {
    // sub4: "Newspaper Voucher"          "Sixday+"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub4/subscription.json")
    assertEquals(
      Newspaper2026X.decidePackage(subscription, LocalDate.of(2026, 8, 3)),
      Some(SixdayBasicAndPlus)
    )
  }
  test("decidePackage") {
    // sub6: "Newspaper Voucher"          "Everyday"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub6/subscription.json")
    assertEquals(
      Newspaper2026X.decidePackage(subscription, LocalDate.of(2026, 8, 3)),
      Some(EverydayBasicAndPlus)
    )
  }
  test("decidePackage") {
    // sub7: "Newspaper Voucher"          "Sixday"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub7/subscription.json")
    assertEquals(
      Newspaper2026X.decidePackage(subscription, LocalDate.of(2026, 8, 3)),
      Some(SixdayBasicAndPlus)
    )
  }
  // -----------
  test("decideBrandTitle") {
    // sub1: "Newspaper Voucher"          "Everyday+"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub1/subscription.json")
    assertEquals(
      Newspaper2026X.decideBrandTitle(subscription, LocalDate.of(2026, 8, 3)),
      Some("the Guardian and the Observer")
    )
  }
  test("decideBrandTitle") {
    // sub4: "Newspaper Voucher"          "Sixday+"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub4/subscription.json")
    assertEquals(
      Newspaper2026X.decideBrandTitle(subscription, LocalDate.of(2026, 8, 3)),
      Some("the Guardian")
    )
  }
  test("decideBrandTitle") {
    // sub6: "Newspaper Voucher"          "Everyday"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub6/subscription.json")
    assertEquals(
      Newspaper2026X.decideBrandTitle(subscription, LocalDate.of(2026, 8, 3)),
      Some("the Guardian and the Observer")
    )
  }
  test("decideBrandTitle") {
    // sub7: "Newspaper Voucher"          "Sixday"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub7/subscription.json")
    assertEquals(
      Newspaper2026X.decideBrandTitle(subscription, LocalDate.of(2026, 8, 3)),
      Some("the Guardian")
    )
  }
  test("priceData") {
    // sub1: "Newspaper Voucher"          "Everyday+"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2026X/sub1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2026X/sub1/invoice-preview.json")
    assertEquals(
      Newspaper2026X.priceData(
        subscription,
        invoicePreview,
        account: ZuoraAccount,
        LocalDate.of(2026, 9, 14)
      ),
      Right(PriceData("GBP", BigDecimal(69.99), BigDecimal(72.99), "Month"))
    )
  }
  test("priceData") {
    // sub7: "Newspaper Voucher"          "Sixday"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub7/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2026X/sub7/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2026X/sub7/invoice-preview.json")
    assertEquals(
      Newspaper2026X.priceData(
        subscription,
        invoicePreview,
        account: ZuoraAccount,
        LocalDate.of(2026, 9, 14)
      ),
      Right(PriceData("GBP", BigDecimal(61.99), BigDecimal(64.99), "Month"))
    )
  }
  test("priceData") {
    // sub8: "Newspaper Voucher"          "Sixday+"     "GBP"   "Quarter"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub8/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2026X/sub8/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2026X/sub8/invoice-preview.json")
    assertEquals(
      Newspaper2026X.priceData(
        subscription,
        invoicePreview,
        account: ZuoraAccount,
        LocalDate.of(2026, 9, 14)
      ),
      Right(PriceData("GBP", BigDecimal(185.97), BigDecimal(194.97), "Quarter"))
    )
  }
  test("priceData") {
    // sub9: "Newspaper Voucher"          "Everyday+"   "GBP"   "Annual"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub9/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2026X/sub9/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2026X/sub9/invoice-preview.json")
    assertEquals(
      Newspaper2026X.priceData(
        subscription,
        invoicePreview,
        account: ZuoraAccount,
        LocalDate.of(2026, 9, 14)
      ),
      Right(PriceData("GBP", BigDecimal(839.88), BigDecimal(875.88), "Annual"))
    )
  }
}
