package pricemigrationengine.model

import pricemigrationengine.Fixtures

import java.time.LocalDate
import pricemigrationengine.Fixtures._

class AmendmentDataTest extends munit.FunSuite {

  val importDate = LocalDate.of(2000, 1, 1) // Some old date, just to build the cohort Spec

  private def migrationStartDate = LocalDate.of(2020, 12, 25)

  test("nextserviceStartDate: billing date is first after migration start date") {
    val invoiceList = invoiceListFromJson("NewspaperVoucher/InvoicePreview.json")
    val subscription = subscriptionFromJson("NewspaperVoucher/Monthly/Subscription.json")
    val serviceStartDate = AmendmentData.nextServiceStartDate(invoiceList, subscription, onOrAfter = migrationStartDate)
    assertEquals(serviceStartDate, Right(LocalDate.of(2021, 1, 8)))
  }

  private def deliveryMigrationStartDate = LocalDate.of(2022, 4, 18)

  private def migrationStartDate2022 = LocalDate.of(2022, 10, 10)

  test("nextserviceStartDate: billing date is first after migration start date (Everyday+Delivery)") {
    val invoiceList = invoiceListFromJson("NewspaperDelivery/Everyday+/InvoicePreview.json")
    val subscription = subscriptionFromJson("NewspaperDelivery/Everyday+/Subscription.json")
    val serviceStartDate =
      AmendmentData.nextServiceStartDate(invoiceList, subscription, onOrAfter = deliveryMigrationStartDate)
    assertEquals(serviceStartDate, Right(LocalDate.of(2022, 4, 19)))
  }

  test("nextserviceStartDate: calculation fails if there are no invoices after migration start date") {
    val invoiceList = invoiceListFromJson("NewspaperVoucher/InvoicePreviewTermEndsBeforeMigration.json")
    val subscription = subscriptionFromJson("NewspaperVoucher/Monthly/Subscription.json")
    val serviceStartDate = AmendmentData.nextServiceStartDate(invoiceList, subscription, onOrAfter = migrationStartDate)
    assertEquals(
      serviceStartDate.left.map(_.reason.take(79)),
      Left("Cannot determine next billing date on or after 2020-12-25 from ZuoraInvoiceList")
    )
  }

  test("nextserviceStartDate: calculation fails if there are no invoices after migration start date (SundayDelivery)") {
    val invoiceList = invoiceListFromJson("NewspaperDelivery/Sunday/InvoicePreview.json")
    val subscription = subscriptionFromJson("NewspaperDelivery/Sunday/Subscription.json")
    val serviceStartDate =
      AmendmentData.nextServiceStartDate(invoiceList, subscription, onOrAfter = deliveryMigrationStartDate)
    assertEquals(
      serviceStartDate.left.map(_.reason.take(79)),
      Left("Cannot determine next billing date on or after 2022-04-18 from ZuoraInvoiceList")
    )
  }

  test("nextserviceStartDate: calculation fails if there are no invoices after migration start date (SixdayDelivery)") {
    val invoiceList = invoiceListFromJson("NewspaperDelivery/Sixday/InvoicePreview.json")
    val subscription = subscriptionFromJson("NewspaperDelivery/Sixday/Subscription.json")
    val serviceStartDate =
      AmendmentData.nextServiceStartDate(invoiceList, subscription, onOrAfter = deliveryMigrationStartDate)
    assertEquals(
      serviceStartDate.left.map(_.reason.take(79)),
      Left("Cannot determine next billing date on or after 2022-04-18 from ZuoraInvoiceList")
    )
  }

