package pricemigrationengine.migrations

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._

import java.time.LocalDate

class HomeDelivery2025MigrationTest extends munit.FunSuite {

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
    val label = HomeDelivery2025Migration.getLabelFromMigrationExtraAttributes(cohortItem)
    assertEquals(label, Some("Label 01"))
  }

}
