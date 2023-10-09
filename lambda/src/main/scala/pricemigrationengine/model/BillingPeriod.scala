package pricemigrationengine.model

sealed trait BillingPeriod
object Monthly extends BillingPeriod
object Quarterly extends BillingPeriod
object Annual extends BillingPeriod

// SemiAnnual will be added when the needs for it arises

object BillingPeriod {
  def toString(period: BillingPeriod): String = {
    period match {
      case Monthly   => "Month"
      case Quarterly => "Quarterly"
      case Annual    => "Annual"
    }
  }

  def fromString(period: String): BillingPeriod = {
    if (period == "Month") {
      Monthly
    } else if (period == "Quarterly") {
      Quarterly
    } else if (period == "Annual") {
      Annual
    } else {
      throw new Exception(s"could no recover BillingPeriod for period: ${period}")
    }
  }
}
