package pricemigrationengine.model

import java.time.LocalDate

import pricemigrationengine.Fixtures._
import pricemigrationengine.model.ZuoraProductCatalogue.productPricingMap

class AmendmentDataTest extends munit.FunSuite {

  private def migrationStartDate = LocalDate.of(2020, 12, 25)

  test("nextBillingDate: billing date is first after migration start date") {
    val invoiceList = invoiceListFromJson("InvoicePreview.json")
    val billingDate = AmendmentData.nextBillingDate(invoiceList, onOrAfter = migrationStartDate)
    assertEquals(billingDate, Right(LocalDate.of(2021, 1, 8)))
  }

  test("nextBillingDate: calculation fails if there are no invoices after migration start date") {
    val invoiceList = invoiceListFromJson("InvoicePreviewTermEndsBeforeMigration.json")
    val billingDate = AmendmentData.nextBillingDate(invoiceList, onOrAfter = migrationStartDate)
    assertEquals(
      billingDate.left.map(_.reason.take(79)),
      Left("Cannot determine next billing date on or after 2020-12-25 from ZuoraInvoiceList")
    )
  }

  test("priceData: calculation is correct for a monthly voucher subscription") {
    val priceData = AmendmentData.priceData(
      pricingData = productPricingMap(productCatalogueFromJson("Catalogue2.json")),
      subscription = subscriptionFromJson("Monthly2.json"),
      invoiceList = invoiceListFromJson("InvoicePreview2.json"),
      startDate = LocalDate.of(2020, 5, 28)
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 47.62, newPrice = 52.99, billingPeriod = "Month"))
    )
  }

  test("priceData: calculation is correct for a monthly discounted voucher subscription") {
    val priceData = AmendmentData.priceData(
      pricingData = productPricingMap(productCatalogueFromJson("Catalogue3.json")),
      subscription = subscriptionFromJson("MonthlyDiscounted3.json"),
      invoiceList = invoiceListFromJson("InvoicePreview3.json"),
      startDate = LocalDate.of(2020, 6, 15)
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 10.38, newPrice = 10.99, billingPeriod = "Month"))
    )
  }

  test("combinePrices: combines prices correctly") {
    val combinedPrice = AmendmentData.combinePrices(
      Seq(
        ZuoraPricing("GBP", Some(11), 0),
        ZuoraPricing("GBP", Some(10.99), 0)
      )
    )
    assertEquals(combinedPrice.toDouble, 21.99)
  }

  test("combinePrices: combines prices and discount correctly") {
    val combinedPrice = AmendmentData.combinePrices(
      Seq(
        ZuoraPricing("GBP", Some(11), 0),
        ZuoraPricing("GBP", Some(10.99), 0),
        ZuoraPricing("", None, 50)
      )
    )
    assertEquals(combinedPrice.toDouble, 10.99)
  }

  test("roundDown: rounds down to nearest hundredth of a currency unit") {
    assertEquals(AmendmentData.roundDown(10.995).toDouble, 10.99)
  }

  test("roundDown: ignores already rounded values") {
    assertEquals(AmendmentData.roundDown(10.1).toDouble, 10.1)
  }

  test("roundDown: rounds down if halfway between two values") {
    assertEquals(AmendmentData.roundDown(10.255).toDouble, 10.25)
  }
}
