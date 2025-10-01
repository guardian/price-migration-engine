package pricemigrationengine.model

import pricemigrationengine.Fixtures
import pricemigrationengine.migrations.GuardianWeekly2025Migration
import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation

import java.time.LocalDate

class StartDatesTest extends munit.FunSuite {

  test("startDateLowerBound (1)") {
    val subscription = Fixtures.subscriptionFromJson("model/StartDates/subscription1/subscription.json")
    val account = Fixtures.accountFromJson("model/StartDates/subscription1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("model/StartDates/subscription1/invoice-preview.json")

    val cohortItem = CohortItem("SUBSCRIPTION-NUMBER", ReadyForEstimation)
    val today = LocalDate.of(2025, 7, 1) // 1 July 2025
    val cohortSpec = CohortSpec(
      cohortName = "Test1",
      brazeName = "BrazeName",
      earliestAmendmentEffectiveDate = LocalDate.of(2025, 9, 10) // 10 Sept 2025
    )

    // --------------------------------------------------
    // startDateLowerBound1:

    val startDateLowerBound1 = StartDates.cohortSpecLowerBound(cohortSpec, today)

    assertEquals(
      startDateLowerBound1,
      LocalDate.of(2025, 9, 10)
    )

    // --------------------------------------------------
    // startDateLowerBound2:

    val startDateLowerBound2 =
      StartDates.noPriceRiseDuringSubscriptionFirstYearPolicyUpdate(startDateLowerBound1, subscription)

    // We compare startDateLowerBound1 and subscription.customerAcceptanceDate.plusMonths(12)
    // I set customerAcceptanceDate to 2025-02-24, leading to a plus 12 months of 2026-02-24

    assertEquals(
      startDateLowerBound2,
      LocalDate.of(2026, 2, 24)
    )

    // --------------------------------------------------
    // startDateLowerBound3:

    // Test1 doesn't have a last price rise date, so we are invariant here:

    val startDateLowerBound3 =
      StartDates.noPriceRiseWithinAYearOfLastPriceRisePolicyUpdate(cohortSpec, subscription, startDateLowerBound2)

    assertEquals(
      startDateLowerBound3,
      LocalDate.of(2026, 2, 24)
    )

    // --------------------------------------------------
    // startDateLowerBound4:

    // [1]
    // Date: June 2025
    // Author: Pascal
    // (Comment group: ef77de28)

    // Here I am re-using GuardianWeekly2025Migration.computeStartDateLowerBound4, for testing it.
    // Technically this test will break when GuardianWeekly2025 is decommissioned in October 2026,
    // but at that point if we really want to carry on testing the migration extended attributes as
    // part of start date computations we can move the code to Test1's own migration module

    // Here nothing happens because the cohort item didn't have extended attributes

    val startDateLowerBound4 = GuardianWeekly2025Migration.computeStartDateLowerBound4(startDateLowerBound3, cohortItem)

    assertEquals(
      startDateLowerBound3,
      LocalDate.of(2026, 2, 24)
    )

    // --------------------------------------------------
    // startDateLowerBound:

    // We report the highest computed date. Without random spread.
    // (Test1 spread period is set to 1)

    assertEquals(
      StartDates.startDateLowerBound(
        item = cohortItem,
        subscription = subscription,
        invoicePreview = invoicePreview,
        cohortSpec = cohortSpec,
        today = today
      ),
      LocalDate.of(2026, 2, 24)
    )
  }

  test("startDateLowerBound (2): with extended attributes") {
    val subscription = Fixtures.subscriptionFromJson("model/StartDates/subscription1/subscription.json")
    val account = Fixtures.accountFromJson("model/StartDates/subscription1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("model/StartDates/subscription1/invoice-preview.json")

    val cohortItem = CohortItem(
      "SUBSCRIPTION-NUMBER",
      ReadyForEstimation,
      migrationExtraAttributes = Some(""" {"earliestMigrationDate":"2026-03-19"} """)
    )
    val today = LocalDate.of(2025, 7, 1) // 1 July 2025
    val cohortSpec = CohortSpec(
      cohortName = "Test1",
      brazeName = "BrazeName",
      earliestAmendmentEffectiveDate = LocalDate.of(2025, 9, 10) // 10 Sept 2025
    )

    // --------------------------------------------------
    // startDateLowerBound1:

    val startDateLowerBound1 = StartDates.cohortSpecLowerBound(cohortSpec, today)

    assertEquals(
      startDateLowerBound1,
      LocalDate.of(2025, 9, 10)
    )

    // --------------------------------------------------
    // startDateLowerBound2:

    val startDateLowerBound2 =
      StartDates.noPriceRiseDuringSubscriptionFirstYearPolicyUpdate(startDateLowerBound1, subscription)

    // We compare startDateLowerBound1 and subscription.customerAcceptanceDate.plusMonths(12)
    // I set customerAcceptanceDate to 2025-02-24, leading to a plus 12 months of 2026-02-24

    assertEquals(
      startDateLowerBound2,
      LocalDate.of(2026, 2, 24)
    )

    // --------------------------------------------------
    // startDateLowerBound3:

    // Test1 doesn't have a last price rise date, so we are invariant here:

    val startDateLowerBound3 =
      StartDates.noPriceRiseWithinAYearOfLastPriceRisePolicyUpdate(cohortSpec, subscription, startDateLowerBound2)

    assertEquals(
      startDateLowerBound3,
      LocalDate.of(2026, 2, 24)
    )

    // --------------------------------------------------
    // startDateLowerBound4:

    // We start by checking the
    // GuardianWeekly2025Migration.getEarliestMigrationDateFromMigrationExtraAttributes(item)
    // function itself

    assertEquals(
      GuardianWeekly2025Migration.getEarliestMigrationDateFromMigrationExtraAttributes(cohortItem),
      Some(LocalDate.of(2026, 3, 19))
    )

    // [1]
    // Date: June 2025
    // Author: Pascal
    // (Comment group: ef77de28)

    // Here I am re-using GuardianWeekly2025Migration.computeStartDateLowerBound4, for testing it.
    // Technically this test will break when GuardianWeekly2025 is decommissioned in October 2026,
    // but at that point if we really want to carry on testing the migration extended attributes as
    // part of start date computations we can move the code to Test1's own migration module

    // Here we have extended attributes: {"earliestMigrationDate":"2026-03-19"}

    val startDateLowerBound4 = GuardianWeekly2025Migration.computeStartDateLowerBound4(startDateLowerBound3, cohortItem)

    assertEquals(
      startDateLowerBound4,
      LocalDate.of(2026, 3, 19)
    )

    // --------------------------------------------------
    // startDateLowerBound:

    // We report the highest computed date. Without random spread.
    // (Test1 spread period is set to 1)

    assertEquals(
      StartDates.startDateLowerBound(
        item = cohortItem,
        subscription = subscription,
        invoicePreview = invoicePreview,
        cohortSpec = cohortSpec,
        today = today
      ),
      LocalDate.of(2026, 3, 19) // [1]
    )

    // [1]
    // Which is higher than LocalDate.of(2026, 2, 24) computed in the previous test and
    // Equal to the extra attributes
  }
}
