package pricemigrationengine.migrations

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._

import java.time.LocalDate

class Newspaper2025P1MigrationTest extends munit.FunSuite {

  test("decoding") {
    val s = """{ "brandTitle": "Label 01" }"""
    val attribute: Newspaper2025ExtraAttributes = upickle.default.read[Newspaper2025ExtraAttributes](s)
    assertEquals(attribute, Newspaper2025ExtraAttributes("Label 01"))
  }

  test("getLabelFromMigrationExtraAttributes") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01" }"""),
    )
    val label = Newspaper2025P1Migration.getLabelFromMigrationExtraAttributes(cohortItem)
    assertEquals(label, Some("Label 01"))
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
