package pricemigrationengine.model

import pricemigrationengine.handlers.NotificationHandler
import pricemigrationengine.migrations.{
  GuardianWeekly2025Migration,
  HomeDelivery2025Migration,
  Newspaper2025P1Migration,
  Newspaper2025P3Migration
}
import scala.util.Random
import java.time.LocalDate

/*
  This aggregates the utility functions required to compute the migration dates
  for a cohort item. They are the date the migration amendment is taking effect.
 */

object AmendmentEffectiveDateCalculator {

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
      case Test1                  => None // default value
      case SupporterPlus2024      => None
      case GuardianWeekly2025     => GuardianWeekly2025Migration.subscriptionToLastPriceMigrationDate(subscription)
      case Newspaper2025P1        => Newspaper2025P1Migration.subscriptionToLastPriceMigrationDate(subscription)
      case HomeDelivery2025       => HomeDelivery2025Migration.subscriptionToLastPriceMigrationDate(subscription)
      case Newspaper2025P3        => Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate(subscription)
      case ProductMigration2025N4 => None
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
      cohortSpec.earliestAmendmentEffectiveDate,
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
        case Test1                  => 1 // default value
        case SupporterPlus2024      => 1 // no spread for S+2024 monthlies
        case GuardianWeekly2025     => 1 // no spread for Guardian Weekly 2025
        case Newspaper2025P1        => 1 // no spread for Newspaper 2025
        case HomeDelivery2025       => 1 // no spread for Home Delivery 2025
        case Newspaper2025P3        => 1 // no spread for Newspaper 2025 (Phase 3)
        case ProductMigration2025N4 => 1
      }
    } else 1
  }

  def AmendmentEffectiveDateLowerBound(
      item: CohortItem,
      subscription: ZuoraSubscription,
      invoicePreview: ZuoraInvoiceList,
      cohortSpec: CohortSpec,
      today: LocalDate
  ): LocalDate = {

    // LowerBound from to the cohort spec and the notification window's end
    val lowerBound1 = MigrationType(cohortSpec) match {
      case Test1                  => cohortSpecLowerBound(cohortSpec, today)
      case SupporterPlus2024      => cohortSpecLowerBound(cohortSpec, today)
      case GuardianWeekly2025     => cohortSpecLowerBound(cohortSpec, today)
      case Newspaper2025P1        => cohortSpecLowerBound(cohortSpec, today)
      case HomeDelivery2025       => cohortSpecLowerBound(cohortSpec, today)
      case Newspaper2025P3        => cohortSpecLowerBound(cohortSpec, today)
      case ProductMigration2025N4 => cohortSpecLowerBound(cohortSpec, today)
    }

    // We now respect the policy of not increasing members during their first year
    // This doesn't apply to ProductMigration2025N4 which is not a price rise
    val lowerBound2 = MigrationType(cohortSpec) match {
      case ProductMigration2025N4 => lowerBound1
      case _                      => noPriceRiseDuringSubscriptionFirstYearPolicyUpdate(lowerBound1, subscription)
    }

    // And the policy not to price rise a sub twice within 12 months of any possible price rise
    // This doesn't apply to ProductMigration2025N4 which is not a price rise
    val lowerBound3 = MigrationType(cohortSpec) match {
      case ProductMigration2025N4 => lowerBound2
      case _ => noPriceRiseWithinAYearOfLastPriceRisePolicyUpdate(cohortSpec, subscription, lowerBound2)
    }

    // With GuardianWeekly2025, we were given lower bounds in the Marketing spreadsheet
    // that was the first use of the new cohortItem's migrationExtraAttributes. If we expect a
    // migration to provide it own lowerbound computation, we do it here, otherwise we identity
    // on lowerBound3
    val lowerBound4 = MigrationType(cohortSpec) match {
      // [1]
      // Date: June 2025
      // Author: Pascal
      // (Comment group: ef77de28)
      // Here I am re-using GuardianWeekly2025Migration.computeStartDateLowerBound4, for testing.
      // Technically this test will break when GuardianWeekly2025 is decommissioned in October 2026,
      // but at that point if we really want to carry on testing the migration extended attributes as
      // part of start date computations we can move the code to Test1's own migration module
      case Test1 => GuardianWeekly2025Migration.computeAmendmentEffectiveDateLowerBound4(lowerBound3, item) // [1]
      case SupporterPlus2024  => lowerBound3
      case GuardianWeekly2025 => GuardianWeekly2025Migration.computeAmendmentEffectiveDateLowerBound4(lowerBound3, item)
      case Newspaper2025P1    => lowerBound3
      case HomeDelivery2025   => lowerBound3
      case Newspaper2025P3    => Newspaper2025P3Migration.computeAmendmentEffectiveDateLowerBound4(lowerBound3, item)
      case ProductMigration2025N4 => lowerBound3
    }

    // Decide the spread period for this migration
    val spreadPeriod = decideSpreadPeriod(subscription, invoicePreview, cohortSpec)

    val randomDelayInMonths = Random.nextInt(spreadPeriod) // [1]
    // [1]
    // Decides an integer in the interval [0, spreadPeriod-1]
    // The default spread period is 1

    lowerBound4.plusMonths(randomDelayInMonths)
  }
}
