package pricemigrationengine.model

case class PriceData(
    currency: Currency,
    oldPrice: BigDecimal,
    newPriceFull: BigDecimal,
    commsPrice: BigDecimal,
    billingPeriod: String
)
