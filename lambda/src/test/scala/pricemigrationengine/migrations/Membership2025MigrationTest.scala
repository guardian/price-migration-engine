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

    // Non standard old price (Non Founder Supporter)

    assertEquals(
      Membership2025Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(5), BigDecimal(10), "Month"))
    )
  }

  test("standard old price detection for sub1") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2025/sub1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Membership2025/sub1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2025/sub1/invoice-preview.json")

    // The standard of old price for uk (GBP monthlies is 7, so we expect a `true` here)

    assertEquals(
      Membership2025Migration.subscriptionHasStandardOldPrice(subscription, invoicePreview),
      Some(true)
    )
  }

  test("standard old price detection for sub2") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2025/sub2/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Membership2025/sub2/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2025/sub2/invoice-preview.json")

    // sub2 is a variation of sub1 with a non standard old price to test the price capping
    // The standard of old price for uk (GBP monthlies is 7, so we expect a `false` here)

    assertEquals(
      Membership2025Migration.subscriptionHasStandardOldPrice(subscription, invoicePreview),
      Some(false)
    )
  }

  test("EstimationHandlerHelper.commsPriceForMembership2025") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2025/sub1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Membership2025/sub1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2025/sub1/invoice-preview.json")

    val cohortSpec = CohortSpec(
      "Membership2025",
      "none", // irrelevant
      LocalDate.of(2025, 10, 16) // irrelevant
    )

    val commsPrice = EstimationHandlerHelper.commsPriceForMembership2025(
      cohortSpec,
      BigDecimal(7), // correct old price for sub1
      BigDecimal(10), // price grid new price
      subscription,
      invoicePreview
    )

    // Here the old price of the sub is the old price of the price grid, so although
    // the jump from 7 to 10 is bigger than the 30% price cap of this migration, we
    // compute the comms price to be the intended new price of 10

    assertEquals(
      commsPrice,
      BigDecimal(10)
    )
  }

  test("EstimationHandlerHelper.commsPriceForMembership2025") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2025/sub2/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Membership2025/sub2/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2025/sub2/invoice-preview.json")

    val cohortSpec = CohortSpec(
      "Membership2025",
      "none", // irrelevant
      LocalDate.of(2025, 10, 16) // irrelevant
    )

    val commsPrice = EstimationHandlerHelper.commsPriceForMembership2025(
      cohortSpec,
      BigDecimal(2.5), // correct old price for sub1
      BigDecimal(10), // price grid new price
      subscription,
      invoicePreview
    )

    // Here the old price of the sub is not the old price of the price grid,
    // We apply the 30% cap

    assertEquals(
      commsPrice,
      BigDecimal(3.25) // 2.5 * 1.3
    )
  }

}
