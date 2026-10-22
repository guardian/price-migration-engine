package pricemigrationengine.model

object GuardianWeeklyHelper {
  // GuardianWeeklyHelper was introduced in September 2026 alongside the
  // GuardianWeeklyFinanceAllocations financial data.

  def billingPeriodToT4xGuardianWeeklyPaymentSchedule(
      billingPeriod: BillingPeriod
  ): Option[T4xGuardianWeeklyPaymentSchedule] = {
    Option(billingPeriod).map {
      case Monthly    => T4xMonth
      case Quarterly  => T4xQuarter
      case SemiAnnual => T4xSemiAnnual
      case Annual     => T4xAnnual
    }
  }

  def subscriptionToFinanceAllocation(
      billingPeriod: BillingPeriod,
      currency: Currency,
      pricingLocalisation: PricingLocalisation
  ): Option[T5xFinanceAllocation] = {
    for {
      paymentSchedule <- billingPeriodToT4xGuardianWeeklyPaymentSchedule(billingPeriod)
      distribution <- GuardianWeeklyFinanceAllocations.getAllocation(
        paymentSchedule: T4xGuardianWeeklyPaymentSchedule,
        currency,
        pricingLocalisation
      )
    } yield distribution
  }
}
