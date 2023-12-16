package pricemigrationengine.migrations

import pricemigrationengine.model._

import java.time.LocalDate
import pricemigrationengine.Fixtures
import pricemigrationengine.handlers.NotificationHandler.thereIsEnoughNotificationLeadTime
import pricemigrationengine.migrations.Newspaper2024Migration._
import pricemigrationengine.model.CohortTableFilter.SalesforcePriceRiceCreationComplete

class Newspaper2024MigrationTest extends munit.FunSuite {
  test("Newspaper2024Migration: Price lookup is correct") {
    assertEquals(priceLookup("Newspaper - Home Delivery", Monthly, "Weekend"), Some(BigDecimal(31.99)))
    assertEquals(priceLookup("Newspaper - Voucher Book", Annual, "Sixday+"), Some(BigDecimal(707.88)))
    assertEquals(priceLookup("Non existent product", Annual, "Weekend"), None)
  }
}
