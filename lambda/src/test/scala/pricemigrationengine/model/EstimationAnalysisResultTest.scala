package pricemigrationengine.model

import pricemigrationengine.Fixtures
import pricemigrationengine.model.SI2025RateplanFromSubAndInvoices

import java.time.LocalDate

class EstimationAnalysisResultTest extends munit.FunSuite {

  test("EstimationAnalysisResult.checkActiveRatePlanUniqueness (standard)") {
    val f1: Int => Option[String] = n => if (n > 10) Some("big") else None
    val f2: Int => Option[String] = n => if (n > 5) Some("medium") else None
    val f3: Int => Option[String] = n => if (n > 0) Some("small") else None
    assertEquals(
      EstimationAnalysisResult.firstVetoElseDefault(4, List(f1, f2, f3), "it wasn't vetoed"),
      "small"
    )
  }

  test("EstimationAnalysisResult.checkActiveRatePlanUniqueness (standard)") {
    val f1: Int => Option[String] = n => if (n > 10) Some(s"big: $n") else None
    val f2: Int => Option[String] = n => if (n > 5) Some(s"medium: $n") else None
    val f3: Int => Option[String] = n => if (n > 0) Some(s"small: $n") else None
    assertEquals(
      EstimationAnalysisResult.firstVetoElseDefault(-1, List(f1, f2, f3), "it wasn't vetoed"),
      "it wasn't vetoed"
    )
  }

  test("EstimationAnalysisResult.checkActiveRatePlanUniqueness (standard)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/EstimationAnalysisResult/newspaper-2026/subscription.json"
      )
    assertEquals(
      EstimationAnalysisResult.checkActiveRatePlanBillingPeriodsUniqueness(
        CheckInput(subscription, LocalDate.of(2026, 9, 10))
      ),
      None
    )
  }
  test("EstimationAnalysisResult.checkActiveRatePlanUniqueness (oddity)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/EstimationAnalysisResult/newspaper-delivery-echo-legacy-multiple-billing-period-detection/subscription.json"
      )

    // Here we have an oddity, two different billing periods carried by the same rate plan
    assertEquals(
      EstimationAnalysisResult.checkActiveRatePlanBillingPeriodsUniqueness(
        CheckInput(subscription, LocalDate.of(2026, 9, 10))
      ),
      Some(EARPrintWithMoreThanTwoBillingPeriods)
    )
  }
  test("EstimationAnalysisResult.checkSubscriptionStatus (standard)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/EstimationAnalysisResult/newspaper-2026/subscription.json"
      )
    // Status is active, so we get a None
    assertEquals(
      EstimationAnalysisResult.checkSubscriptionStatus(CheckInput(subscription, LocalDate.of(2026, 9, 10))),
      None
    )
  }
  test("EstimationAnalysisResult.checkSubscriptionStatus (Cancelled)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/EstimationAnalysisResult/newspaper-2026-Cancelled/subscription.json"
      )
    // Status is active, so we get a None
    assertEquals(
      EstimationAnalysisResult.checkSubscriptionStatus(CheckInput(subscription, LocalDate.of(2026, 9, 10))),
      Some(EARSubscriptionCancelled)
    )
  }
  test("EstimationAnalysisResult.checkSubscriptionAutoRenewFlag (standard)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/EstimationAnalysisResult/newspaper-2026/subscription.json"
      )
    // autoRenew: true
    assertEquals(
      EstimationAnalysisResult.checkSubscriptionAutoRenewFlag(CheckInput(subscription, LocalDate.of(2026, 9, 10))),
      None
    )
  }
  test("EstimationAnalysisResult.checkSubscriptionAutoRenewFlag (Cancelled)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/EstimationAnalysisResult/newspaper-2026-AutoRenewFlagFalse/subscription.json"
      )
    // autoRenew: false
    assertEquals(
      EstimationAnalysisResult.checkSubscriptionAutoRenewFlag(CheckInput(subscription, LocalDate.of(2026, 9, 10))),
      Some(EARSubscriptionAutoRenewFlagFalse)
    )
  }
  test("EstimationAnalysisResult.subscriptionEstimationAnalysis (Cancelled)") {
    // We are now going to look at a subscription that passes subscriptionEstimationAnalysis
    // or not depending on the cohort name
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/EstimationAnalysisResult/newspaper-delivery-echo-legacy-multiple-billing-period-detection/subscription.json"
      )

    // The sub gets clearance with SupporterPlus2026
    assertEquals(
      EstimationAnalysisResult
        .subscriptionEstimationAnalysis(CohortSpec("SupporterPlus2026", true), subscription, LocalDate.of(2026, 9, 17)),
      EARClearance
    )

    // but not with Print2026C1NPAnnualsUK, because of the extra
    // checkActiveRatePlanUniqueness
    assertEquals(
      EstimationAnalysisResult.subscriptionEstimationAnalysis(
        CohortSpec("Print2026C1NPAnnualsUK", true),
        subscription,
        LocalDate.of(2026, 9, 17)
      ),
      EARPrintWithMoreThanTwoBillingPeriods
    )
  }
}
