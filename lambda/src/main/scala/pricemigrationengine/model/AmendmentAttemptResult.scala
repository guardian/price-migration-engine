package pricemigrationengine.model

import java.time.{Instant, LocalDate}

trait AmendmentAttemptResult

// The "AAR" prefix means "Amendment Attempt Result"

case class AARSuccessfulAmendment(
    subscriptionNumber: String,
    amendmentEffectiveDate: LocalDate,
    newPrice: BigDecimal,
    newSubscriptionId: ZuoraSubscriptionId,
    whenDone: Instant
) extends AmendmentAttemptResult

case class AARSubscriptionCancelledInZuora(
    subscriptionNumber: String
) extends AmendmentAttemptResult

case class AAROperationPreventedDueToLockResult(
    subscriptionNumber: String
) extends AmendmentAttemptResult

case class AAROperationPostponed(
    subscriptionNumber: String
) extends AmendmentAttemptResult
