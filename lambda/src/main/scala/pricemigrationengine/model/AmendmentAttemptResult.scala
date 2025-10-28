package pricemigrationengine.model

import java.time.{Instant, LocalDate}

trait AmendmentAttemptResult

case class SuccessfulAmendmentResult(
    subscriptionNumber: String,
    amendmentEffectiveDate: LocalDate,
    newPrice: BigDecimal,
    newSubscriptionId: ZuoraSubscriptionId,
    whenDone: Instant
) extends AmendmentAttemptResult

case class SubscriptionCancelledInZuoraAmendmentResult(
    subscriptionNumber: String
) extends AmendmentAttemptResult

case class AmendmentPreventedDueToLockResult(
    subscriptionNumber: String
) extends AmendmentAttemptResult

case class AmendmentPostponed(
    subscriptionNumber: String
) extends AmendmentAttemptResult
