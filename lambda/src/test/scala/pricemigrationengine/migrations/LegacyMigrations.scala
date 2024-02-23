package pricemigrationengine.migrations

class LegacyMigrationsTest extends munit.FunSuite {

  test("The price capping works correctly") {
    val oldPrice = BigDecimal(100)
    val cappedPrice = BigDecimal(120)
    val uncappedPrice = BigDecimal(156)
    assertEquals(cappedPrice, LegacyMigrations.priceCap(oldPrice, uncappedPrice))
  }

  test("The price capping works correctly in case of force estimated") {
    val oldPrice = BigDecimal(100)
    val uncappedPrice = BigDecimal(156)
    assertEquals(uncappedPrice, LegacyMigrations.priceCap(oldPrice, uncappedPrice, true))
  }
}
