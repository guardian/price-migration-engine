package pricemigrationengine.model

import pricemigrationengine.Fixtures
import pricemigrationengine.model.SI2025RateplanFromSubAndInvoices

class AmendmentHandlerHelperTest extends munit.FunSuite {

  test("subscriptionHasCorrectBillingPeriodAfterUpdate is computed correctly (1)") {

    // --------------------------------------------------------------------
    // Fixtures:

    // 277291-everyday-annual
    val subscription1 =
      Fixtures.subscriptionFromJson("model/AmendmentHandlerHelperTest/277291-everyday-annual/subscription.json")
    val invoicePreview1 =
      Fixtures.invoiceListFromJson("model/AmendmentHandlerHelperTest/277291-everyday-annual/invoice-preview.json")

    // 277750-everyday-month
    val subscription2 =
      Fixtures.subscriptionFromJson("model/AmendmentHandlerHelperTest/277750-everyday-month/subscription.json")
    val invoicePreview2 =
      Fixtures.invoiceListFromJson("model/AmendmentHandlerHelperTest/277750-everyday-month/invoice-preview.json")

    assertEquals(
      AmendmentHandlerHelper.subscriptionHasCorrectBillingPeriodAfterUpdate(
        Some("Annual"),
        subscription1,
        invoicePreview1
      ),
      Some(true)
    )

    assertEquals(
      AmendmentHandlerHelper.subscriptionHasCorrectBillingPeriodAfterUpdate(
        Some("Annual"),
        subscription2, // this one is a Monthly
        invoicePreview2
      ),
      Some(false)
    )

    assertEquals(
      AmendmentHandlerHelper.subscriptionHasCorrectBillingPeriodAfterUpdate(
        None, // Also testing that the answer should be None if the reference billing period is None
        subscription2,
        invoicePreview2
      ),
      None
    )
  }

  test("subscriptionHasCorrectBillingPeriodAfterUpdate is computed correctly (2)") {

    val subscription =
      Fixtures.subscriptionFromJson("model/AmendmentHandlerHelperTest/1761090945067/subscription.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("model/AmendmentHandlerHelperTest/1761090945067/invoice-preview.json")

    assertEquals(
      SI2025RateplanFromSubAndInvoices.invoicePreviewToUniqueChargeNumbers(invoicePreview),
      List(
        "C-07370728",
        "C-07370729",
        "C-05616013",
        "C-05616014"
      )
    )

    assertEquals(
      SI2025RateplanFromSubAndInvoices
        .determineRatePlan(
          subscription,
          invoicePreview
        )
        .get
        .id,
      "8a128d8c99f14124019a093248f06ee6"
    )

    assertEquals(
      AmendmentHandlerHelper.subscriptionHasCorrectBillingPeriodAfterUpdate(
        Some("Month"),
        subscription, // this one is a Monthly
        invoicePreview
      ),
      Some(true)
    )
  }
}
