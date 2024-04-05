package pricemigrationengine.model

object PriceCap {

  // --------------------------------------------------------
  // Part 1

  private val priceCappingMultiplier = 1.2 // old price + 20%
  def priceCapLegacy(
      oldPrice: BigDecimal,
      estimatedNewPrice: BigDecimal
  ): BigDecimal = {
    List(estimatedNewPrice, oldPrice * priceCappingMultiplier).min
  }

  // --------------------------------------------------------
  // Part 2

  // We have separate functions for determining the
  // estimated new price and for computing the adjusted ZuoraSubscriptionUpdate. Unlike the now obsolete part 1
  // We design those functions with the signature that is actually useful for price cap, where we will need to apply it
  // 1. The notification handler, and
  // 2. The SalesforcePriceRiseCreationHandler, and
  // 3. The amendment handler

  // Note: We should not apply capping during the estimation step and capped prices should not be written in the
  // migration dynamo tables !

  def priceCapForNotification(oldPrice: BigDecimal, newPrice: BigDecimal, cap: BigDecimal): BigDecimal = {
    // The cap is the price cap expressed as a multiple of the old price. For instance for a price cap
    // of 20%, we will use a cap equal to 1.2
    (oldPrice * cap).min(newPrice)
  }

  def priceCorrectionFactor(oldPrice: BigDecimal, newPrice: BigDecimal, cap: BigDecimal) = {
    if (newPrice < oldPrice * cap) 1.0
    else
      (oldPrice * cap).toDouble / newPrice.toDouble
  }

  def updateChargeOverride(chargeOverride: ChargeOverride, correctionFactor: Double): ChargeOverride = {
    chargeOverride.copy(price = chargeOverride.price * correctionFactor)
  }

  def updateAddZuoraRatePlan(addZuoraRatePlan: AddZuoraRatePlan, correctionFactor: Double): AddZuoraRatePlan = {
    addZuoraRatePlan.copy(chargeOverrides =
      addZuoraRatePlan.chargeOverrides.map(chargeOverride =>
        updateChargeOverride(chargeOverride: ChargeOverride, correctionFactor: Double)
      )
    )
  }

  def priceCapForAmendment(
      oldPrice: BigDecimal,
      newPrice: BigDecimal,
      cap: BigDecimal,
      zuoraUpdate: ZuoraSubscriptionUpdate
  ): ZuoraSubscriptionUpdate = {
    val shouldApplyCapping: Boolean = (oldPrice * cap) < newPrice
    if (shouldApplyCapping) {
      val correctionFactor = priceCorrectionFactor(oldPrice, newPrice, cap)
      zuoraUpdate.copy(add =
        zuoraUpdate.add.map(addZuoraRatePlan =>
          updateAddZuoraRatePlan(addZuoraRatePlan: AddZuoraRatePlan, correctionFactor: Double)
        )
      )
    } else {
      zuoraUpdate
    }
  }
}
