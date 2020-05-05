package pricemigrationengine.model

import java.time.LocalDate

import upickle.default._

import scala.io.Source

class AmendmentDataTest extends munit.FunSuite {

  private def migrationStartDate = LocalDate.of(2020, 12, 25)

  private def invoiceListFromJson(resource: String): ZuoraInvoiceList = {
    val json = Source.fromResource(resource).mkString
    read[ZuoraInvoiceList](json)
  }

  test("billing date is first after migration start date") {
    val invoiceList = invoiceListFromJson("InvoicePreview.json")
    val billingDate = AmendmentData.nextBillingDate(invoiceList, after = migrationStartDate)
    assertEquals(billingDate, Right(LocalDate.of(2021, 1, 8)))
  }

  test("billing date calculation fails if there are no invoices after migration start date") {
    val invoiceList = invoiceListFromJson("InvoicePreviewTermEndsBeforeMigration.json")
    val billingDate = AmendmentData.nextBillingDate(invoiceList, after = migrationStartDate)
    assertEquals(billingDate.left.map(_.reason.take(73)),
                 Left("Cannot determine next billing date after 2020-12-25 from ZuoraInvoiceList"))
  }
}
