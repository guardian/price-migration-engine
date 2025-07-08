package pricemigrationengine.migrations

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.Fixtures
import pricemigrationengine.libs.SI2025RateplanFromSubAndInvoices

import java.time.LocalDate

class HomeDelivery2025MigrationTest extends munit.FunSuite {

  test("decoding") {
    val s = """{ "brandTitle": "Label 01" }"""
    val attribute: Newspaper2025ExtraAttributes = upickle.default.read[Newspaper2025ExtraAttributes](s)
    assertEquals(attribute, Newspaper2025ExtraAttributes("Label 01"))
  }

  test("getLabelFromMigrationExtraAttributes (1)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01" }"""),
    )
    val label = HomeDelivery2025Migration.getLabelFromMigrationExtraAttributes(cohortItem)
    assertEquals(label, Some("Label 01"))
  }

  test("getLabelFromMigrationExtraAttributes (2)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01", "removeDiscount": true }"""),
    )
    val label = HomeDelivery2025Migration.getLabelFromMigrationExtraAttributes(cohortItem)
    assertEquals(label, Some("Label 01"))
  }

  test("decideShouldRemoveDiscount (1)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01" }"""),
    )
    val label = HomeDelivery2025Migration.decideShouldRemoveDiscount(cohortItem)
    assertEquals(label, false)
  }

  test("decideShouldRemoveDiscount (2)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "the Guardian", "removeDiscount": true }"""),
    )
    val label = HomeDelivery2025Migration.decideShouldRemoveDiscount(cohortItem)
    assertEquals(label, true)
  }

  test("decideShouldRemoveDiscount (3)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "the Guardian", "removeDiscount": false }"""),
    )
    val label = HomeDelivery2025Migration.decideShouldRemoveDiscount(cohortItem)
    assertEquals(label, false)
  }

  test("priceLookUp") {
    assertEquals(
      HomeDelivery2025Migration.priceLookUp(HomeDelivery2025Everyday, Monthly),
      Some(BigDecimal(83.99))
    )

    assertEquals(
      HomeDelivery2025Migration.priceLookUp(HomeDelivery2025Sixday, SemiAnnual),
      Some(BigDecimal(443.94))
    )

    assertEquals(
      HomeDelivery2025Migration.priceLookUp(HomeDelivery2025Weekend, Quarterly),
      Some(BigDecimal(104.97))
    )

    assertEquals(
      HomeDelivery2025Migration.priceLookUp(HomeDelivery2025Saturday, Monthly),
      Some(BigDecimal(20.99))
    )

    // And we test an undefined combination
    assertEquals(
      HomeDelivery2025Migration.priceLookUp(HomeDelivery2025Saturday, SemiAnnual),
      Some(BigDecimal(125.94))
    )
  }

  // ---------------------------------------------------
  // A-S00252266
  // Newspaper - Home Delivery
  // Sixday
  // Month
  // val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/A-S00252266/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/HomeDelivery2025/A-S00252266/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/A-S00252266/invoice-preview.json")

  // ---------------------------------------------------
  // A-S00256852
  // Newspaper - Home Delivery
  // Weekend
  // Month
  // val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/A-S00256852/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/HomeDelivery2025/A-S00256852/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/A-S00256852/invoice-preview.json")

  // ---------------------------------------------------
  // GA0000464
  // Newspaper - Home Delivery
  // Saturday
  // Quarter
  // val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/GA0000464/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/HomeDelivery2025/GA0000464/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/GA0000464/invoice-preview.json")

  // ---------------------------------------------------
  // GA0004508
  // Newspaper - Home Delivery
  // Weekend
  // Quarter
  // val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/GA0004508/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/HomeDelivery2025/GA0004508/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/GA0004508/invoice-preview.json")

  // ---------------------------------------------------
  // GA0006731
  // Newspaper - Home Delivery
  // Saturday
  // Month
  // val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/GA0006731/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/HomeDelivery2025/GA0006731/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/GA0006731/invoice-preview.json")

  test("decideDeliveryPattern (A-S00252266)") {
    // A-S00252266
    // Newspaper - Home Delivery
    // Sixday
    // Month
    val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/A-S00252266/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/A-S00252266/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan(subscription, invoicePreview).get
    assertEquals(
      HomeDelivery2025Migration.decideDeliveryPattern(ratePlan),
      Some(HomeDelivery2025Sixday)
    )
  }

  test("decideDeliveryPattern (A-S00256852)") {
    // A-S00256852
    // Newspaper - Home Delivery
    // Weekend
    // Month
    val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/A-S00256852/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/A-S00256852/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan(subscription, invoicePreview).get
    assertEquals(
      HomeDelivery2025Migration.decideDeliveryPattern(ratePlan),
      Some(HomeDelivery2025Weekend)
    )
  }

  test("decideDeliveryPattern (GA0000464)") {
    // GA0000464
    // Newspaper - Home Delivery
    // Saturday
    // Quarter
    val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/GA0000464/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/GA0000464/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan(subscription, invoicePreview).get
    assertEquals(
      HomeDelivery2025Migration.decideDeliveryPattern(ratePlan),
      Some(HomeDelivery2025Saturday)
    )
  }

  test("decideDeliveryPattern (GA0004508)") {
    // GA0004508
    // Newspaper - Home Delivery
    // Weekend
    // Quarter
    val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/GA0004508/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/GA0004508/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan(subscription, invoicePreview).get
    assertEquals(
      HomeDelivery2025Migration.decideDeliveryPattern(ratePlan),
      Some(HomeDelivery2025Weekend)
    )
  }

  test("decideDeliveryPattern (GA0006731)") {
    // GA0006731
    // Newspaper - Home Delivery
    // Saturday
    // Month
    val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/GA0006731/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/GA0006731/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan(subscription, invoicePreview).get
    assertEquals(
      HomeDelivery2025Migration.decideDeliveryPattern(ratePlan),
      Some(HomeDelivery2025Saturday)
    )
  }
}
