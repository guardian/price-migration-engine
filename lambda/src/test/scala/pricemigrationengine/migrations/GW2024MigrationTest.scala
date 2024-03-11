package pricemigrationengine.migrations

import pricemigrationengine.model._

import java.time.LocalDate
import pricemigrationengine.Fixtures
import pricemigrationengine.migrations.GW2024Migration
import pricemigrationengine.migrations.GW2024Migration

class GW2024MigrationTest extends munit.FunSuite {

  test("Price lookup is correct") {
    assertEquals(
      GW2024Migration.getNewPrice1(Monthly, "GBP"),
      Some(BigDecimal(15))
    )
    assertEquals(
      GW2024Migration.getNewPrice1(Quarterly, "ROW (USD)"),
      Some(BigDecimal(99))
    )
  }

}
