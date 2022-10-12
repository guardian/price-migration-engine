package pricemigrationengine

import pricemigrationengine.model.NewPriceOverride.NewPriceOverrider

object TestUtils {
  def newPriceIdentity: NewPriceOverrider = (oldPrice: BigDecimal, newPrice: BigDecimal) => newPrice
}
