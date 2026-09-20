package pricemigrationengine.model

import pricemigrationengine.Fixtures
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
      active = true
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
