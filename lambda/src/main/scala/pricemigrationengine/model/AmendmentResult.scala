package pricemigrationengine.model

import java.time.LocalDate

case class AmendmentResult(
    subscriptionName: String,
    startDate: LocalDate,
    newPrice: BigDecimal,
    newSubscriptionId: ZuoraSubscriptionId
)
