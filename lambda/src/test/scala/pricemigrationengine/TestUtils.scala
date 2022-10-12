package pricemigrationengine

import pricemigrationengine.model.ChargeOverrider.ChargeOverrideFunc

object TestUtils {
  def newPriceIdentity: ChargeOverrideFunc = (oldPrice: BigDecimal, newPrice: BigDecimal) => newPrice
}
