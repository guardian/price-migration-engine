package pricemigrationengine.model

import java.time.LocalDate

import pricemigrationengine.model.ZuoraProductCatalogue.productPricingMap
import upickle.default._

import scala.io.Source

class AmendmentDataTest extends munit.FunSuite {

  private def migrationStartDate = LocalDate.of(2020, 12, 25)

  private def instanceFromJson[A: Reader](resource: String): A = {
    val json = Source.fromResource(resource).mkString
    read[A](json)
  }

  private def productCatalogueFromJson(resource: String): ZuoraProductCatalogue =
    instanceFromJson[ZuoraProductCatalogue](resource)

  private def subscriptionFromJson(resource: String): ZuoraSubscription =
    instanceFromJson[ZuoraSubscription](resource)

  private def invoiceListFromJson(resource: String): ZuoraInvoiceList =
    instanceFromJson[ZuoraInvoiceList](resource)

  test("billing date is first after migration start date") {
    val invoiceList = invoiceListFromJson("InvoicePreview.json")
    val billingDate = AmendmentData.nextBillingDate(invoiceList, after = migrationStartDate)
    assertEquals(billingDate, Right(LocalDate.of(2021, 1, 8)))
  }

  test("billing date calculation fails if there are no invoices after migration start date") {
    val invoiceList = invoiceListFromJson("InvoicePreviewTermEndsBeforeMigration.json")
    val billingDate = AmendmentData.nextBillingDate(invoiceList, after = migrationStartDate)
    assertEquals(
      billingDate.left.map(_.reason.take(73)),
      Left("Cannot determine next billing date after 2020-12-25 from ZuoraInvoiceList")
    )
  }

  test("price data calculation is correct for a monthly voucher subscription") {
    val priceData = AmendmentData.priceData(
      pricing = productPricingMap(productCatalogueFromJson("Catalogue2.json")),
      subscription = subscriptionFromJson("Monthly2.json"),
      invoiceList = invoiceListFromJson("InvoicePreview2.json"),
      startDate = LocalDate.of(2020, 5, 28)
    )
    assertEquals(priceData, Right(PriceData(currency = "GBP", oldPrice = 47.62, newPrice = 52.99)))
  }
}
