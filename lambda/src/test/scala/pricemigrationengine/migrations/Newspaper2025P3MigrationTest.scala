package pricemigrationengine.migrations

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.Fixtures
import pricemigrationengine.libs.{SI2025RateplanFromSub, SI2025RateplanFromSubAndInvoices}

import java.time.LocalDate

class Newspaper2025P3MigrationTest extends munit.FunSuite {

  // val s = """{ "brandTitle": "the Guardian" }"""
  // val s = """{ "brandTitle": "the Guardian and the Observer" }"""
  // val s = """{ "brandTitle": "the Guardian", "removeDiscount": true }"""

  test("decoding") {
    val s = """{ "brandTitle": "Label 01" }"""
    val attribute: Newspaper2025P3ExtraAttributes = upickle.default.read[Newspaper2025P3ExtraAttributes](s)
    assertEquals(attribute, Newspaper2025P3ExtraAttributes("Label 01", None))
  }

  test("getLabelFromMigrationExtraAttributes (1)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01" }"""),
    )
    val label = Newspaper2025P3Migration.getLabelFromMigrationExtraAttributes(cohortItem)
    assertEquals(label, Some("Label 01"))
  }

  test("getLabelFromMigrationExtraAttributes (2)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01", "removeDiscount": true }"""),
    )
    val label = Newspaper2025P3Migration.getLabelFromMigrationExtraAttributes(cohortItem)
    assertEquals(label, Some("Label 01"))
  }

  test("decideShouldRemoveDiscount (2)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01" }"""),
    )
    val label = Newspaper2025P3Migration.decideShouldRemoveDiscount(cohortItem)
    assertEquals(label, false)
  }

  test("decideShouldRemoveDiscount (2)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01", "removeDiscount": true }"""),
    )
    val label = Newspaper2025P3Migration.decideShouldRemoveDiscount(cohortItem)
    assertEquals(label, true)
  }

  test("decideShouldRemoveDiscount (3)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01", "removeDiscount": false }"""),
    )
    val label = Newspaper2025P3Migration.decideShouldRemoveDiscount(cohortItem)
    assertEquals(label, false)
  }

  test("priceLookUp") {
    assertEquals(
      Newspaper2025P3Migration.priceLookUp(Newspaper2025P3Everyday, Monthly),
      Some(BigDecimal(69.99))
    )

    assertEquals(
      Newspaper2025P3Migration.priceLookUp(Newspaper2025P3Sixday, SemiAnnual),
      Some(BigDecimal(371.94))
    )

    assertEquals(
      Newspaper2025P3Migration.priceLookUp(Newspaper2025P3Weekend, Quarterly),
      Some(BigDecimal(83.97))
    )

    assertEquals(
      Newspaper2025P3Migration.priceLookUp(Newspaper2025P3Saturday, Monthly),
      Some(BigDecimal(15.99))
    )
  }

  // --------------------------------------------------------------------
  // Fixtures:

  // 277291-everyday-annual
  // val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/invoice-preview.json")

  // 277750-everyday-month
  // val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/277750-everyday-month/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/277750-everyday-month/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/277750-everyday-month/invoice-preview.json")

  // 412032-sixday-annual
  // val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/412032-sixday-annual/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/412032-sixday-annual/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/412032-sixday-annual/invoice-preview.json")

  // A-S02075439-saturday-month
  // val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/A-S02075439-saturday-month/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/A-S02075439-saturday-month/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/A-S02075439-saturday-month/invoice-preview.json")

  // --------------------------------------------------------------------
  // Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate

  test("Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate") {
    // 277291-everyday-annual
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/invoice-preview.json")

    assertEquals(
      Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate(subscription),
      Some(LocalDate.of(2024, 11, 27))
    )
  }

  test("Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate") {
    // 412032-sixday-annual
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/412032-sixday-annual/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/412032-sixday-annual/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/412032-sixday-annual/invoice-preview.json")

    assertEquals(
      Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate(subscription),
      Some(LocalDate.of(2024, 8, 12))
    )
  }

  // --------------------------------------------------------------------
  // Newspaper2025P3Migration.decideDeliveryPattern

  test("Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate") {
    // 277291-everyday-annual
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSub.determineRatePlan(subscription: ZuoraSubscription).get

    assertEquals(
      Newspaper2025P3Migration.decideDeliveryPattern(ratePlan),
      Some(Newspaper2025P3Everyday)
    )
  }

  test("Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate") {
    // 277750-everyday-month
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/277750-everyday-month/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/277750-everyday-month/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/277750-everyday-month/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSub.determineRatePlan(subscription: ZuoraSubscription).get

    assertEquals(
      Newspaper2025P3Migration.decideDeliveryPattern(ratePlan),
      Some(Newspaper2025P3Everyday)
    )
  }

  test("Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate") {
    // 412032-sixday-annual
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/412032-sixday-annual/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/412032-sixday-annual/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/412032-sixday-annual/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSub.determineRatePlan(subscription: ZuoraSubscription).get

    assertEquals(
      Newspaper2025P3Migration.decideDeliveryPattern(ratePlan),
      Some(Newspaper2025P3Sixday)
    )
  }

  test("Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate") {
    // A-S02075439-saturday-month
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/A-S02075439-saturday-month/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/A-S02075439-saturday-month/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/A-S02075439-saturday-month/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSub.determineRatePlan(subscription: ZuoraSubscription).get

    assertEquals(
      Newspaper2025P3Migration.decideDeliveryPattern(ratePlan),
      Some(Newspaper2025P3Saturday)
    )
  }

}
