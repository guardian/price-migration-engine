package pricemigrationengine.model

class PriceCapTest extends munit.FunSuite {

  test("The price capping works correctly") {
    val oldPrice = BigDecimal(100)
    val cappedPrice = BigDecimal(120)
    val uncappedPrice = BigDecimal(156)
    assertEquals(cappedPrice, PriceCap(oldPrice, uncappedPrice))
  }

  test("The price capping works correctly in case of force estimated") {
    val oldPrice = BigDecimal(100)
    val uncappedPrice = BigDecimal(156)
    assertEquals(uncappedPrice, PriceCap(oldPrice, uncappedPrice, true))
  }
}
