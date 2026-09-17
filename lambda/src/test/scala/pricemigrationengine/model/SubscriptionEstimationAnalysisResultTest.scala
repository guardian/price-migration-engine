package pricemigrationengine.model

import pricemigrationengine.Fixtures
import pricemigrationengine.model.SI2025RateplanFromSubAndInvoices

import java.time.LocalDate

class SubscriptionEstimationAnalysisResultTest extends munit.FunSuite {

  test("SubscriptionEstimationAnalysisResult.checkActiveRatePlanUniqueness (standard)") {
    val f1: Int => Option[String] = n => if (n > 10) Some("big") else None
    val f2: Int => Option[String] = n => if (n > 5) Some("medium") else None
    val f3: Int => Option[String] = n => if (n > 0) Some("small") else None
    assertEquals(
      SubscriptionEstimationAnalysisResult.firstDefined(4, List(f1, f2, f3), "it wasn't vetoed"),
      "small"
    )
  }

  test("SubscriptionEstimationAnalysisResult.checkActiveRatePlanUniqueness (standard)") {
    val f1: Int => Option[String] = n => if (n > 10) Some(s"big: $n") else None
    val f2: Int => Option[String] = n => if (n > 5) Some(s"medium: $n") else None
    val f3: Int => Option[String] = n => if (n > 0) Some(s"small: $n") else None
    assertEquals(
      SubscriptionEstimationAnalysisResult.firstDefined(-1, List(f1, f2, f3), "it wasn't vetoed"),
      "it wasn't vetoed"
    )
  }

  test("SubscriptionEstimationAnalysisResult.checkActiveRatePlanUniqueness (standard)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/EstimationAnalysisResult/newspaper-2026/subscription.json"
      )
    assertEquals(
      SubscriptionEstimationAnalysisResult.checkActiveRatePlanBillingPeriodsUniqueness(
        CheckInput(subscription, LocalDate.of(2026, 9, 10))
      ),
      None
    )
  }
  test("SubscriptionEstimationAnalysisResult.checkActiveRatePlanUniqueness (oddity)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/EstimationAnalysisResult/newspaper-delivery-echo-legacy-multiple-billing-period-detection/subscription.json"
      )

    // Here we have an oddity, two different billing periods carried by the same rate plan
    assertEquals(
      SubscriptionEstimationAnalysisResult.checkActiveRatePlanBillingPeriodsUniqueness(
        CheckInput(subscription, LocalDate.of(2026, 9, 10))
      ),
      Some(EARPrintWithMoreThanTwoBillingPeriods)
    )
  }
  test("SubscriptionEstimationAnalysisResult.checkSubscriptionStatus (standard)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/EstimationAnalysisResult/newspaper-2026/subscription.json"
      )
    // Status is active, so we get a None
    assertEquals(
      SubscriptionEstimationAnalysisResult.checkSubscriptionStatus(CheckInput(subscription, LocalDate.of(2026, 9, 10))),
      None
    )
  }
  test("SubscriptionEstimationAnalysisResult.checkSubscriptionStatus (Cancelled)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/EstimationAnalysisResult/newspaper-2026-Cancelled/subscription.json"
      )
    // Status is active, so we get a None
    assertEquals(
      SubscriptionEstimationAnalysisResult.checkSubscriptionStatus(CheckInput(subscription, LocalDate.of(2026, 9, 10))),
      Some(EARSubscriptionCancelled)
    )
  }
  test("SubscriptionEstimationAnalysisResult.checkSubscriptionAutoRenewFlag (standard)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/EstimationAnalysisResult/newspaper-2026/subscription.json"
      )
    // autoRenew: true
    assertEquals(
      SubscriptionEstimationAnalysisResult.checkSubscriptionAutoRenewFlag(
        CheckInput(subscription, LocalDate.of(2026, 9, 10))
      ),
      None
    )
  }
  test("SubscriptionEstimationAnalysisResult.checkSubscriptionAutoRenewFlag (Cancelled)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/EstimationAnalysisResult/newspaper-2026-AutoRenewFlagFalse/subscription.json"
      )
    // autoRenew: false
    assertEquals(
      SubscriptionEstimationAnalysisResult.checkSubscriptionAutoRenewFlag(
        CheckInput(subscription, LocalDate.of(2026, 9, 10))
      ),
      Some(EARSubscriptionAutoRenewFlagFalse)
    )
  }
  test("SubscriptionEstimationAnalysisResult.subscriptionEstimationAnalysis (Cancelled)") {
    // We are now going to look at a subscription that passes subscriptionEstimationAnalysis
    // or not depending on the cohort name
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/EstimationAnalysisResult/newspaper-delivery-echo-legacy-multiple-billing-period-detection/subscription.json"
      )

    // The sub gets clearance with SupporterPlus2026
    assertEquals(
      SubscriptionEstimationAnalysisResult
        .subscriptionEstimationAnalysis(CohortSpec("SupporterPlus2026", true), subscription, LocalDate.of(2026, 9, 17)),
      EARClearance
    )

    // but not with Print2026C1NPAnnualsUK, because of the extra
    // checkActiveRatePlanUniqueness
    assertEquals(
      SubscriptionEstimationAnalysisResult.subscriptionEstimationAnalysis(
        CohortSpec("Print2026C1NPAnnualsUK", true),
        subscription,
        LocalDate.of(2026, 9, 17)
      ),
      EARPrintWithMoreThanTwoBillingPeriods
    )
  }
}
