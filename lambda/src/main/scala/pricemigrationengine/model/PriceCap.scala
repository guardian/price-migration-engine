package pricemigrationengine.model

import scala.math.BigDecimal.RoundingMode

object PriceCap {

  // Directive:
  // If you edit this file, please keep docs/the-art-of-the-cap.md up to date.

  def cappedPrice(
      oldPrice: BigDecimal,
      uncappedNewPrice: BigDecimal,
      priceCappingMultiplierOpt: Option[BigDecimal]
  ): BigDecimal = {
    priceCappingMultiplierOpt match {
      case None                         => uncappedNewPrice
      case Some(priceCappingMultiplier) => {
        // For a price cap of 20%, the priceCappingMultiplier is set to 1.2
        val cappedPrice = (oldPrice * priceCappingMultiplier).setScale(2, RoundingMode.DOWN)
        List(uncappedNewPrice, cappedPrice).min
      }
    }
  }
}
