package pricemigrationengine.model

import pricemigrationengine.Fixtures

import java.time.LocalDate
import pricemigrationengine.Fixtures._

class AmendmentDataTest extends munit.FunSuite {

  val importDate = LocalDate.of(2000, 1, 1) // Some old date, just to build the cohort Spec

  private def migrationStartDate = LocalDate.of(2020, 12, 25)

  test("nextserviceStartDate: billing date is first after migration start date") {
    val invoiceList = invoiceListFromJson("InvoicePreview.json")
    val subscription = subscriptionFromJson("NewspaperVoucher/Monthly/Subscription.json")
    val serviceStartDate = AmendmentData.nextServiceStartDate(invoiceList, subscription, onOrAfter = migrationStartDate)
    assertEquals(serviceStartDate, Right(LocalDate.of(2021, 1, 8)))
  }

  test("nextserviceStartDate: billing date is first after migration start date for a GW Zone A plan") {
    val invoiceList = invoiceListFromJson("GuardianWeekly/ZoneABC/ZoneA_USD_Domestic/InvoicePreview.json")
    val subscription = subscriptionFromJson("GuardianWeekly/ZoneABC/ZoneA_USD_Domestic/Subscription.json")
    val serviceStartDate = AmendmentData.nextServiceStartDate(invoiceList, subscription, onOrAfter = migrationStartDate)
    assertEquals(serviceStartDate, Right(LocalDate.of(2022, 10, 14)))
  }

  private def deliveryMigrationStartDate = LocalDate.of(2022, 4, 18)

  test("priceData: is correct for a quarterly GW Domestic plan") {
    val fixtureSet = "GuardianWeekly/QuarterlyDomestic"
    val priceData = AmendmentData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      migrationStartDate2022,
      CohortSpec("Cohort1", "Campaign1", importDate, migrationStartDate2022)
    )
    assertEquals(
      priceData,
      Right(
        AmendmentData(
          LocalDate.of(2022, 11, 12),
          PriceData(currency = "GBP", oldPrice = 37.50, newPrice = 41.25, billingPeriod = "Quarter")
        )
      )
    )
  }

  test("priceData: is correct for a GW ROW subscription with a past Zone ABC rate plan") {
    val fixtureSet = "GuardianWeekly/CappedPriceIncrease4"
    val priceData = AmendmentData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      migrationStartDate2022,
      CohortSpec("Cohort1", "Campaign1", importDate, migrationStartDate2022),
    )
    assertEquals(
      priceData,
      Right(
        AmendmentData(
          LocalDate.of(2023, 2, 11),
          PriceData(currency = "GBP", oldPrice = 60.00, newPrice = 74.40, billingPeriod = "Quarter")
        )
      )
    )
  }

  test("priceData: is correct migrating a quarterly GW Zone A plan (billed in USD) to GW Domestic plan") {
    val fixtureSet = "GuardianWeekly/ZoneABC/ZoneA_USD_Domestic"
    val priceData = AmendmentData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      migrationStartDate2022,
      CohortSpec("Cohort1", "Campaign1", importDate, migrationStartDate2022),
    )
    assertEquals(
      priceData,
      Right(
        AmendmentData(
          LocalDate.of(2022, 10, 14),
          PriceData(currency = "USD", oldPrice = 60.00, newPrice = 82.50, billingPeriod = "Quarter")
        )
      )
    )
  }

  private def migrationStartDate2022 = LocalDate.of(2022, 10, 10)

  test("priceData: is correct migrating an annual GW Zone B plan (billed in USD) to GW Rest Of World plan") {
    val fixtureSet = "GuardianWeekly/ZoneABC/ZoneB_USD_Annual"
    val priceData = AmendmentData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      migrationStartDate2022,
      CohortSpec("Cohort1", "Campaign1", importDate, migrationStartDate2022),
    )
    assertEquals(
      priceData,
      Right(
        AmendmentData(
          LocalDate.of(2023, 5, 26),
          PriceData(currency = "USD", oldPrice = 240.00, newPrice = 360.00, billingPeriod = "Annual")
        )
      )
    )
  }

  test("priceData: is correct migrating a quarterly GW Zone C plan (billed in USD) to GW Rest Of World plan") {
    val fixtureSet = "GuardianWeekly/ZoneABC/ZoneC_USD"
    val priceData = AmendmentData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      migrationStartDate2022,
      CohortSpec("Cohort1", "Campaign1", importDate, migrationStartDate2022)
    )
    assertEquals(
      priceData,
      Right(
        AmendmentData(
          LocalDate.of(2022, 10, 13),
          PriceData(currency = "USD", oldPrice = 65.0, newPrice = 90.0, billingPeriod = "Quarter")
        )
      )
    )
  }

  test("priceData: is correct migrating a quarterly GW Zone C plan to GW Domestic plan") {
    val fixtureSet = "GuardianWeekly/ZoneABC/ZoneC_GBP"
    val priceData = AmendmentData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      migrationStartDate2022,
      CohortSpec("Cohort1", "Campaign1", importDate, migrationStartDate2022)
    )
    assertEquals(
      priceData,
      Right(
        AmendmentData(
          LocalDate.of(2022, 11, 17),
          PriceData(currency = "GBP", oldPrice = 48.00, newPrice = 41.25, billingPeriod = "Quarter")
        )
      )
    )
  }

  test("nextserviceStartDate: billing date is first after migration start date (Everyday+Delivery)") {
    val invoiceList = invoiceListFromJson("NewspaperDelivery/Everyday+/InvoicePreview.json")
    val subscription = subscriptionFromJson("NewspaperDelivery/Everyday+/Subscription.json")
    val serviceStartDate =
      AmendmentData.nextServiceStartDate(invoiceList, subscription, onOrAfter = deliveryMigrationStartDate)
    assertEquals(serviceStartDate, Right(LocalDate.of(2022, 4, 19)))
  }

  test("nextserviceStartDate: calculation fails if there are no invoices after migration start date") {
    val invoiceList = invoiceListFromJson("InvoicePreviewTermEndsBeforeMigration.json")
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

  test("priceData: is correct for a quarterly GW subscription") {
    val fixtureSet = "QuarterlyGW"
    val priceData = AmendmentData.priceData(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      LocalDate.of(2020, 7, 28),
      CohortSpec("Cohort1", "Campaign1", importDate, LocalDate.of(2020, 7, 20))
    )
    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 37.50, newPrice = 42.40, billingPeriod = "Quarter"))
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

  test("priceData: is correct in the case of membership subscriptions") {
    val cohortSpec =
      CohortSpec("Membership2023_Batch1", "Campaign1", LocalDate.of(2000, 1, 1), LocalDate.of(2023, 5, 1))

    val account = Fixtures.accountFromJson("Membership2023/Batch1/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Membership2023/Batch1/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Membership2023/Batch1/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Membership2023/Batch1/invoice-preview.json")

    val priceData =
      AmendmentData.priceData(account, catalogue, subscription, invoicePreview, LocalDate.of(2023, 1, 26), cohortSpec)

    assertEquals(
      priceData,
      Right(PriceData(currency = "GBP", oldPrice = 5, newPrice = 7, billingPeriod = "Month"))
    )
  }

}
