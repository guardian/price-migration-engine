package pricemigrationengine.libs

import pricemigrationengine.Fixtures
import pricemigrationengine.libs.AmendmentHelper
import pricemigrationengine.model.{Annual, BillingPeriod, ZuoraInvoiceList, ZuoraSubscription}

class AmendmentHelperTest extends munit.FunSuite {

  test("subscriptionHasCorrectBillingPeriodAfterUpdate is computed correctly") {

    // --------------------------------------------------------------------
    // Fixtures:

    // 277291-everyday-annual
    val subscription1 =
      Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/subscription.json")
    val invoicePreview1 =
      Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/invoice-preview.json")

    // 277750-everyday-month
    val subscription2 =
      Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/277750-everyday-month/subscription.json")
    val invoicePreview2 =
      Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/277750-everyday-month/invoice-preview.json")

    assertEquals(
      AmendmentHelper.subscriptionHasCorrectBillingPeriodAfterUpdate(
        Annual,
        subscription1,
        invoicePreview1
      ),
      Some(true)
    )

    assertEquals(
      AmendmentHelper.subscriptionHasCorrectBillingPeriodAfterUpdate(
        Annual,
        subscription2, // this one is a Monthly
        invoicePreview2
      ),
      Some(false)
    )
  }
}
