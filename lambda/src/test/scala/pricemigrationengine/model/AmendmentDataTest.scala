package pricemigrationengine.model

import java.time.LocalDate

import pricemigrationengine.Fixtures._
import pricemigrationengine.model.ZuoraProductCatalogue.productPricingMap

class AmendmentDataTest extends munit.FunSuite {

  private def migrationStartDate = LocalDate.of(2020, 12, 25)

  test("nextserviceStartDate: billing date is first after migration start date") {
    val invoiceList = invoiceListFromJson("InvoicePreview.json")
    val subscription = subscriptionFromJson("Monthly/Subscription.json")
    val serviceStartDate = AmendmentData.nextServiceStartDate(invoiceList, subscription, onOrAfter = migrationStartDate)
    assertEquals(serviceStartDate, Right(LocalDate.of(2021, 1, 8)))
  }

  test("nextserviceStartDate: calculation fails if there are no invoices after migration start date") {
    val invoiceList = invoiceListFromJson("InvoicePreviewTermEndsBeforeMigration.json")
    val subscription = subscriptionFromJson("Monthly/Subscription.json")
    val serviceStartDate = AmendmentData.nextServiceStartDate(invoiceList, subscription, onOrAfter = migrationStartDate)
    assertEquals(
      serviceStartDate.left.map(_.reason.take(79)),
      Left("Cannot determine next billing date on or after 2020-12-25 from ZuoraInvoiceList")
    )
  }

  test("priceData: is correct for a monthly voucher subscription") {
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

  test("priceData: is correct for a monthly discounted voucher subscription") {
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

  test("priceData: is correct for a non-taxable 25% discounted voucher subscription") {
    val fixtureSet = "Discount25%"
    val priceData = AmendmentData.priceData(
      pricingData = productPricingMap(productCatalogueFromJson(s"$fixtureSet/Catalogue.json")),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      startDate = LocalDate.of(2020, 7, 16)
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 15.57, newPrice = 16.49, billingPeriod = "Month"))
    )
  }

  test("priceData: ignores holiday-stop credits") {
    val fixtureSet = "HolidayCredited"
    val priceData = AmendmentData.priceData(
      pricingData = productPricingMap(productCatalogueFromJson(s"$fixtureSet/Catalogue.json")),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      startDate = LocalDate.of(2020, 6, 4)
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 41.12, newPrice = 44.99, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct for a 25% discounted Everyday voucher subscription") {
    val fixtureSet = "EverydayDiscount25%"
    val priceData = AmendmentData.priceData(
      pricingData = productPricingMap(productCatalogueFromJson(s"$fixtureSet/Catalogue.json")),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      startDate = LocalDate.of(2020, 6, 4)
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 35.7, newPrice = 39.71, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct for a manually-set percentage discounted voucher subscription") {
    val fixtureSet = "PercentageDiscount"
    val priceData = AmendmentData.priceData(
      pricingData = productPricingMap(productCatalogueFromJson(s"$fixtureSet/Catalogue.json")),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      startDate = LocalDate.of(2020, 8, 8)
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 8.09, newPrice = 8.99, billingPeriod = "Month"))
    )
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
    val serviceStartDate = LocalDate.of(2020, 6, 4)
    val totalChargeAmount = AmendmentData.totalChargeAmount(subscription, invoiceList, serviceStartDate)
    assertEquals(totalChargeAmount, Right(BigDecimal(54.99)))
  }

  test("totalChargeAmount: is correct for a discounted taxable product") {
    val fixtureSet = "Everyday+Discounted"
    val subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json")
    val invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json")
    val serviceStartDate = LocalDate.of(2020, 6, 9)
    val totalChargeAmount = AmendmentData.totalChargeAmount(subscription, invoiceList, serviceStartDate)
    assertEquals(totalChargeAmount, Right(BigDecimal(25.95)))
  }

  test("totalChargeAmount: is correct for a discounted non-taxable product") {
    val fixtureSet = "Discount25%"
    val subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json")
    val invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json")
    val serviceStartDate = LocalDate.of(2020, 7, 16)
    val totalChargeAmount = AmendmentData.totalChargeAmount(subscription, invoiceList, serviceStartDate)
    assertEquals(totalChargeAmount, Right(BigDecimal(15.57)))
  }

  test("totalChargeAmount: is correct where invoice preview has multiple subscriptions") {
    val fixtureSet = "InvoicePreviewWithMultipleSubs"
    val totalChargeAmount = AmendmentData.totalChargeAmount(
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      serviceStartDate = LocalDate.of(2020, 8, 4)
    )
    assertEquals(
      totalChargeAmount,
      Right(BigDecimal(47.62))
    )
  }

  test("individualChargeAmount: is correct for a product invoice item") {
    val chargeAmount = AmendmentData.individualChargeAmount(
      ZuoraRatePlanCharge(productRatePlanChargeId = "id", number = "C1", currency = "GBP", price = Some(4.34))
    )
    assertEquals(chargeAmount, Right(BigDecimal(4.34)))
  }

  test("individualChargeAmount: is correct for a percentage discount invoice item") {
    val chargeAmount = AmendmentData.individualChargeAmount(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "id",
        number = "C1",
        currency = "GBP",
        price = None,
        discountPercentage = Some(50.0)
      )
    )
    assertEquals(chargeAmount, Left(50.0))
  }

  test("individualChargeAmount: ignores absolute discount invoice items") {
    val chargeAmount = AmendmentData.individualChargeAmount(
      ZuoraRatePlanCharge(productRatePlanChargeId = "id", number = "C1", currency = "GBP", price = Some(-3.42))
    )
    assertEquals(chargeAmount, Right(BigDecimal(0)))
  }
}
