package pricemigrationengine.migrations

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._

import java.time.LocalDate

class Newspaper2025P1MigrationTest extends munit.FunSuite {

  test("decoding") {
    val s = """{ "brandTitle": "Label 01" }"""
    val attribute: Newspaper2025ExtendedAttributes = upickle.default.read[Newspaper2025ExtendedAttributes](s)
    assertEquals(attribute, Newspaper2025ExtendedAttributes("Label 01"))
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
      Newspaper2025P1Migration.priceLookUp(Voucher, EverydayPlus, Monthly),
      Some(BigDecimal(69.99))
    )

    assertEquals(
      Newspaper2025P1Migration.priceLookUp(Voucher, SixdayPlus, SemiAnnual),
      Some(BigDecimal(371.94))
    )

    assertEquals(
      Newspaper2025P1Migration.priceLookUp(Subcard, EverydayPlus, Quarterly),
      Some(BigDecimal(209.97))
    )

    assertEquals(
      Newspaper2025P1Migration.priceLookUp(HomeDelivery, SixdayPlus, Monthly),
      Some(BigDecimal(73.99))
    )

    // And we test an undefined combination
    assertEquals(
      Newspaper2025P1Migration.priceLookUp(HomeDelivery, SixdayPlus, SemiAnnual),
      None
    )
  }
}
