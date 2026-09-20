package pricemigrationengine.model

import java.time.LocalDate

sealed trait SubscriptionNotificationAnalyseResult

// "SNAR" means "Subscription Notification Analyse Result"

object SNARReadyToNotify extends SubscriptionNotificationAnalyseResult
object SNARCancelledInZuora extends SubscriptionNotificationAnalyseResult
object SNARExcludeFromMigration extends SubscriptionNotificationAnalyseResult
object SNARMissingNotificationWindow extends SubscriptionNotificationAnalyseResult

object SubscriptionNotificationAnalyseResult {

  def toString(result: SubscriptionNotificationAnalyseResult): String = {
    result match {
      case SNARReadyToNotify             => "SNARReadyToNotify"
      case SNARCancelledInZuora          => "SNARCancelledInZuora"
      case SNARExcludeFromMigration      => "SNARExcludeFromMigration"
      case SNARMissingNotificationWindow => "SNARMissingNotificationWindow"
    }
  }

  def analyseSubscriptionForNotification_Legacy(
      ratePlanProbeResult: RatePlanProbeResult
  ): Option[SubscriptionNotificationAnalyseResult] = {
    ratePlanProbeResult match {
      case RPPShouldProceed        => Some(SNARReadyToNotify)
      case RPPCancelledInZuora     => Some(SNARCancelledInZuora)
      case IndeterminateConclusion => None
    }
  }

  def analyseSubscriptionForNotification_SupporterPlus2026(
      subscription: ZuoraSubscription,
      cohortItem: CohortItem,
      date: LocalDate
  ): Option[SubscriptionNotificationAnalyseResult] = {
    // The check here consists in verifying that the product name is "Supporter Plus" [1] and that
    // The billing period of the subscription's active rate plan is the same as the cohort item [2]

    // [1] The first discrepancy happens when the customer has upgraded to DigitalPack
    // [2] The second discrepancy happens when the customer migrated from Monthly to Annual

    for {
      ratePlan <- SI2025RateplanFromSub.uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(
        subscription,
        date
      )
      subscriptionBillingPeriod <- ZuoraRatePlan.ratePlanToOptionalUniquelyDeterminedBillingPeriod(ratePlan)
      cohortItemBillingPeriod <- cohortItem.billingPeriod
    } yield {
      if (
        ratePlan.productName == "Supporter Plus" &&
        BillingPeriod.toString(subscriptionBillingPeriod) == cohortItemBillingPeriod
      ) {
        SNARReadyToNotify
      } else {
        SNARExcludeFromMigration
      }
    }
  }

  def analyseSubscriptionForNotification(
      cohortSpec: CohortSpec,
      subscription: ZuoraSubscription,
      cohortItem: CohortItem,
      date: LocalDate,
      ratePlanProbeResult: RatePlanProbeResult
  ): Option[SubscriptionNotificationAnalyseResult] = {
    if (subscription.status == "Cancelled") {
      Some(SNARCancelledInZuora)
    } else if (!NotificationHandlerHelper.thereIsEnoughNotificationLeadTime(cohortSpec, date, cohortItem)) {
      Some(SNARMissingNotificationWindow)
    } else {
      MigrationType(cohortSpec) match {
        case Test1             => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Newspaper2025P1   => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Newspaper2025P3   => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Membership2025    => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case DigiSubs2025      => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case SupporterPlus2026 => analyseSubscriptionForNotification_SupporterPlus2026(subscription, cohortItem, date)
        case Print2026C1GWAnnualsUK     => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C1GWQuarterliesUK => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C1NPAnnualsUK     => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C1NPQuarterliesUK => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C1NPSemiannualsUK => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C2NPMonthliesUK   => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C3GWMonthliesUK   => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C3NPMonthliesUK   => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C4NPMonthliesUK   => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C5GW              =>
          analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C5NP =>
          analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C6GWQuarterliesNonUK => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
      }
    }
  }
}
