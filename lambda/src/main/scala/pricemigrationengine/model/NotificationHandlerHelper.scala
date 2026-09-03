package pricemigrationengine.model

import pricemigrationengine.handlers.NotificationHandler.minLeadTime

import java.time.LocalDate
import pricemigrationengine.model.membershipworkflow.EmailMessage

object NotificationHandlerHelper {

  def isNonTrivialValue(value: Option[String]): Boolean = {
    value.isDefined && value.get.nonEmpty
  }

  def messageIsWellFormed(cohortSpec: CohortSpec, message: EmailMessage): Boolean = {
    // This function return whether or not an EmailMessage is "well formed". And for the moment
    // this is limited to checking that the special circumstances extra attributes (which were
    // originally introduced for the Summer 2025 print migrations) are not empty.

    MigrationType(cohortSpec) match {
      case Test1              => true
      case GuardianWeekly2025 => true
      case Newspaper2025P1    => {
        List(
          isNonTrivialValue(message.To.ContactAttributes.SubscriberAttributes.newspaper2025_brand_title)
        ).forall(identity)
      }
      case Newspaper2025P3 => {
        List(
          isNonTrivialValue(message.To.ContactAttributes.SubscriberAttributes.newspaper2025_phase3_brand_title)
        ).forall(identity)
      }
      case ProductMigration2025N4 => {
        List(
          isNonTrivialValue(message.To.ContactAttributes.SubscriberAttributes.newspaper2025_phase4_brand_title),
          isNonTrivialValue(message.To.ContactAttributes.SubscriberAttributes.newspaper2025_phase4_formstack_url),
        ).forall(identity)
      }
      case Membership2025      => true
      case DigiSubs2025        => true
      case SupporterPlus2026   => true
      case SupporterPlus2026N2 => true
      case SupporterPlus2026N3 => true
      case SupporterPlus2026N4 => true
      case SupporterPlus2026N5 => true
    }
  }

  def checkProductName(
      ratePlan: ZuoraRatePlan,
      today: LocalDate,
      productNameOpt: Option[String]
  ): Boolean = {
    // This function essentially returns `true` if the rate plan product name is
    // what we expect. This was introduced to ensure that at Notification time
    // the subscription has not moved to a different product. This can happen to,
    // for instance, to Supporter Plus subs that can be transmuted to Digital Packs

    productNameOpt match {
      case Some(productName) => {
        ratePlan.productName == productName
      }
      case None => true // for backward compatibility when the information is not available for previous subs
    }
  }

  def thereIsEnoughNotificationLeadTime(cohortSpec: CohortSpec, today: LocalDate, cohortItem: CohortItem): Boolean = {
    // To help with backward compatibility with existing tests, we apply this condition from 1st Dec 2020.
    if (today.isBefore(LocalDate.of(2020, 12, 1))) {
      true
    } else {
      cohortItem.amendmentEffectiveDate match {
        case Some(sd) => today.plusDays(minLeadTime(cohortSpec)).isBefore(sd)
        case _        => false
      }
    }
  }

  def zuoraSubscriptionToActiveRatePlanId(subscription: ZuoraSubscription, today: LocalDate): Option[String] = {
    for {
      ratePlan <- SI2025RateplanFromSub.uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(
        subscription: ZuoraSubscription,
        today: LocalDate
      )
    } yield ratePlan.id
  }
}

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
    } else if (
      !cohortSpec.forceNotifications
        .contains(true) && !NotificationHandlerHelper.thereIsEnoughNotificationLeadTime(cohortSpec, date, cohortItem)
    ) {
      Some(SNARMissingNotificationWindow)
    } else {
      MigrationType(cohortSpec) match {
        case Test1                  => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case GuardianWeekly2025     => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Newspaper2025P1        => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Newspaper2025P3        => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case ProductMigration2025N4 => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Membership2025         => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case DigiSubs2025           => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case SupporterPlus2026      =>
          analyseSubscriptionForNotification_SupporterPlus2026(subscription, cohortItem, date)
        case SupporterPlus2026N2 =>
          analyseSubscriptionForNotification_SupporterPlus2026(subscription, cohortItem, date)
        case SupporterPlus2026N3 =>
          analyseSubscriptionForNotification_SupporterPlus2026(subscription, cohortItem, date)
        case SupporterPlus2026N4 =>
          analyseSubscriptionForNotification_SupporterPlus2026(subscription, cohortItem, date)
        case SupporterPlus2026N5 =>
          analyseSubscriptionForNotification_SupporterPlus2026(subscription, cohortItem, date)
      }
    }
  }
}
