package pricemigrationengine.model

import java.time.{LocalDate}

sealed trait SubscriptionAmendmentAnalyseResult

// "SAAR" means "Subscription Amendment Analyse Result"

object SAARReadyToAmend extends SubscriptionAmendmentAnalyseResult
object SAARCancelledInZuora extends SubscriptionAmendmentAnalyseResult
case class SAARExcludeFromMigration(cause: String) extends SubscriptionAmendmentAnalyseResult
object SAARFailNoisily extends SubscriptionAmendmentAnalyseResult

object SubscriptionAmendmentAnalyseResult {

  def toString(result: SubscriptionAmendmentAnalyseResult): String = {
    result match {
      case SAARReadyToAmend                 => "SAARReadyToAmend"
      case SAARCancelledInZuora             => "SAARCancelledInZuora"
      case SAARExcludeFromMigration(reason) => s"SAARExcludeFromMigration (reason: ${reason})"
      case SAARFailNoisily                  => "SAARFailNoisily"
    }
  }
}
