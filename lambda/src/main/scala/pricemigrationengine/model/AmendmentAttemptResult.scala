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

// This case was introduced for the ProductMigration2025N4, where the users
// had the option to opt out from the product migration, determined by a flag
// set in Salesforce
case class AARUserOptOut(
    subscriptionNumber: String
) extends AmendmentAttemptResult
