package pricemigrationengine.migrations

import pricemigrationengine.model._

class GuardianWeekly2025MigrationTest extends munit.FunSuite {

  test("priceLookup") {
    val value =
      GuardianWeekly2025Migration.priceLookUp(Domestic, Annual, "CAD")
    assertEquals(
      value,
      Some(BigDecimal(432))
    )
  }

}