  test("priceData: is correct for a monthly voucher subscription") {
    val fixtureSet = "NewspaperVoucher/Monthly"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2020, 5, 28),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2020, 5, 28))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 47.62, newPrice = 52.99, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct for a monthly discounted voucher subscription") {
    val fixtureSet = "NewspaperVoucher/MonthlyDiscounted"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2020, 6, 15),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2020, 6, 15))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 10.38, newPrice = 10.99, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct for a non-taxable 25% discounted voucher subscription") {
    val fixtureSet = "NewspaperVoucher/Discount25%"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2020, 7, 16),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2020, 7, 16))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 15.57, newPrice = 16.49, billingPeriod = "Month"))
    )
  }

  test("priceData: ignores holiday-stop credits") {
    val fixtureSet = "NewspaperVoucher/HolidayCredited"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2020, 6, 4),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2020, 6, 4))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 41.12, newPrice = 44.99, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct for a 25% discounted Everyday voucher subscription") {
    val fixtureSet = "NewspaperVoucher/EverydayDiscount25%"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2020, 6, 4),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2020, 6, 4))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 35.7, newPrice = 39.71, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct for a manually-set percentage discounted voucher subscription") {
    val fixtureSet = "NewspaperVoucher/PercentageDiscount"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2020, 8, 8),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2020, 8, 8))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 8.09, newPrice = 8.99, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct for a quarterly voucher subscription") {
    val fixtureSet = "NewspaperVoucher/QuarterlyVoucher"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2020, 7, 5),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2020, 7, 5))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 62.27, newPrice = 65.97, billingPeriod = "Quarter"))
    )
  }

  test("priceData: is correct for a semi-annual voucher subscription") {
    val fixtureSet = "NewspaperVoucher/SemiAnnualVoucher"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2021, 1, 13),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2021, 1, 13))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 220.74, newPrice = 269.94, billingPeriod = "Semi_Annual"))
    )
  }

  test("priceData: is correct for an annual voucher subscription") {
    val fixtureSet = "NewspaperVoucher/AnnualVoucher"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2020, 12, 7),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2020, 12, 7))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 249.08, newPrice = 263.88, billingPeriod = "Annual"))
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
    val fixtureSet = "NewspaperVoucher/Everyday+"
    val subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json")
    val invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json")
    val serviceStartDate = LocalDate.of(2020, 6, 4)
    val totalChargeAmount = AmendmentData.totalChargeAmount(subscription, invoiceList, serviceStartDate)
    assertEquals(totalChargeAmount, Right(BigDecimal(54.99)))
  }

  test("totalChargeAmount: is correct for a taxable product (delivery)") {
    val fixtureSet = "NewspaperDelivery/Everyday+"
    val subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json")
    val invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json")
    val serviceStartDate = LocalDate.of(2022, 2, 19)
    val totalChargeAmount = AmendmentData.totalChargeAmount(subscription, invoiceList, serviceStartDate)
    assertEquals(totalChargeAmount, Right(BigDecimal(69.99)))
  }

  test("totalChargeAmount: is correct for a discounted taxable product") {
    val fixtureSet = "NewspaperVoucher/Everyday+Discounted"
    val subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json")
    val invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json")
    val serviceStartDate = LocalDate.of(2020, 6, 9)
    val totalChargeAmount = AmendmentData.totalChargeAmount(subscription, invoiceList, serviceStartDate)
    assertEquals(totalChargeAmount, Right(BigDecimal(25.95)))
  }

  test("totalChargeAmount: is correct for a discounted non-taxable product") {
    val fixtureSet = "NewspaperVoucher/Discount25%"
    val subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json")
    val invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json")
    val serviceStartDate = LocalDate.of(2020, 7, 16)
    val totalChargeAmount = AmendmentData.totalChargeAmount(subscription, invoiceList, serviceStartDate)
    assertEquals(totalChargeAmount, Right(BigDecimal(15.57)))
  }

  test("totalChargeAmount: is correct for a discounted newspaper delivery taxable product (25%)") {
    val fixtureSet = "NewspaperDelivery/Waitrose25%Discount"
    val subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json")
    val invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json")
    val serviceStartDate = LocalDate.of(2022, 6, 26)
    val totalChargeAmount = AmendmentData.totalChargeAmount(subscription, invoiceList, serviceStartDate)
    assertEquals(totalChargeAmount, Right(BigDecimal(20.99)))
  }

  test("totalChargeAmount: is correct for pre-2020 newspaper delivery taxable product") {
    val fixtureSet = "NewspaperDelivery/Pre2020SixDay"
    val subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json")
    val invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json")
    val serviceStartDate = LocalDate.of(2022, 4, 18)
    val totalChargeAmount = AmendmentData.totalChargeAmount(subscription, invoiceList, serviceStartDate)
    assertEquals(totalChargeAmount, Right(BigDecimal(54.12)))
  }

  test("totalChargeAmount: is correct where invoice preview has multiple subscriptions") {
    val fixtureSet = "InvoicePreviewWithMultipleSubs"
    val totalChargeAmount = AmendmentData.totalChargeAmount(
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      serviceStartDate = LocalDate.of(2020, 8, 4),
    )
    assertEquals(
      totalChargeAmount,
      Right(BigDecimal(47.62))
    )
  }

  test("individualChargeAmount: is correct for a product invoice item") {
    val chargeAmount = AmendmentData.individualChargeAmount(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "id",
        name = "Weekend",
        number = "C1",
        currency = "GBP",
        price = Some(4.34)
      )
    )
    assertEquals(chargeAmount, Right(BigDecimal(4.34)))
  }

  test("individualChargeAmount: is correct for a percentage discount invoice item") {
    val chargeAmount = AmendmentData.individualChargeAmount(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "id",
        name = "Weekend",
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
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "id",
        name = "Weekend",
        number = "C1",
        currency = "GBP",
        price = Some(-3.42)
      )
    )
    assertEquals(chargeAmount, Right(BigDecimal(0)))
  }

  test("priceData: is correct for a newspaper delivery (everyday+)") {
    val fixtureSet = "NewspaperDelivery/Everyday+"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2022, 2, 19),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2022, 2, 19))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 69.99, newPrice = 71.99, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct for a newspaper delivery (everyday)") {
    val fixtureSet = "NewspaperDelivery/Everyday"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2022, 2, 11),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2022, 2, 11))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 67.99, newPrice = 69.99, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct for a newspaper delivery (weekend)") {
    val fixtureSet = "NewspaperDelivery/Weekend"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2022, 2, 28),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2022, 2, 28))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 26.99, newPrice = 27.99, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct for a newspaper delivery (sunday)") {
    val fixtureSet = "NewspaperDelivery/Sunday"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2022, 2, 28),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2022, 2, 28))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 16.99, newPrice = 17.99, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct for a newspaper delivery (sixday+)") {
    val fixtureSet = "NewspaperDelivery/Sixday+"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2022, 3, 15),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2022, 3, 15))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 60.99, newPrice = 62.99, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct for a newspaper delivery (sixday)") {
    val fixtureSet = "NewspaperDelivery/Sixday"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2022, 2, 19),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2022, 2, 19))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 57.99, newPrice = 59.99, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct for a newspaper delivery (saturday)") {
    val fixtureSet = "NewspaperDelivery/Saturday"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2022, 2, 8),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2022, 2, 8))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 16.99, newPrice = 17.99, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct for pre-2020 sixday newspaper delivery subscription (with past discounts)") {
    val fixtureSet = "NewspaperDelivery/Pre2020SixDay"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2022, 4, 18),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2022, 4, 18))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 54.12, newPrice = 59.99, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct for pre-2020 sixday newspaper delivery (with past discounts) 2") {
    val fixtureSet = "NewspaperDelivery/Pre2020Sixday2"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2022, 4, 5),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2022, 4, 5))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 54.12, newPrice = 59.99, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct for pre-2020 everyday newspaper delivery subscription") {
    val fixtureSet = "NewspaperDelivery/Pre2020Everyday"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2022, 6, 19),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2022, 6, 19))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 62.79, newPrice = 69.99, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct for a new (purchased after price rise) discounted newspaper delivery subscription") {
    val fixtureSet = "NewspaperDelivery/Waitrose25%Discount"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2023, 1, 26),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2023, 1, 26))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 20.99, newPrice = 20.99, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct in the case of membership subscriptions [GBP], Batch 1") {
    val cohortSpec =
      CohortSpec("Membership2023_Batch1", "Campaign1", LocalDate.of(2000, 1, 1), LocalDate.of(2023, 5, 1))

    val account = Fixtures.accountFromJson("Migrations/Membership2023/Batch1/GBP/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Migrations/Membership2023/Batch1/GBP/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2023/Batch1/GBP/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2023/Batch1/GBP/invoice-preview.json")

    val priceData =
      AmendmentData.priceData(account, catalogue, subscription, invoicePreview, LocalDate.of(2023, 1, 26), cohortSpec)

    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 5, newPrice = 7, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct in the case of membership subscriptions [AUD], Batch 1") {
    // Note: this tests exists to show correctness against a non GBP currency, but there actually won't be any such
    // currencies in Batch1
    val cohortSpec =
      CohortSpec("Membership2023_Batch1", "Campaign1", LocalDate.of(2000, 1, 1), LocalDate.of(2023, 5, 1))

    val account = Fixtures.accountFromJson("Migrations/Membership2023/Batch1/AUD/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Migrations/Membership2023/Batch1/AUD/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2023/Batch1/AUD/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2023/Batch1/AUD/invoice-preview.json")

    val priceData =
      AmendmentData.priceData(account, catalogue, subscription, invoicePreview, LocalDate.of(2023, 1, 26), cohortSpec)

    assertEquals(
      priceData,
      Right(PriceData(currency = "AUD", oldPrice = 10, newPrice = 14.99, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct in the case of membership subscriptions [GBP], Batch2") {
    val cohortSpec =
      CohortSpec("Membership2023_Batch2", "Campaign1", LocalDate.of(2000, 1, 1), LocalDate.of(2023, 6, 1))

    val account = Fixtures.accountFromJson("Migrations/Membership2023/Batch1/GBP/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Migrations/Membership2023/Batch1/GBP/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2023/Batch1/GBP/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2023/Batch1/GBP/invoice-preview.json")

    val priceData =
      AmendmentData.priceData(account, catalogue, subscription, invoicePreview, LocalDate.of(2023, 1, 26), cohortSpec)

    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 5, newPrice = 7, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct in the case of membership subscriptions [AUD], Batch2") {
    val cohortSpec =
      CohortSpec("Membership2023_Batch2", "Campaign1", LocalDate.of(2000, 1, 1), LocalDate.of(2023, 6, 1))

    val account = Fixtures.accountFromJson("Migrations/Membership2023/Batch1/AUD/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Migrations/Membership2023/Batch1/AUD/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2023/Batch1/AUD/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2023/Batch1/AUD/invoice-preview.json")

    val priceData =
      AmendmentData.priceData(account, catalogue, subscription, invoicePreview, LocalDate.of(2023, 1, 26), cohortSpec)

    assertEquals(
      priceData,
      Right(PriceData(currency = "AUD", oldPrice = 10, newPrice = 14.99, billingPeriod = "Month"))
    )
  }

  test("priceData: is correct in the case of membership subscriptions [GBP], Batch3") {
    val cohortSpec =
      CohortSpec("Membership2023_Batch3", "Campaign1", LocalDate.of(2000, 1, 1), LocalDate.of(2023, 7, 1))

    val account = Fixtures.accountFromJson("Migrations/Membership2023/Batch3/GBP/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Migrations/Membership2023/Batch3/GBP/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2023/Batch3/GBP/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2023/Batch3/GBP/invoice-preview.json")

    val priceData =
      AmendmentData.priceData(account, catalogue, subscription, invoicePreview, LocalDate.of(2024, 1, 20), cohortSpec)

    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 49, newPrice = 75, billingPeriod = "Annual"))
    )
  }

  test("priceData: is correct in the case of membership subscriptions [USD], Batch3") {
    val cohortSpec =
      CohortSpec("Membership2023_Batch3", "Campaign1", LocalDate.of(2000, 1, 1), LocalDate.of(2023, 7, 1))

    val account = Fixtures.accountFromJson("Migrations/Membership2023/Batch3/USD/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Migrations/Membership2023/Batch3/USD/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2023/Batch3/USD/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2023/Batch3/USD/invoice-preview.json")

    val priceData =
      AmendmentData.priceData(account, catalogue, subscription, invoicePreview, LocalDate.of(2024, 1, 20), cohortSpec)

    assertEquals(
      priceData,
      Right(PriceData(currency = "USD", oldPrice = 69, newPrice = 120, billingPeriod = "Annual"))
    )
  }

  test("billing period is correct for SupporterPlus2023V1V2 (monthly standard)") {
    val cohortSpec =
      CohortSpec("SupporterPlus2023V1V2", "Campaign1", LocalDate.of(2023, 7, 14), LocalDate.of(2023, 8, 21))

    val account = Fixtures.accountFromJson("Migrations/SupporterPlus2023V1V2/monthly-standard/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Migrations/SupporterPlus2023V1V2/monthly-standard/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2023V1V2/monthly-standard/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2023V1V2/monthly-standard/invoice-preview.json")

    val nextServiceStartDate = LocalDate.of(2023, 8, 1)

    val invoiceItems = ZuoraInvoiceItem.items(invoicePreview, subscription, nextServiceStartDate)
    assertEquals(
      invoiceItems,
      List(
        ZuoraInvoiceItem(
          subscriptionNumber = "SUBSCRIPTION-NUMBER",
          serviceStartDate = LocalDate.of(2023, 8, 1),
          chargeNumber = "C-04240692",
          productName = "Supporter Plus"
        )
      )
    )

    val rpcof = SupporterPlus2023V1V2Migration.ratePlanChargesOrFail(subscription, invoiceItems)
    assertEquals(
      rpcof,
      Right(
        List(
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "8a12865b8219d9b401822106194e64e3", // Supporter Plus Monthly Charge (GBP)
            name = "Supporter Plus Monthly Charge",
            number = "C-04240692",
            currency = "GBP",
            price = Some(10.0),
            billingPeriod = Some("Month"),
            chargedThroughDate = Some(LocalDate.of(2023, 8, 1)),
            processedThroughDate = Some(LocalDate.of(2023, 7, 1)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2023, 4, 1))
          )
        )
      )
    )

    val billingPeriod =
      SupporterPlus2023V1V2Migration.billingPeriod(
        account,
        catalogue,
        subscription,
        invoicePreview,
        LocalDate.of(2023, 8, 1)
      )

    assertEquals(
      billingPeriod,
      Right("Month")
    )
  }

  test("billing period is correct for SupporterPlus2023V1V2 (monthly contribution)") {
    val cohortSpec =
      CohortSpec("SupporterPlus2023V1V2", "Campaign1", LocalDate.of(2023, 7, 14), LocalDate.of(2023, 8, 21))

    val account = Fixtures.accountFromJson("Migrations/SupporterPlus2023V1V2/monthly-contribution/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Migrations/SupporterPlus2023V1V2/monthly-contribution/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2023V1V2/monthly-contribution/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2023V1V2/monthly-contribution/invoice-preview.json")

    val nextServiceStartDate = LocalDate.of(2023, 8, 3)

    val invoiceItems = ZuoraInvoiceItem.items(invoicePreview, subscription, nextServiceStartDate)
    assertEquals(
      invoiceItems,
      List(
        ZuoraInvoiceItem(
          subscriptionNumber = "SUBSCRIPTION-NUMBER",
          serviceStartDate = LocalDate.of(2023, 8, 3),
          chargeNumber = "C-04419773",
          productName = "Supporter Plus"
        )
      )
    )

    val rpcof = SupporterPlus2023V1V2Migration.ratePlanChargesOrFail(subscription, invoiceItems)
    assertEquals(
      rpcof,
      Right(
        List(
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "8a12865b8219d9b401822106194e64e3", // Supporter Plus Monthly Charge (USD)
            name = "Supporter Plus Monthly Charge",
            number = "C-04419773",
            currency = "GBP",
            price = Some(25.0),
            billingPeriod = Some("Month"),
            chargedThroughDate = Some(LocalDate.of(2023, 8, 3)),
            processedThroughDate = Some(LocalDate.of(2023, 7, 3)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2023, 7, 3))
          )
        )
      )
    )

    val billingPeriod =
      SupporterPlus2023V1V2Migration.billingPeriod(
        account,
        catalogue,
        subscription,
        invoicePreview,
        LocalDate.of(2023, 8, 3)
      )

    assertEquals(
      billingPeriod,
      Right("Month")
    )
  }

  test("billing period is correct for SupporterPlus2023V1V2 (annual standard)") {
    val cohortSpec =
      CohortSpec("SupporterPlus2023V1V2", "Campaign1", LocalDate.of(2023, 7, 14), LocalDate.of(2023, 8, 21))

    val account = Fixtures.accountFromJson("Migrations/SupporterPlus2023V1V2/annual-standard/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Migrations/SupporterPlus2023V1V2/annual-standard/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2023V1V2/annual-standard/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2023V1V2/annual-standard/invoice-preview.json")

    val nextServiceStartDate = LocalDate.of(2024, 7, 2)

    val invoiceItems = ZuoraInvoiceItem.items(invoicePreview, subscription, nextServiceStartDate)
    assertEquals(
      invoiceItems,
      List(
        ZuoraInvoiceItem(
          subscriptionNumber = "SUBSCRIPTION-NUMBER",
          serviceStartDate = LocalDate.of(2024, 7, 2),
          chargeNumber = "C-04417974",
          productName = "Supporter Plus"
        )
      )
    )

    val rpcof = SupporterPlus2023V1V2Migration.ratePlanChargesOrFail(subscription, invoiceItems)
    assertEquals(
      rpcof,
      Right(
        List(
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "8a12865b8219d9b40182210618c664c1", // Supporter Plus Annual Charge (USD)
            name = "Supporter Plus Annual Charge",
            number = "C-04417974",
            currency = "USD",
            price = Some(120.0),
            billingPeriod = Some("Annual"),
            chargedThroughDate = Some(LocalDate.of(2024, 7, 2)),
            processedThroughDate = Some(LocalDate.of(2023, 7, 2)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2023, 7, 2))
          )
        )
      )
    )

    val billingPeriod =
      SupporterPlus2023V1V2Migration.billingPeriod(
        account,
        catalogue,
        subscription,
        invoicePreview,
        LocalDate.of(2024, 7, 2)
      )

    assertEquals(
      billingPeriod,
      Right("Annual")
    )
  }

  test("priceData: is correct for SupporterPlus2023V1V2 (monthly standard)") {

    val cohortSpec =
      CohortSpec("SupporterPlus2023V1V2", "Campaign1", LocalDate.of(2023, 7, 14), LocalDate.of(2023, 8, 21))

    val account = Fixtures.accountFromJson("Migrations/SupporterPlus2023V1V2/monthly-standard/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Migrations/SupporterPlus2023V1V2/monthly-standard/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2023V1V2/monthly-standard/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2023V1V2/monthly-standard/invoice-preview.json")

    // We are going to use this test to make the computation of the price data for supporter plus explicit

    // We are using this `.toOption.get` technique to go through the original for construct step by step
    // By doing so we could get a runtime error while running the tests instead of a test framework error.
    // In either case it's an error :)

    val ratePlan = SupporterPlus2023V1V2Migration.subscriptionRatePlan(subscription: ZuoraSubscription).toOption.get
    assertEquals(
      ratePlan,
      ZuoraRatePlan(
        id = "8a129d388962fe000189679374ce42a1",
        productName = "Supporter Plus",
        productRatePlanId = "8a12865b8219d9b401822106192b64dc",
        ratePlanName = "Supporter Plus Monthly",
        List(
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "8a12865b8219d9b401822106194e64e3",
            name = "Supporter Plus Monthly Charge",
            number = "C-04240692",
            currency = "GBP",
            price = Some(10.0),
            billingPeriod = Some("Month"),
            chargedThroughDate = Some(LocalDate.of(2023, 8, 1)),
            processedThroughDate = Some(LocalDate.of(2023, 7, 1)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2023, 4, 1))
          )
        ),
        lastChangeType = Some("Add")
      )
    )

    val ratePlanCharges =
      SupporterPlus2023V1V2Migration.subscriptionRatePlanCharges(subscription, ratePlan).toOption.get
    assertEquals(
      ratePlanCharges,
      List(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "8a12865b8219d9b401822106194e64e3",
          name = "Supporter Plus Monthly Charge",
          number = "C-04240692",
          currency = "GBP",
          price = Some(10.0),
          billingPeriod = Some("Month"),
          chargedThroughDate = Some(LocalDate.of(2023, 8, 1)),
          processedThroughDate = Some(LocalDate.of(2023, 7, 1)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2023, 4, 1))
        )
      )
    )

    val currency = ratePlanCharges.head.currency
    assertEquals(
      currency,
      "GBP"
    )

    val oldPrice = SupporterPlus2023V1V2Migration.getOldPrice(subscription, ratePlanCharges).toOption.get
    assertEquals(
      oldPrice,
      BigDecimal(10)
    )

    val billingP =
      SupporterPlus2023V1V2Migration
        .billingPeriod(account, catalogue, subscription, invoicePreview, LocalDate.of(2024, 4, 1))
        .toOption
        .get
    assertEquals(
      billingP,
      "Month"
    )

    val newPrice = SupporterPlus2023V1V2Migration.currencyToNewPrice(billingP, currency).toOption.get
    assertEquals(
      newPrice,
      BigDecimal(10)
    )

    val priceData =
      AmendmentData
        .priceData(account, catalogue, subscription, invoicePreview, LocalDate.of(2024, 4, 1), cohortSpec)
        .toOption
        .get
    assertEquals(
      priceData,
      PriceData(currency, 10, 10, "Month")
    )
  }

  test("priceData: is correct for SupporterPlus2023V1V2 (monthly contribution)") {

    val cohortSpec =
      CohortSpec("SupporterPlus2023V1V2", "Campaign1", LocalDate.of(2023, 7, 14), LocalDate.of(2023, 8, 21))

    val account = Fixtures.accountFromJson("Migrations/SupporterPlus2023V1V2/monthly-contribution/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Migrations/SupporterPlus2023V1V2/monthly-contribution/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2023V1V2/monthly-contribution/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2023V1V2/monthly-contribution/invoice-preview.json")

    // We are going to use this test to make the computation of the price data for supporter plus explicit

    // We are using this `.toOption.get` technique to go through the original for construct step by step
    // By doing so we could get a runtime error while running the tests instead of a test framework error.
    // In either case it's an error :)

    val ratePlan = SupporterPlus2023V1V2Migration.subscriptionRatePlan(subscription: ZuoraSubscription).toOption.get
    assertEquals(
      ratePlan,
      ZuoraRatePlan(
        id = "8a12921d89018aaa01891bef52021b65",
        productName = "Supporter Plus",
        productRatePlanId = "8a12865b8219d9b401822106192b64dc",
        ratePlanName = "Supporter Plus Monthly",
        List(
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "8a12865b8219d9b401822106194e64e3",
            name = "Supporter Plus Monthly Charge",
            number = "C-04419773",
            currency = "GBP",
            price = Some(25.0),
            billingPeriod = Some("Month"),
            chargedThroughDate = Some(LocalDate.of(2023, 8, 3)),
            processedThroughDate = Some(LocalDate.of(2023, 7, 3)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2023, 7, 3))
          )
        ),
        lastChangeType = None
      )
    )

    val ratePlanCharges =
      SupporterPlus2023V1V2Migration.subscriptionRatePlanCharges(subscription, ratePlan).toOption.get
    assertEquals(
      ratePlanCharges,
      List(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "8a12865b8219d9b401822106194e64e3",
          name = "Supporter Plus Monthly Charge",
          number = "C-04419773",
          currency = "GBP",
          price = Some(25.0),
          billingPeriod = Some("Month"),
          chargedThroughDate = Some(LocalDate.of(2023, 8, 3)),
          processedThroughDate = Some(LocalDate.of(2023, 7, 3)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2023, 7, 3))
        )
      )
    )

    val currency = ratePlanCharges.head.currency
    assertEquals(
      currency,
      "GBP"
    )

    val oldPrice = SupporterPlus2023V1V2Migration.getOldPrice(subscription, ratePlanCharges).toOption.get
    assertEquals(
      oldPrice,
      BigDecimal(25.0)
    )

    val billingP =
      SupporterPlus2023V1V2Migration
        .billingPeriod(account, catalogue, subscription, invoicePreview, LocalDate.of(2023, 8, 3))
        .toOption
        .get
    assertEquals(
      billingP,
      "Month"
    )

    val newPrice = SupporterPlus2023V1V2Migration.currencyToNewPrice(billingP, currency).toOption.get
    assertEquals(
      newPrice,
      BigDecimal(10)
    )

    val priceData =
      AmendmentData
        .priceData(account, catalogue, subscription, invoicePreview, LocalDate.of(2023, 8, 3), cohortSpec)
        .toOption
        .get
    assertEquals(
      priceData,
      PriceData(currency, 25, 10, "Month")
    )
  }

  test("priceData: is correct for SupporterPlus2023V1V2 (annual standard)") {

    val cohortSpec =
      CohortSpec("SupporterPlus2023V1V2", "Campaign1", LocalDate.of(2023, 7, 14), LocalDate.of(2023, 8, 21))

    val account = Fixtures.accountFromJson("Migrations/SupporterPlus2023V1V2/annual-standard/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Migrations/SupporterPlus2023V1V2/annual-standard/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2023V1V2/annual-standard/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2023V1V2/annual-standard/invoice-preview.json")

    // We are going to use this test to make the computation of the price data for supporter plus explicit

    // We are using this `.toOption.get` technique to go through the original for construct step by step
    // By doing so we could get a runtime error while running the tests instead of a test framework error.
    // In either case it's an error :)

    val ratePlan = SupporterPlus2023V1V2Migration.subscriptionRatePlan(subscription: ZuoraSubscription).toOption.get
    assertEquals(
      ratePlan,
      ZuoraRatePlan(
        "8a128432890171d1018914866bee0e7f",
        "Supporter Plus",
        "8a12865b8219d9b40182210618a464ba",
        "Supporter Plus Annual",
        List(
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "8a12865b8219d9b40182210618c664c1",
            name = "Supporter Plus Annual Charge",
            number = "C-04417974",
            currency = "USD",
            price = Some(120.0),
            billingPeriod = Some("Annual"),
            chargedThroughDate = Some(LocalDate.of(2024, 7, 2)),
            processedThroughDate = Some(LocalDate.of(2023, 7, 2)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2023, 7, 2))
          )
        ),
        None
      )
    )

    val ratePlanCharges =
      SupporterPlus2023V1V2Migration.subscriptionRatePlanCharges(subscription, ratePlan).toOption.get
    assertEquals(
      ratePlanCharges,
      List(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "8a12865b8219d9b40182210618c664c1",
          name = "Supporter Plus Annual Charge",
          number = "C-04417974",
          currency = "USD",
          price = Some(120.0),
          billingPeriod = Some("Annual"),
          chargedThroughDate = Some(LocalDate.of(2024, 7, 2)),
          processedThroughDate = Some(LocalDate.of(2023, 7, 2)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2023, 7, 2))
        )
      )
    )

    val currency = ratePlanCharges.head.currency
    assertEquals(
      currency,
      "USD"
    )

    val oldPrice = SupporterPlus2023V1V2Migration.getOldPrice(subscription, ratePlanCharges).toOption.get
    assertEquals(
      oldPrice,
      BigDecimal(120)
    )

    val billingP =
      SupporterPlus2023V1V2Migration
        .billingPeriod(account, catalogue, subscription, invoicePreview, LocalDate.of(2024, 7, 2))
        .toOption
        .get
    assertEquals(
      billingP,
      "Annual"
    )

    val newPrice = SupporterPlus2023V1V2Migration.currencyToNewPrice(billingP, currency).toOption.get
    assertEquals(
      newPrice,
      BigDecimal(120)
    )

    val priceData =
      AmendmentData
        .priceData(account, catalogue, subscription, invoicePreview, LocalDate.of(2024, 7, 2), cohortSpec)
        .toOption
        .get
    assertEquals(
      priceData,
      PriceData(currency, 120, 120, "Annual")
    )
  }

  test("priceData: is correct for SupporterPlus2023V1V2 (annual contribution)") {

    val cohortSpec =
      CohortSpec("SupporterPlus2023V1V2", "Campaign1", LocalDate.of(2023, 7, 14), LocalDate.of(2023, 8, 21))

    val account = Fixtures.accountFromJson("Migrations/SupporterPlus2023V1V2/annual-contribution/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Migrations/SupporterPlus2023V1V2/annual-contribution/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2023V1V2/annual-contribution/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2023V1V2/annual-contribution/invoice-preview.json")

    // We are going to use this test to make the computation of the price data for supporter plus explicit

    // We are using this `.toOption.get` technique to go through the original for construct step by step
    // By doing so we could get a runtime error while running the tests instead of a test framework error.
    // In either case it's an error :)

    val ratePlan = SupporterPlus2023V1V2Migration.subscriptionRatePlan(subscription: ZuoraSubscription).toOption.get
    assertEquals(
      ratePlan,
      ZuoraRatePlan(
        id = "8a12843288f6ded10188ff5fbef67bb3",
        productName = "Supporter Plus",
        productRatePlanId = "8a12865b8219d9b40182210618a464ba",
        ratePlanName = "Supporter Plus Annual",
        List(
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "8a12865b8219d9b40182210618c664c1",
            name = "Supporter Plus Annual Charge",
            number = "C-04411538",
            currency = "GBP",
            price = Some(120.0),
            billingPeriod = Some("Annual"),
            chargedThroughDate = Some(LocalDate.of(2024, 6, 28)),
            processedThroughDate = Some(LocalDate.of(2023, 6, 28)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2023, 6, 28))
          )
        ),
        None
      )
    )

    val ratePlanCharges =
      SupporterPlus2023V1V2Migration.subscriptionRatePlanCharges(subscription, ratePlan).toOption.get
    assertEquals(
      ratePlanCharges,
      List(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "8a12865b8219d9b40182210618c664c1",
          name = "Supporter Plus Annual Charge",
          number = "C-04411538",
          currency = "GBP",
          price = Some(120.0),
          billingPeriod = Some("Annual"),
          chargedThroughDate = Some(LocalDate.of(2024, 6, 28)),
          processedThroughDate = Some(LocalDate.of(2023, 6, 28)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2023, 6, 28))
        )
      )
    )

    val currency = ratePlanCharges.head.currency
    assertEquals(
      currency,
      "GBP"
    )

    val oldPrice = SupporterPlus2023V1V2Migration.getOldPrice(subscription, ratePlanCharges).toOption.get
    assertEquals(
      oldPrice,
      BigDecimal(120)
    )

    val billingP =
      SupporterPlus2023V1V2Migration
        .billingPeriod(account, catalogue, subscription, invoicePreview, LocalDate.of(2024, 6, 28))
        .toOption
        .get
    assertEquals(
      billingP,
      "Annual"
    )

    val newPrice = SupporterPlus2023V1V2Migration.currencyToNewPrice(billingP, currency).toOption.get
    assertEquals(
      newPrice,
      BigDecimal(95)
    )

    val priceData =
      AmendmentData
        .priceData(account, catalogue, subscription, invoicePreview, LocalDate.of(2024, 6, 28), cohortSpec)
        .toOption
        .get
    assertEquals(
      priceData,
      PriceData(currency, 120, 95, "Annual")
    )
  }
}
