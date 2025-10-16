package pricemigrationengine.migrations

import pricemigrationengine.Fixtures
import pricemigrationengine.model._

import java.time.{Instant, LocalDate}

class Membership2025MigrationTest extends munit.FunSuite {

  test("thing") {

    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2025/sub1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Membership2025/sub1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2025/sub1/invoice-preview.json")

    assertEquals(
      Membership2025Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(3), BigDecimal(10), "Month"))
    )
  }
}
