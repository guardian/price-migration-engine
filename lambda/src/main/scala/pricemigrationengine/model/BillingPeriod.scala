package pricemigrationengine.model

sealed trait BillingPeriod
object Monthly extends BillingPeriod
object Quarterly extends BillingPeriod
object Annual extends BillingPeriod

// SemiAnnual will be added when the needs for it arises

object BillingPeriod {

  val notificationPaymentFrequencyMapping = Map(
    // This map is used to convert a CohortItem's billingPeriod in to the user friendly representation in letters
    // and emails.
    "Month" -> "Monthly",
    "Quarter" -> "Quarterly",
    "Quarterly" -> "Quarterly",
    "Semi_Annual" -> "Semiannually",
    "Annual" -> "Annually"
  )

  def toString(period: BillingPeriod): String = {
    // For Zuora, the billingPeriod should be one of: Month, Quarter, Semi_Annual, Annual, Eighteen_Months,
    // Two_Years, Three_Years, Five_Years, Specific_Months, Subscription_Term, Week, Specific_Weeks, Specific_Days

    // We are only using Month, Quarter and Annual
    period match {
      case Monthly   => "Month"
      case Quarterly => "Quarter"
      case Annual    => "Annual"
    }
  }

  def fromString(period: String): BillingPeriod = {
    if (period == "Month") {
      Monthly
    } else if (period == "Quarterly" || period == "Quarter") {
      // This function is used when reading a BillingPeriod from a CohortItem, and we have both
      // strings, Quarterly and Quarter in the cohort tables.
      Quarterly
    } else if (period == "Annual") {
      Annual
    } else {
      throw new Exception(s"could no recover BillingPeriod for period: ${period}")
    }
  }
}
