package pricemigrationengine.model

import java.time.{Instant, LocalDate}

sealed trait AmendmentAttemptResult

// The "AAR" prefix means "Amendment Attempt Result"

case class AARSuccessfulAmendment(
    subscriptionNumber: String,
    amendmentEffectiveDate: LocalDate,
    newPrice: BigDecimal,
    newSubscriptionId: ZuoraSubscriptionId,
    whenDone: Instant
) extends AmendmentAttemptResult
