package pricemigrationengine.model

import pricemigrationengine.Fixtures
import pricemigrationengine.model.SI2025RateplanFromSubAndInvoices

import java.time.LocalDate

class EstimationHandlerHelperTest extends munit.FunSuite {

  test("EstimationHandlerHelper.printProduct2026EstimationAnalysis (standard)") {
    val subscription =
      Fixtures.subscriptionFromJson("model/EstimationHandlerHelper/newspaper-2026/subscription.json")

    // With that subscription we are expecting just one unambiguous billing period, we get a EARClearance

    assertEquals(
      EstimationHandlerHelper.printProduct2026EstimationAnalysis(subscription, LocalDate.of(2026, 9, 10)),
      EARClearance
    )
  }
  test("EstimationHandlerHelper.printProduct2026EstimationAnalysis (oddity)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/EstimationHandlerHelper/newspaper-delivery-echo-legacy-multiple-billing-period-detection/subscription.json"
      )

    // Here we have an oddity, two different billing periods carried by the same rate plan

    assertEquals(
      EstimationHandlerHelper.printProduct2026EstimationAnalysis(subscription, LocalDate.of(2026, 9, 10)),
      EARPrintWithTwoBillingPeriods
    )
  }
}
