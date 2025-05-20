package pricemigrationengine.util

import pricemigrationengine.handlers.NotificationHandler
import pricemigrationengine.migrations.GW2024Migration
import pricemigrationengine.model._
import zio.{IO, Random}

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
      case GW2024            => GW2024Migration.subscriptionToLastPriceMigrationDate(subscription)
      case SupporterPlus2024 => None
    }
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
        case GW2024            => 3
        case SupporterPlus2024 => 1 // no spread for S+2024 monthlies
      }
    } else 1
  }

  def startDateLowerBound(
      subscription: ZuoraSubscription,
      invoicePreview: ZuoraInvoiceList,
      cohortSpec: CohortSpec,
      today: LocalDate
  ): IO[ConfigFailure, LocalDate] = {

    // LowerBound from to the cohort spec and the notification window's end
    val startDateLowerBound1 = MigrationType(cohortSpec) match {
      case GW2024            => cohortSpecLowerBound(cohortSpec, today)
      case SupporterPlus2024 => cohortSpecLowerBound(cohortSpec, today)
    }

    // We now respect the policy of not increasing members during their first year
    val startDateLowerBound2 = noPriceRiseDuringSubscriptionFirstYearPolicyUpdate(startDateLowerBound1, subscription)

    // And the policy not to price rise a sub twice within 12 months of any possible price rise
    val startDateLowerBound3 =
      noPriceRiseWithinAYearOfLastPriceRisePolicyUpdate(cohortSpec, subscription, startDateLowerBound2)

    // Decide the spread period for this migration
    val spreadPeriod = decideSpreadPeriod(subscription, invoicePreview, cohortSpec)

    for {
      randomFactor <- Random.nextIntBetween(0, spreadPeriod)
    } yield startDateLowerBound3.plusMonths(randomFactor)
  }
}
