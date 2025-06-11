package pricemigrationengine.libs

class PriceCapTest extends munit.FunSuite {

  test("cappedPrice is computed correctly") {
    val oldPrice = BigDecimal(100)
    val uncappedNewPrice = BigDecimal(156)
    val priceCappingMultiplier = BigDecimal(1.2)
    assertEquals(
      PriceCap.cappedPrice(
        oldPrice,
        uncappedNewPrice,
        priceCappingMultiplier
      ),
      BigDecimal(120)
    )
  }

  test("priceCapNotification (no need to apply)") {
    val oldPrice = BigDecimal(100)
    val newPrice = BigDecimal(110)
    val cap = 1.25
    assertEquals(
      PriceCap.cappedPrice(oldPrice, newPrice, cap),
      BigDecimal(110)
    )
  }

  test("priceCapNotification (need to apply)") {
    val oldPrice = BigDecimal(100)
    val newPrice = BigDecimal(250)
    val cap = 1.25
    assertEquals(
      PriceCap.cappedPrice(oldPrice, newPrice, cap),
      BigDecimal(125)
    )
  }
}
