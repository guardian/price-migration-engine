package pricemigrationengine.model

import pricemigrationengine.Fixtures
import pricemigrationengine.migrations.GuardianWeekly2025Migration
import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation

import java.time.LocalDate

class AmendmentEffectiveDateCalculatorTest extends munit.FunSuite {

  test("lowerBound (1)") {
    val subscription =
      Fixtures.subscriptionFromJson("model/AmendmentEffectiveDateCalculator/subscription1/subscription.json")
    val account = Fixtures.accountFromJson("model/AmendmentEffectiveDateCalculator/subscription1/account.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("model/AmendmentEffectiveDateCalculator/subscription1/invoice-preview.json")

    val cohortItem = CohortItem("SUBSCRIPTION-NUMBER", ReadyForEstimation)
    val today = LocalDate.of(2025, 7, 1) // 1 July 2025
    val cohortSpec = CohortSpec(
      cohortName = "Test1",
      brazeName = "BrazeName",
      earliestAmendmentEffectiveDate = LocalDate.of(2025, 9, 10) // 10 Sept 2025
    )

    // --------------------------------------------------
    // lowerBound1:

    val lowerBound1 = AmendmentEffectiveDateCalculator.cohortSpecLowerBound(cohortSpec, today)

    assertEquals(
      lowerBound1,
      LocalDate.of(2025, 9, 10)
    )

    // --------------------------------------------------
    // lowerBound2:

    val lowerBound2 =
      AmendmentEffectiveDateCalculator.noPriceRiseDuringSubscriptionFirstYearPolicyUpdate(lowerBound1, subscription)

    // We compare lowerBound1 and subscription.subscriptionStartDate.plusMonths(12)
    // I manually set subscriptionStartDate to 2025-02-24, leading to a plus 12 months of 2026-02-24

    assertEquals(
      lowerBound2,
      LocalDate.of(2026, 2, 24)
    )

    // --------------------------------------------------
    // lowerBound3:

    // Test1 doesn't have a last price rise date, so we are invariant here:

    val lowerBound3 =
      AmendmentEffectiveDateCalculator.noPriceRiseWithinAYearOfLastPriceRisePolicyUpdate(
        cohortSpec,
        subscription,
        today,
        lowerBound2
      )

    assertEquals(
      lowerBound3,
      LocalDate.of(2026, 2, 24)
    )

    // --------------------------------------------------
    // lowerBound4:

    // [1]
    // Date: June 2025
    // Author: Pascal
    // (Comment group: ef77de28)

    // Here I am re-using GuardianWeekly2025Migration.computeStartDateLowerBound4, for testing it.
    // Technically this test will break when GuardianWeekly2025 is decommissioned in October 2026,
    // but at that point if we really want to carry on testing the migration extended attributes as
    // part of start date computations we can move the code to Test1's own migration module

    // Here nothing happens because the cohort item didn't have extended attributes

    val lowerBound4 = GuardianWeekly2025Migration.computeAmendmentEffectiveDateLowerBound4(lowerBound3, cohortItem)

    assertEquals(
      lowerBound3,
      LocalDate.of(2026, 2, 24)
    )

    // --------------------------------------------------
    // lowerBound:

    // We report the highest computed date. Without random spread.
    // (Test1 spread period is set to 1)

    assertEquals(
      AmendmentEffectiveDateCalculator.amendmentEffectiveDateLowerBound(
        item = cohortItem,
        subscription = subscription,
        invoicePreview = invoicePreview,
        cohortSpec = cohortSpec,
        today = today
      ),
      LocalDate.of(2026, 2, 24)
    )
  }

  test("lowerBound (2): with extended attributes") {
    val subscription =
      Fixtures.subscriptionFromJson("model/AmendmentEffectiveDateCalculator/subscription1/subscription.json")
    val account = Fixtures.accountFromJson("model/AmendmentEffectiveDateCalculator/subscription1/account.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("model/AmendmentEffectiveDateCalculator/subscription1/invoice-preview.json")

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
    // lowerBound1:

    val lowerBound1 = AmendmentEffectiveDateCalculator.cohortSpecLowerBound(cohortSpec, today)

    assertEquals(
      lowerBound1,
      LocalDate.of(2025, 9, 10)
    )

    // --------------------------------------------------
    // lowerBound2:

    val lowerBound2 =
      AmendmentEffectiveDateCalculator.noPriceRiseDuringSubscriptionFirstYearPolicyUpdate(lowerBound1, subscription)

    // We compare lowerBound1 and subscription.customerAcceptanceDate.plusMonths(12)
    // I set customerAcceptanceDate to 2025-02-24, leading to a plus 12 months of 2026-02-24

    assertEquals(
      lowerBound2,
      LocalDate.of(2026, 2, 24)
    )

    // --------------------------------------------------
    // lowerBound3:

    // Test1 doesn't have a last price rise date, so we are invariant here:

    val lowerBound3 =
      AmendmentEffectiveDateCalculator.noPriceRiseWithinAYearOfLastPriceRisePolicyUpdate(
        cohortSpec,
        subscription,
        today,
        lowerBound2
      )

    assertEquals(
      lowerBound3,
      LocalDate.of(2026, 2, 24)
    )

    // --------------------------------------------------
    // lowerBound4:

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

    val lowerBound4 = GuardianWeekly2025Migration.computeAmendmentEffectiveDateLowerBound4(lowerBound3, cohortItem)

    assertEquals(
      lowerBound4,
      LocalDate.of(2026, 3, 19)
    )

    // --------------------------------------------------
    // lowerBound:

    // We report the highest computed date. Without random spread.
    // (Test1 spread period is set to 1)

    assertEquals(
      AmendmentEffectiveDateCalculator.amendmentEffectiveDateLowerBound(
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

  test("no price rise during first year after acquisition policy (1)") {

    // subscription-03 : Digital Pack Annual  (acquisition: 2025-09-12) : Annually (subscription start date: 2025-09-12)
    // Current date is 2025-11-25

    // For subscription 03 we expect min [2025-11-25, 2025-09-12 + 12 months] = 2026-09-12

    val subscription03 =
      Fixtures.subscriptionFromJson("model/AmendmentEffectiveDateCalculator/subscription-03/subscription.json")

    val currentDate = LocalDate.of(2025, 11, 25)

    assertEquals(
      AmendmentEffectiveDateCalculator
        .noPriceRiseDuringSubscriptionFirstYearPolicyUpdate(currentDate, subscription03),
      LocalDate.of(2026, 9, 12)
    )
  }

  test("no price rise during first year after acquisition policy (2)") {

    // subscription-04 : Digital Pack Monthly (acquisition: 2025-11-19) : Monthly  (subscription start date: 2025-11-19)

    // Current date is 2025-11-25

    // For subscription 04 we expect min [2025-11-25, 2025-11-19 + 12 months] = 2026-11-19

    val subscription04 =
      Fixtures.subscriptionFromJson("model/AmendmentEffectiveDateCalculator/subscription-04/subscription.json")

    val currentDate = LocalDate.of(2025, 11, 25)

    assertEquals(
      AmendmentEffectiveDateCalculator
        .noPriceRiseDuringSubscriptionFirstYearPolicyUpdate(currentDate, subscription04),
      LocalDate.of(2026, 11, 19)
    )
  }
}
