package pricemigrationengine.model

object BillingPeriod extends Enumeration {
  type BillingPeriod = Value
  val Month, Quarterly, Annual = Value

  def toString(period: BillingPeriod): String = {
    period match {
      case Month     => "Month"
      case Quarterly => "Quarterly"
      case Annual    => "Annual"
    }
  }

  def fromString(period: String): BillingPeriod = {
    if (period == "Month") {
      BillingPeriod.Month
    } else if (period == "Quarterly") {
      BillingPeriod.Quarterly
    } else if (period == "Annual") {
      BillingPeriod.Annual
    } else {
      throw new Exception(s"could no recover BillingPeriod for period: ${period}")
    }
  }
}
