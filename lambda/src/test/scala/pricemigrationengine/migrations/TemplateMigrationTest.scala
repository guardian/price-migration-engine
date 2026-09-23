package pricemigrationengine.migrations

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.Fixtures

import java.time.LocalDate

class TemplateMigrationTest extends munit.FunSuite {
  // ----------------------------------------------------
  // SI2025Templates
  // ----------------------------------------------------

  test("TemplateMigration.priceData") {
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/TemplateMigration/subscription1/subscription.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("Migrations/TemplateMigration/subscription1/invoice-preview.json")
    val priceData = TemplateMigration.priceData(CohortSpec("Test1", true), subscription, invoicePreview)
    assertEquals(priceData, Right(PriceData("USD", BigDecimal(90.0), BigDecimal(2.71), BigDecimal(2.55), "Quarter")))
  }
}
