package pricemigrationengine.model

object PriceCap {

  /*
    This object implements the policy of not increasing subscription prices, and
    therefore what our customers pay, by more then 20% during a single price rise.

    With that said, there are situations where we want the capping not to apply.
    For instance for very small prices or for certain products, or certain rate plans.
    In which case we can specify that the estimated price will be the new price, even
    if it was higher than 20% increase. The decision is not left to the PriceCap but
    belongs to the caller.
   */

  private val priceCappingMultiplier = 1.2 // old price + 20%
  def cappedPrice(oldPrice: BigDecimal, estimatedNewPrice: BigDecimal, forceEstimated: Boolean = false): BigDecimal = {
    if (forceEstimated) {
      estimatedNewPrice
    } else {
      List(estimatedNewPrice, oldPrice * priceCappingMultiplier).min
    }
  }

  def priceCorrectionFactorForPriceCap(
      oldPrice: BigDecimal,
      estimatedNewPrice: BigDecimal,
      forceEstimated: Boolean = false
  ): BigDecimal = {
    if (
      forceEstimated || estimatedNewPrice == 0 || estimatedNewPrice.compareTo(oldPrice * priceCappingMultiplier) <= 0
    ) {
      1
    } else {
      (oldPrice * priceCappingMultiplier) / estimatedNewPrice
    }
  }
}
