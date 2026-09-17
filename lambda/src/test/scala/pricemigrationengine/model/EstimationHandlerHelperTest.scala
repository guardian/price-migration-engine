package pricemigrationengine.model

import pricemigrationengine.Fixtures
import pricemigrationengine.model.SI2025RateplanFromSubAndInvoices

import java.time.LocalDate

class EstimationHandlerHelperTest extends munit.FunSuite {

  test("EstimationAnalysisResult.checkActiveRatePlanUniqueness (standard)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/EstimationHandlerHelper/newspaper-2026/subscription.json"
      )
    assertEquals(
      EstimationAnalysisResult.checkActiveRatePlanUniqueness(DataPacket(subscription, LocalDate.of(2026, 9, 10))),
      None
    )
  }
  test("EstimationAnalysisResult.checkActiveRatePlanUniqueness (oddity)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/EstimationHandlerHelper/newspaper-delivery-echo-legacy-multiple-billing-period-detection/subscription.json"
      )

    // Here we have an oddity, two different billing periods carried by the same rate plan
    assertEquals(
      EstimationAnalysisResult.checkActiveRatePlanUniqueness(DataPacket(subscription, LocalDate.of(2026, 9, 10))),
      Some(EARPrintWithTwoBillingPeriods)
    )
  }
}
