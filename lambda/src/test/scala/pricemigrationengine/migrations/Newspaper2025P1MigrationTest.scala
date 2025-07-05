package pricemigrationengine.migrations

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._

import java.time.LocalDate

class Newspaper2025P1MigrationTest extends munit.FunSuite {

  // val s = """{ "brandTitle": "the Guardian" }"""
  // val s = """{ "brandTitle": "the Guardian and the Observer" }"""
  // val s = """{ "brandTitle": "the Guardian", "removeDiscount": true }"""

  test("decoding") {
    val s = """{ "brandTitle": "Label 01" }"""
    val attribute: Newspaper2025ExtraAttributes = upickle.default.read[Newspaper2025ExtraAttributes](s)
    assertEquals(attribute, Newspaper2025ExtraAttributes("Label 01", None))
  }

  test("getLabelFromMigrationExtraAttributes (1)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01" }"""),
    )
    val label = Newspaper2025P1Migration.getLabelFromMigrationExtraAttributes(cohortItem)
    assertEquals(label, Some("Label 01"))
  }

  test("getLabelFromMigrationExtraAttributes (2)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01", "removeDiscount": true }"""),
    )
    val label = Newspaper2025P1Migration.getLabelFromMigrationExtraAttributes(cohortItem)
    assertEquals(label, Some("Label 01"))
  }

  test("decideShouldRemoveDiscount (2)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01" }"""),
    )
    val label = Newspaper2025P1Migration.decideShouldRemoveDiscount(cohortItem)
    assertEquals(label, false)
  }

  test("decideShouldRemoveDiscount (2)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01", "removeDiscount": true }"""),
    )
    val label = Newspaper2025P1Migration.decideShouldRemoveDiscount(cohortItem)
    assertEquals(label, true)
  }

  test("priceLookUp") {
    assertEquals(
      Newspaper2025P1Migration.priceLookUp(Newspaper2025P1Voucher, Newspaper2025P1EverydayPlus, Monthly),
      Some(BigDecimal(69.99))
    )

    assertEquals(
      Newspaper2025P1Migration.priceLookUp(Newspaper2025P1Voucher, Newspaper2025P1SixdayPlus, SemiAnnual),
      Some(BigDecimal(371.94))
    )

    assertEquals(
      Newspaper2025P1Migration.priceLookUp(Newspaper2025P1Subcard, Newspaper2025P1EverydayPlus, Quarterly),
      Some(BigDecimal(209.97))
    )

    assertEquals(
      Newspaper2025P1Migration.priceLookUp(Newspaper2025P1HomeDelivery, Newspaper2025P1SixdayPlus, Monthly),
      Some(BigDecimal(73.99))
    )

    // And we test an undefined combination
    assertEquals(
      Newspaper2025P1Migration.priceLookUp(Newspaper2025P1HomeDelivery, Newspaper2025P1SixdayPlus, SemiAnnual),
      None
    )
  }
}
