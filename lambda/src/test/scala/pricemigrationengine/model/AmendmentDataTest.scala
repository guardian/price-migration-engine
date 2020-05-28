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
    val fixtureSet = "Monthly"
    val priceData = AmendmentData.priceData(
      pricingData = productPricingMap(productCatalogueFromJson(s"$fixtureSet/Catalogue.json")),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      startDate = LocalDate.of(2020, 5, 28)
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 47.62, newPrice = 52.99, billingPeriod = "Month"))
    )
  }

  test("priceData: calculation is correct for a monthly discounted voucher subscription") {
    val fixtureSet = "MonthlyDiscounted"
    val priceData = AmendmentData.priceData(
      pricingData = productPricingMap(productCatalogueFromJson(s"$fixtureSet/Catalogue.json")),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
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

  test("totalChargeAmount: is correct for a taxable product") {
    val fixtureSet = "Everyday+"
    val subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json")
    val invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json")
    val billingDate = LocalDate.of(2020, 6, 4)
    val totalChargeAmount = AmendmentData.totalChargeAmount(subscription, invoiceList, billingDate)
    assertEquals(totalChargeAmount, Right(BigDecimal(54.99)))
  }

  test("totalChargeAmount: is correct for a discounted taxable product") {
    val fixtureSet = "Everyday+Discounted"
    val subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json")
    val invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json")
    val billingDate = LocalDate.of(2020, 6, 9)
    val totalChargeAmount = AmendmentData.totalChargeAmount(subscription, invoiceList, billingDate)
    assertEquals(totalChargeAmount, Right(BigDecimal(25.98)))
  }

  test("individualChargeAmount: is correct for a product invoice item") {
    val chargeAmount = AmendmentData.individualChargeAmount(
      ZuoraRatePlanCharge(productRatePlanChargeId = "id", number = "C1", price = Some(4.34))
    )
    assertEquals(chargeAmount, Right(BigDecimal(4.34)))
  }

  test("individualChargeAmount: is correct for a percentage discount invoice item") {
    val chargeAmount = AmendmentData.individualChargeAmount(
      ZuoraRatePlanCharge(productRatePlanChargeId = "id", number = "C1", price = None, discountPercentage = Some(50.0))
    )
    assertEquals(chargeAmount, Left(50.0))
  }

  test("individualChargeAmount: ignores absolute discount invoice items") {
    val chargeAmount = AmendmentData.individualChargeAmount(
      ZuoraRatePlanCharge(productRatePlanChargeId = "id", number = "C1", price = Some(-3.42))
    )
    assertEquals(chargeAmount, Right(BigDecimal(0)))
  }
}
