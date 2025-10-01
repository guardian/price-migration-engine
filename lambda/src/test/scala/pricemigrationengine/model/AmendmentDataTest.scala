package pricemigrationengine.model

import pricemigrationengine.Fixtures

import java.time.LocalDate
import pricemigrationengine.Fixtures._
import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation

class AmendmentDataTest extends munit.FunSuite {

  val importDate = LocalDate.of(2000, 1, 1) // Some old date, just to build the cohort Spec

  private def migrationStartDate = LocalDate.of(2021, 1, 25)

  test("nextserviceStartDate: billing date is first after migration start date") {
    val invoiceList = invoiceListFromJson("Core/AmendmentData/Misc/InvoicePreview.json")
    val subscription = subscriptionFromJson("Handlers/EstimationHandler/Monthly/Subscription.json")
    val serviceStartDate = AmendmentData.nextServiceStartDate(invoiceList, subscription, onOrAfter = migrationStartDate)
    assertEquals(serviceStartDate, Right(LocalDate.of(2021, 2, 8)))
  }

  private def deliveryMigrationStartDate = LocalDate.of(2022, 4, 18)

  private def migrationStartDate2022 = LocalDate.of(2022, 10, 10)

  test("nextserviceStartDate: billing date is first after migration start date (Everyday+Delivery)") {
    val invoiceList = invoiceListFromJson("Core/AmendmentData/Everyday+/InvoicePreview.json")
    val subscription = subscriptionFromJson("Core/AmendmentData/Everyday+/Subscription.json")
    val serviceStartDate =
      AmendmentData.nextServiceStartDate(invoiceList, subscription, onOrAfter = deliveryMigrationStartDate)
    assertEquals(serviceStartDate, Right(LocalDate.of(2022, 4, 19)))
  }

  test("nextserviceStartDate: calculation fails if there are no invoices after migration start date") {
    val invoiceList = invoiceListFromJson("Core/AmendmentData/Misc/InvoicePreviewTermEndsBeforeMigration.json")
    val subscription = subscriptionFromJson("Core/AmendmentData/Monthly/Subscription.json")
    val serviceStartDate =
      AmendmentData.nextServiceStartDate(invoiceList, subscription, onOrAfter = LocalDate.of(2021, 5, 25))
    assertEquals(
      serviceStartDate.left.map(_.reason.take(79)),
      Left("Cannot determine next billing date on or after 2021-05-25 from ZuoraInvoiceList")
    )
  }

  test("nextserviceStartDate: calculation fails if there are no invoices after migration start date (SundayDelivery)") {
    val invoiceList = invoiceListFromJson("Core/AmendmentData/Sunday/InvoicePreview.json")
    val subscription = subscriptionFromJson("Core/AmendmentData/Sunday/Subscription.json")
    val serviceStartDate =
      AmendmentData.nextServiceStartDate(invoiceList, subscription, onOrAfter = deliveryMigrationStartDate)
    assertEquals(
      serviceStartDate.left.map(_.reason.take(79)),
      Left("Cannot determine next billing date on or after 2022-04-18 from ZuoraInvoiceList")
    )
  }

