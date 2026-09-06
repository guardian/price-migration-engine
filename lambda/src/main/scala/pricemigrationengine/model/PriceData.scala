package pricemigrationengine.model

case class PriceData(
    currency: Currency,
    oldPrice: BigDecimal,
    priceGridNewPrice: BigDecimal,
    commsPrice: BigDecimal,
    billingPeriod: String
)
