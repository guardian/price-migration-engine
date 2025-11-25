package pricemigrationengine.migrations

import pricemigrationengine.Fixtures
import pricemigrationengine.model.CohortTableFilter
import pricemigrationengine.model._

import java.time.{Instant, LocalDate}

class DigiSubs2025MigrationTest extends munit.FunSuite {

  // 01 : Digital Pack Monthly   : 15/06/2016 : Monthly
  // val subscription = Fixtures.subscriptionFromJson("Migrations/DigiSubs2025/01/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/DigiSubs2025/01/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/DigiSubs2025/01/invoice-preview.json")

  // 02 : Digital Pack Quarterly : 14/06/2016 : Quarterly
  // val subscription = Fixtures.subscriptionFromJson("Migrations/DigiSubs2025/02/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/DigiSubs2025/02/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/DigiSubs2025/02/invoice-preview.json")

  // 03 : Digital Pack Annual    : 12/09/2025 : Annually
  // val subscription = Fixtures.subscriptionFromJson("Migrations/DigiSubs2025/03/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/DigiSubs2025/03/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/DigiSubs2025/03/invoice-preview.json")

  // 04 : Digital Pack Monthly   : 19/11/2025 : Monthly
  // val subscription = Fixtures.subscriptionFromJson("Migrations/DigiSubs2025/04/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/DigiSubs2025/04/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/DigiSubs2025/04/invoice-preview.json")

  test("init") {
    assertEquals(
      true,
      true
    )
  }
}
