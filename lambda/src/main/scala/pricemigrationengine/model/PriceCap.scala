package pricemigrationengine.model

object PriceCap {

  /*
    This object implements the policy of not increasing subscription prices, and
    therefore what our customers pay, by more then 20% during a single price rise.
   */

  private val priceCappingMultiplier = 1.2 // old price + 20%
  def cappedPrice(oldPrice: BigDecimal, estimatedNewPrice: BigDecimal): BigDecimal =
    List(estimatedNewPrice, oldPrice * priceCappingMultiplier).min

  def priceCorrectionFactor(oldPrice: BigDecimal, estimatedNewPrice: BigDecimal): BigDecimal = {
    if (estimatedNewPrice == 0 || estimatedNewPrice.compareTo(oldPrice * priceCappingMultiplier) <= 0) {
      1
    } else {
      (oldPrice * priceCappingMultiplier) / estimatedNewPrice
    }
  }
}
