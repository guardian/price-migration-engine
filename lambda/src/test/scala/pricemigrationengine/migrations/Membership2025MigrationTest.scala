package pricemigrationengine.migrations

import pricemigrationengine.Fixtures
import pricemigrationengine.model._

import java.time.{Instant, LocalDate}

class Membership2025MigrationTest extends munit.FunSuite {

  test("price data for sub1") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2025/sub1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Membership2025/sub1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2025/sub1/invoice-preview.json")

    assertEquals(
      Membership2025Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(7), BigDecimal(10), "Month"))
    )
  }

  test("price data for sub2") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2025/sub2/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Membership2025/sub2/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2025/sub2/invoice-preview.json")

    // sub2 is a variation of sub1 with a non standard old price to test the price capping

    assertEquals(
      Membership2025Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(2.5), BigDecimal(10), "Month"))
    )
  }

  test("price data for sub3") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2025/sub3/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Membership2025/sub3/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2025/sub3/invoice-preview.json")

    // Non standard old price and annual

    assertEquals(
      Membership2025Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(75), BigDecimal(100), "Annual"))
    )
  }

  test("price data for sub4") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2025/sub4/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Membership2025/sub4/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2025/sub4/invoice-preview.json")

    // Non standard old price

    assertEquals(
      Membership2025Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(5), BigDecimal(10), "Month"))
    )
  }
}
