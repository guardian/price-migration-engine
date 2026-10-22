package pricemigrationengine.model

import java.time.{Instant, LocalDate}

// The "AAR" prefix means "Amendment Attempt Result"
case class AARSuccessfulAmendment(
    subscriptionNumber: String,
    amendmentEffectiveDate: LocalDate,
    newPrice: BigDecimal,
    newSubscriptionId: ZuoraSubscriptionId,
    whenDone: Instant
)
