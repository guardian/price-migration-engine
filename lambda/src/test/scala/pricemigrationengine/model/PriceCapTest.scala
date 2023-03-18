package pricemigrationengine.model

class PriceCapTest extends munit.FunSuite {

  test("The price capping works correctly") {
    val oldPrice = BigDecimal(100)
    val cappedPrice = BigDecimal(120)
    val uncappedPrice = BigDecimal(156)
    assertEquals(cappedPrice, PriceCap.cappedPrice(oldPrice, uncappedPrice))
  }

  test("The price correction factor is computed correctly") {
    val oldPrice = BigDecimal(100)
    val estimatedNewPrice = BigDecimal(240)
    val correctionFactor = BigDecimal(0.5)
    // The capped price is 120, half of the estimated new price, hence a correction factor of 0.5
    assertEquals(correctionFactor, PriceCap.priceCorrectionFactor(oldPrice, estimatedNewPrice))
  }
}
