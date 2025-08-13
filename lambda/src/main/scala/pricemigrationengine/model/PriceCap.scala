package pricemigrationengine.model

object PriceCap {

  // Directive:
  // If you edit this file, please keep docs/the-art-of-the-cap.md up to date.

  def cappedPrice(
      oldPrice: BigDecimal,
      uncappedNewPrice: BigDecimal,
      priceCappingMultiplier: BigDecimal
  ): BigDecimal = {
    // For a price cap of 20%, the priceCappingMultiplier is set to 1.2
    List(uncappedNewPrice, oldPrice * priceCappingMultiplier).min
  }
}
