package pricemigrationengine.model

import pricemigrationengine.Fixtures
import pricemigrationengine.model.SI2025RateplanFromSubAndInvoices

import java.time.LocalDate

class EstimationAnalysisResultTest extends munit.FunSuite {

  test("EstimationAnalysisResult.printProduct2026EstimationAnalysis (standard)") {
    val subscription =
      Fixtures.subscriptionFromJson("model/EstimationAnalysisResult/newspaper-2026/subscription.json")

    // With that subscription we are expecting just one unambiguous billing period, we get a EARClearance

    assertEquals(
      EstimationAnalysisResult.printProduct2026EstimationAnalysis(subscription, LocalDate.of(2026, 9, 10)),
      EARClearance
    )

    assertEquals(
      EstimationAnalysisResult.subscriptionEstimationAnalysis(
        CohortSpec("Print2026C1NPAnnualsUK", active = true),
        subscription,
        LocalDate.of(2026, 9, 10)
      ),
      EARClearance
    )
  }
  test("EstimationAnalysisResult.printProduct2026EstimationAnalysis (oddity)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/EstimationAnalysisResult/newspaper-delivery-echo-legacy-multiple-billing-period-detection/subscription.json"
      )

    // Here we have an oddity, two different billing periods carried by the same rate plan

    assertEquals(
      EstimationAnalysisResult.printProduct2026EstimationAnalysis(subscription, LocalDate.of(2026, 9, 10)),
      EARPrintWithMoreThanTwoBillingPeriods
    )

    assertEquals(
      EstimationAnalysisResult.subscriptionEstimationAnalysis(
        CohortSpec("Print2026C1NPAnnualsUK", active = true),
        subscription,
        LocalDate.of(2026, 9, 10)
      ),
      EARPrintWithMoreThanTwoBillingPeriods
    )

    // And to show that printProduct2026EstimationAnalysis is limited to the 2026 newspapers

    assertEquals(
      EstimationAnalysisResult.subscriptionEstimationAnalysis(
        CohortSpec("Print2026C1GWQuarterliesUK", active = true),
        subscription,
        LocalDate.of(2026, 9, 10)
      ),
      EARClearance
    )
  }
}
