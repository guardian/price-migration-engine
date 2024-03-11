package pricemigrationengine.model

object PriceCap {

  /*

    ### Some bits of history

    Price capping was introduced in late 2022. At the time as a general policy not to price rise by more than
    20% up from the old price. It has been applied to the so called Legacy migrations (notably the Guardian Weekly 2022
    migration), but has not been applied to any of the 2023 Digital Migrations.

    The Newspaper2024 migration implemented its own very specfic price cap functionality and there was a
    feeling that onward each migration would implement its own price cap mechanism

    ### PriceCap

    The PriceCap object is split in two parts

    Part 1 has the variable and function that we first used to implement price capping. They are actually
    difficult to use in practice because they are not really suitable for price rises where rate plans
    have several rate plan charges. They should be considered obsolete and deprecated, they will be removed
    from the code as soon as we can clean up Legacy migration supporting code.

    Part 2 has the new approach, introduced in March 2024. We have separate functions for determining the
    estimated new price and for computing the adjusted ZuoraSubscriptionUpdate.

    Note that each migration needs to decide whether or not they are applying a price cap. We are just providing
    functions that can be used if we want to apply a price rise. This means that price capping is not part of the
    general/basic logic of the price migration because doing it right is very, very, difficult. Instead price capping
    and notably these functions should be thought of the basic elements if a migration needs to implement
    price capping.

    Note to avoid the painful redesign that happened here: https://github.com/guardian/price-migration-engine/pull/781
    the price that is written in the DynamoDB should never be capped. Instead only the price that is
    communicated to the user should be capped.

   */

  // --------------------------------------------------------
  // Part 1

  private val priceCappingMultiplier = 1.2 // old price + 20%
  def priceCapLegacy(
      oldPrice: BigDecimal,
      estimatedNewPrice: BigDecimal,
      forceEstimated: Boolean = false
  ): BigDecimal = {
    if (forceEstimated) {
      estimatedNewPrice
    } else {
      List(estimatedNewPrice, oldPrice * priceCappingMultiplier).min
    }
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
