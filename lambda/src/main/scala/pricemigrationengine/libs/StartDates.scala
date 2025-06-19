package pricemigrationengine.libs

import pricemigrationengine.handlers.NotificationHandler
import pricemigrationengine.migrations.{GuardianWeekly2025Migration, Newspaper2025P1Migration}
import pricemigrationengine.model._
import scala.util.Random

import java.time.LocalDate

/*
  StartDates aggregate the utility functions required to compute the migration dates
  also known as start data in a cohort item. They are the date the price migration
  amendment are taking effect.
 */

object StartDates {

  // Determines whether the subscription is a monthly subscription
  def isMonthlySubscription(subscription: ZuoraSubscription, invoicePreview: ZuoraInvoiceList): Boolean = {
    invoicePreview.invoiceItems
      .flatMap(invoiceItem => ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toOption)
      .flatMap(_.billingPeriod)
      .headOption
      .contains("Month")
  }

  // This function returns the optional date of the last price rise.
  def lastPriceRiseDate(cohortSpec: CohortSpec, subscription: ZuoraSubscription): Option[LocalDate] = {
    MigrationType(cohortSpec) match {
      case SupporterPlus2024  => None
      case GuardianWeekly2025 => GuardianWeekly2025Migration.subscriptionToLastPriceMigrationDate(subscription) // [1]
      case Newspaper2025P1    => Newspaper2025P1Migration.subscriptionToLastPriceMigrationDate(subscription) // [2]
    }
    // [1 & 2] We are applying the "one year since the last price migration" policy for
    // GuardianWeekly2025 and Newspaper2025
  }

  def cohortSpecLowerBound(
      cohortSpec: CohortSpec,
      today: LocalDate
  ): LocalDate = {
    // This is a function of the cohort spec and the notification min time.
    // The cohort spec carries the lowest date we specify there can be a price migration, and the notification min
    // time ensures the legally required lead time for customer communication. The max of those two dates is the date
    // from which we can realistically perform a price increase. With that said, other policies can apply, for
    // instance:
    // - The one year policy, which demand that we do not price rise customers during the subscription first year
    // - The spread: a mechanism, used for monthlies, by which we do not let a large number of monthlies migrate
    //   during a single month.

    Date.datesMax(
      cohortSpec.earliestPriceMigrationStartDate,
      today.plusDays(
        NotificationHandler.minLeadTime(cohortSpec: CohortSpec) + 1
      ) // +1 because we need to be strictly over minLeadTime days away. Exactly minLeadTime is not enough.
    )
  }

  // This function implements the policy of not price rising a subscription within the first year of its existence.
  def noPriceRiseDuringSubscriptionFirstYearPolicyUpdate(
      lowerbound: LocalDate,
      subscription: ZuoraSubscription
  ): LocalDate = {
    Date.datesMax(lowerbound, subscription.customerAcceptanceDate.plusMonths(12))
  }

  // This function implements the policy of not price rising a subscription less than a year after the optional
  // last price rise.
  def noPriceRiseWithinAYearOfLastPriceRisePolicyUpdate(
      cohortSpec: CohortSpec,
      subscription: ZuoraSubscription,
      lowerBound1: LocalDate
  ): LocalDate = {
    Date.datesMax(
      lowerBound1,
      lastPriceRiseDate(cohortSpec, subscription).map(date => date.plusMonths(12)).getOrElse(lowerBound1)
    )
  }

  // In legacy print product cases, we have spread the price rises over 3 months for monthly subscriptions, this is
  // the default behaviour. For annual subscriptions we are not applying any spread and defaulting to value 1.
  def decideSpreadPeriod(
      subscription: ZuoraSubscription,
      invoicePreview: ZuoraInvoiceList,
      cohortSpec: CohortSpec
  ): Int = {
    if (isMonthlySubscription(subscription, invoicePreview)) {
      MigrationType(cohortSpec) match {
        case SupporterPlus2024  => 1 // no spread for S+2024 monthlies
        case GuardianWeekly2025 => 1 // no spread for Guardian Weekly 2025
        case Newspaper2025P1    => 1 // no spread for Newspaper 2025
      }
    } else 1
  }

  def startDateLowerBound(
      item: CohortItem,
      subscription: ZuoraSubscription,
      invoicePreview: ZuoraInvoiceList,
      cohortSpec: CohortSpec,
      today: LocalDate
  ): LocalDate = {

    // LowerBound from to the cohort spec and the notification window's end
    val startDateLowerBound1 = MigrationType(cohortSpec) match {
      case SupporterPlus2024  => cohortSpecLowerBound(cohortSpec, today)
      case GuardianWeekly2025 => cohortSpecLowerBound(cohortSpec, today)
      case Newspaper2025P1    => cohortSpecLowerBound(cohortSpec, today)
    }

    // We now respect the policy of not increasing members during their first year
    val startDateLowerBound2 = noPriceRiseDuringSubscriptionFirstYearPolicyUpdate(startDateLowerBound1, subscription)

    // And the policy not to price rise a sub twice within 12 months of any possible price rise
    val startDateLowerBound3 =
      noPriceRiseWithinAYearOfLastPriceRisePolicyUpdate(cohortSpec, subscription, startDateLowerBound2)

    // With GuardianWeekly2025, we were given lower bounds in the Marketing spreadsheet
    // that was the first use of the new cohortItem's migrationExtraAttributes. If we expect a
    // migration to provide it own lowerbound computation, we do it here, otherwise we identity
    // on startDateLowerBound3
    val startDateLowerBound4 = MigrationType(cohortSpec) match {
      case SupporterPlus2024  => startDateLowerBound3
      case GuardianWeekly2025 => GuardianWeekly2025Migration.computeStartDateLowerBound4(startDateLowerBound3, item)
      case Newspaper2025P1    => startDateLowerBound3
    }

    // Decide the spread period for this migration
    val spreadPeriod = decideSpreadPeriod(subscription, invoicePreview, cohortSpec)

    val randomDelayInMonths = Random.nextInt(spreadPeriod) // [1]
    // [1]
    // Decides an integer in the interval [0, spreadPeriod-1]
    // The default spread period is 1

    startDateLowerBound4.plusMonths(randomDelayInMonths)
  }
}
