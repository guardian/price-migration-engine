package pricemigrationengine.model

object GuardianWeeklyHelper {
  // GuardianWeeklyHelper was introduced in September 2026 alongside the
  // GuardianWeeklyLegPercentageDistribution financial data.

  def billingPeriodToT4xGuardianWeeklyPaymentSchedule(
      billingPeriod: BillingPeriod
  ): Option[T4xGuardianWeeklyPaymentSchedule] = {
    billingPeriod match {
      case Monthly    => Some(T4xMonth)
      case Quarterly  => Some(T4xQuarter)
      case SemiAnnual => Some(T4xSemiAnnual)
      case Annual     => Some(T4xAnnual)
      case _          => None
    }
  }

  def subscriptionToProductRatePlanChargeIdMapping(subscription: ZuoraSubscription): Map[T7xGWSubLegs, String] = {
    Map(
      T7xGuardianWeekly -> "TO BE DECIDED",
      T7xDigitalPack -> "TO BE DECIDED",
    )
  }

  def subscriptionToFinancePercentageDistribution(
      billingPeriod: BillingPeriod,
      currency: Currency,
      pricingLocalisation: PricingLocalisation
  ): Option[T5xDistribution] = {
    for {
      paymentSchedule <- billingPeriodToT4xGuardianWeeklyPaymentSchedule(billingPeriod)
      distribution <- GuardianWeeklyLegPercentageDistribution.getDistribution(
        paymentSchedule: T4xGuardianWeeklyPaymentSchedule,
        currency,
        pricingLocalisation
      )
    } yield distribution
  }
}