  test("nextserviceStartDate: calculation fails if there are no invoices after migration start date (SixdayDelivery)") {
    val invoiceList = invoiceListFromJson("Core/AmendmentData/Sixday/InvoicePreview.json")
    val subscription = subscriptionFromJson("Core/AmendmentData/Sixday/Subscription.json")
    val serviceStartDate =
      AmendmentData.nextServiceStartDate(invoiceList, subscription, onOrAfter = deliveryMigrationStartDate)
    assertEquals(
      serviceStartDate.left.map(_.reason.take(79)),
      Left("Cannot determine next billing date on or after 2022-04-18 from ZuoraInvoiceList")
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

  test("totalChargeAmount: is correct for a discounted taxable product") {
    val fixtureSet = "Core/AmendmentData/Everyday+Discounted"
    val subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json")
    val invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json")
    val serviceStartDate = LocalDate.of(2020, 6, 9)
    val totalChargeAmount = AmendmentData.totalChargeAmount(subscription, invoiceList, serviceStartDate)
    assertEquals(totalChargeAmount, Right(BigDecimal(25.95)))
  }

  test("totalChargeAmount: is correct for a discounted non-taxable product") {
    val fixtureSet = "Core/AmendmentData/Discount25%"
    val subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json")
    val invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json")
    val serviceStartDate = LocalDate.of(2020, 7, 16)
    val totalChargeAmount = AmendmentData.totalChargeAmount(subscription, invoiceList, serviceStartDate)
    assertEquals(totalChargeAmount, Right(BigDecimal(15.57)))
  }

  test("totalChargeAmount: is correct for a discounted newspaper delivery taxable product (25%)") {
    val fixtureSet = "Core/AmendmentData/Waitrose25%Discount"
    val subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json")
    val invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json")
    val serviceStartDate = LocalDate.of(2022, 6, 26)
    val totalChargeAmount = AmendmentData.totalChargeAmount(subscription, invoiceList, serviceStartDate)
    assertEquals(totalChargeAmount, Right(BigDecimal(20.99)))
  }

  test("totalChargeAmount: is correct for pre-2020 newspaper delivery taxable product") {
    val fixtureSet = "Core/AmendmentData/Pre2020SixDay"
    val subscription = subscriptionFromJson(s"$fixtureSet/Subscription.json")
    val invoiceList = invoiceListFromJson(s"$fixtureSet/InvoicePreview.json")
    val serviceStartDate = LocalDate.of(2022, 4, 18)
    val totalChargeAmount = AmendmentData.totalChargeAmount(subscription, invoiceList, serviceStartDate)
    assertEquals(totalChargeAmount, Right(BigDecimal(54.12)))
  }

  test("totalChargeAmount: is correct where invoice preview has multiple subscriptions") {
    val fixtureSet = "Core/InvoicePreviewWithMultipleSubs"
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

  // ---------------------------------------------------------------------------
  // Date: 19 June 2025
  // Author: Pascal
  //
  // I am adding new tests to this, with fixtures located in `model/AmendmentData`
  // This is follow up of the tests in model/StartDatesTest, applied to the case of
  // subscription A-S02059070, with extra: {"earliestMigrationDate":"2026-03-19"}
  // (part of Guardian Weekly 2025)

  test("AmendmentData.nextServiceStartDate") {
    val subscription = Fixtures.subscriptionFromJson("model/AmendmentData/A-S02059070/subscription.json")
    val account = Fixtures.accountFromJson("model/AmendmentData/A-S02059070/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("model/AmendmentData/A-S02059070/invoice-preview.json")

    val cohortItem = CohortItem(
      "SUBSCRIPTION-NUMBER",
      ReadyForEstimation,
      migrationExtraAttributes = Some(""" {"earliestMigrationDate":"2026-03-19"} """)
    )
    val today = LocalDate.of(2025, 7, 1) // 1 July 2025
    val cohortSpec = CohortSpec(
      cohortName = "Test1",
      brazeName = "BrazeName",
      earliestAmendmentEffectDate = LocalDate.of(2025, 9, 10) // 10 Sept 2025
    )

    assertEquals(
      StartDates.startDateLowerBound(
        item = cohortItem,
        subscription = subscription,
        invoicePreview = invoicePreview,
        cohortSpec = cohortSpec,
        today = today
      ),
      LocalDate.of(2026, 3, 19)
    )
  }

  test("AmendmentData.nextServiceStartDate") {
    val subscription = Fixtures.subscriptionFromJson("model/AmendmentData/A-S02059070/subscription.json")
    val account = Fixtures.accountFromJson("model/AmendmentData/A-S02059070/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("model/AmendmentData/A-S02059070/invoice-preview.json")

    assertEquals(
      AmendmentData.nextServiceStartDate(
        invoiceList = invoicePreview,
        subscription = subscription,
        onOrAfter = LocalDate.of(2026, 3, 19)
      ),
      Right(LocalDate.of(2026, 3, 19))
    )

    assertEquals(
      AmendmentData.nextServiceStartDate(
        invoiceList = invoicePreview,
        subscription = subscription,
        onOrAfter = LocalDate.of(2026, 3, 20)
      ),
      Right(LocalDate.of(2026, 4, 19))
    )
  }
}
