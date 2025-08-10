package pricemigrationengine.model

import java.time.{Instant, LocalDate}

trait AmendmentResult

case class SuccessfulAmendmentResult(
    subscriptionNumber: String,
    startDate: LocalDate,
    oldPrice: BigDecimal,
    newPrice: BigDecimal,
    estimatedNewPrice: BigDecimal,
    newSubscriptionId: ZuoraSubscriptionId,
    whenDone: Instant
) extends AmendmentResult

case class SubscriptionCancelledInZuoraAmendmentResult(
    subscriptionNumber: String
) extends AmendmentResult

case class AmendmentPreventedDueToLockResult(
    subscriptionNumber: String
) extends AmendmentResult
