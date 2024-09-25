package pricemigrationengine.model

object PriceCap {

  // Directive:
  // If you edit this file, please keep docs/the-art-of-the-cap.md up to date.

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
  // 1. The SalesforcePriceRiseCreationHandler (priceCapForNotification), and
  // 2. The notification handler (priceCapForNotification), and
  // 3. The amendment handler (priceCapForAmendment)

  // Note: We should not apply capping during the estimation step and capped prices should not be written in the
  // migration dynamo tables !

  // Note: These functions work better for simple price migrations where the subscription has one price
  // that we update. In the case of SupporterPlus subscriptions, with their base price and extra optional
  // contribution (see example of the SupporterPlus2024 price migration), the base price is price risen and
  // capped, but the subscription price is the sum of that new price together with the contribution. In a
  // case like that we just implemented the price capping in the migration code itself, without messing around
  // with this code.

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
