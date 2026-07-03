package pricemigrationengine.migrations

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.Fixtures

import java.time.LocalDate

class SupporterPlus2026Test extends munit.FunSuite {
  test("date calculation") {
    assertEquals(
      SupporterPlus2026Migration.monthliesOverSixWeeks(LocalDate.of(2026, 7, 20), Annual),
      LocalDate.of(2026, 7, 20)
    )
    assertEquals(
      SupporterPlus2026Migration.monthliesOverSixWeeks(LocalDate.of(2026, 9, 1), Monthly),
      LocalDate.of(2026, 9, 1)
    )
    assert(
      List(LocalDate.of(2026, 7, 21), LocalDate.of(2026, 8, 21))
        .contains(SupporterPlus2026Migration.monthliesOverSixWeeks(LocalDate.of(2026, 7, 21), Monthly))
    )
  }

  test("price data [01]") {
    // 01: Monthly USD
    // Acquired in 30 Jun 2026, used to test the basic 1 year policy.

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/01/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/01/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/01/invoice-preview.json")

    assertEquals(
      SupporterPlus2026Migration.priceData(subscription, invoicePreview),
      Right(PriceData("USD", BigDecimal(15.0), BigDecimal(18.0), "Month"))
    )
  }

  test("1 year policy [01]") {
    // 01:
    // Monthly USD
    // Acquired in 30 Jun 2026, used to test the basic 1 year policy.

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/01/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/01/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/01/invoice-preview.json")

    val cohortItem = CohortItem(
      subscriptionName = subscription.subscriptionNumber,
      processingStage = CohortTableFilter.ReadyForEstimation,
    )

    val cohortSpec = CohortSpec(
      "SupporterPlus2026",
      "",
      LocalDate.of(2026, 9, 19),
    )

    val today = LocalDate.of(2026, 7, 2)

    assertEquals(
      AmendmentEffectiveDateCalculator.amendmentEffectiveDateLowerBound(
        cohortItem: CohortItem,
        subscription: ZuoraSubscription,
        invoicePreview: ZuoraInvoiceList,
        cohortSpec: CohortSpec,
        today: LocalDate
      ),
      LocalDate.of(2027, 6, 30)
    )

    // Here we observe that the 1 year policy is applied as AmendmentEffectiveDateCalculator.amendmentEffectiveDateLowerBound
    // returns the date a year after the acquisition date.
  }

  test("get active discounts") {
    /*
      05:

      This one is a manually modified variant of 05. We shifted the discount dates to end later.
      We expect the price rise date to not be the next billing date, but the one after

      Annually EUR
      Acquired on 26 Nov 2016
      Got that one to test the one year discount policy.

      The discount has dates
        "effectiveStartDate": "2024-12-30"
        "effectiveEndDate": "2025-12-30"

      The supporter plus rate plan has dates
        "effectiveStartDate": "2024-12-20"
        "effectiveEndDate": "2026-12-20"
     */

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/05/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/05/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/05/invoice-preview.json")

    assertEquals(
      SI2025Extractions.getActiveDiscountsPossiblyAfterEffectiveEndDate(subscription),
      List(
        ZuoraRatePlan(
          id = "8a128f399b353104019b39fc32a763dc",
          productName = "Discounts",
          productRatePlanId = "8a128adf8b64bcfd018b6b6fdc7674f5",
          ratePlanName = "Cancellation Save Discount - 25% off for 12 months",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "8a129c288b64d798018b6b711d8845d0",
              name = "Cancellation Save Discount - 25% off for 12 months",
              number = "C-06219793",
              currency = "EUR",
              price = None,
              billingPeriod = Some("Month"),
              chargedThroughDate = Some(LocalDate.of(2025, 1, 20)),
              processedThroughDate = Some(LocalDate.of(2024, 12, 30)),
              specificBillingPeriod = None,
              endDateCondition = Some("Fixed_Period"),
              upToPeriodsType = Some("Months"),
              upToPeriods = Some(12),
              billingDay = Some("ChargeTriggerDay"),
              triggerEvent = Some("CustomerAcceptance"),
              triggerDate = None,
              discountPercentage = Some(25.0),
              originalOrderDate = Some(LocalDate.of(2024, 12, 15)),
              effectiveStartDate = Some(LocalDate.of(2024, 12, 30)),
              effectiveEndDate = Some(LocalDate.of(2025, 12, 30))
            )
          ),
          lastChangeType = Some("Add")
        )
      )
    )
  }

  test("SupporterPlus2026Migration.annualWithDiscountOneYearPolicy") {
    /*
      05:

      This one is a manually modified variant of 05. We shifted the discount dates to end later.
      We expect the price rise date to not be the next billing date, but the one after

      Annually EUR
      Acquired on 26 Nov 2016
      Got that one to test the one year discount policy.

      The discount has dates
        "effectiveStartDate": "2024-12-30"
        "effectiveEndDate": "2025-12-30"

      The supporter plus rate plan has dates
        "effectiveStartDate": "2024-12-20"
        "effectiveEndDate": "2026-12-20"
     */

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/05/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/05/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/05/invoice-preview.json")

    assertEquals(
      SupporterPlus2026Migration.annualWithDiscountOneYearPolicy(LocalDate.of(2026, 7, 2), subscription),
      LocalDate.of(2026, 12, 30)
    )

    // Here we observe that we returned the max effective discount end date plus one year
  }

  test("1 year policy [02]") {
    // 02:
    // Monthly, GBP
    // Acquired in 2 Aug 2017, used to test the basic 1 year policy (in this case it is irrelevant)

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/02/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/02/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/02/invoice-preview.json")

    val cohortItem = CohortItem(
      subscriptionName = subscription.subscriptionNumber,
      processingStage = CohortTableFilter.ReadyForEstimation,
      billingPeriod = Some("Month")
    )

    val cohortSpec = CohortSpec(
      "SupporterPlus2026",
      "",
      LocalDate.of(2026, 9, 19),
    )

    val today = LocalDate.of(2026, 7, 2)

    assertEquals(
      AmendmentEffectiveDateCalculator.amendmentEffectiveDateLowerBound(
        cohortItem: CohortItem,
        subscription: ZuoraSubscription,
        invoicePreview: ZuoraInvoiceList,
        cohortSpec: CohortSpec,
        today: LocalDate
      ),
      LocalDate.of(2026, 9, 19)
    )

    // Here the subscription is ready to be price risen as soon as possible. The lowerbound
    // is equal to the CohortSpec's earliest amendment effective date.
  }

  test("1 year policy [03]") {

    /*
      03:

      Annually EUR
      Acquired on 26 Nov 2016
      Got that one to test the one year discount policy.

      The discount has dates
        "effectiveStartDate": "2024-12-20"
        "effectiveEndDate": "2025-12-20"

      The supporter plus rate plan has dates
        "effectiveStartDate": "2024-12-20"
        "effectiveEndDate": "2026-12-20"

      The discount ended less than a year ago, but one year after the end is the price rise date
      This is a edge case and we expect the price rise to happen on 2026-12-20
     */

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/03/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/03/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/03/invoice-preview.json")

    val cohortItem = CohortItem(
      subscriptionName = subscription.subscriptionNumber,
      processingStage = CohortTableFilter.ReadyForEstimation,
      billingPeriod = Some("Annual")
    )

    val cohortSpec = CohortSpec(
      "SupporterPlus2026",
      "",
      LocalDate.of(2026, 9, 19),
    )

    val today = LocalDate.of(2026, 7, 2)

    assertEquals(
      AmendmentEffectiveDateCalculator.amendmentEffectiveDateLowerBound(
        cohortItem: CohortItem,
        subscription: ZuoraSubscription,
        invoicePreview: ZuoraInvoiceList,
        cohortSpec: CohortSpec,
        today: LocalDate
      ),
      LocalDate.of(2026, 12, 20)
    )

    // Here the subscription is ready to be price risen one year after the end of the discount.
    // Discount ended in 2025-12-20.
    // We are still going to be price risen on that date as it's billing date
  }

  test("1 year policy [04]") {

    /*
      04:

      This one is a manually modified variant of 03. We shifted the discount dates to end earlier.
      We expect the price rise date to be the next billind date.

      Annually EUR
      Acquired on 26 Nov 2016
      Got that one to test the one year discount policy.

      The discount has dates
        "effectiveStartDate": "2024-11-10"
        "effectiveEndDate": "2025-11-10"

      The supporter plus rate plan has dates
        "effectiveStartDate": "2024-12-20"
        "effectiveEndDate": "2026-12-20"
     */

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/04/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/04/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/04/invoice-preview.json")

    val cohortItem = CohortItem(
      subscriptionName = subscription.subscriptionNumber,
      processingStage = CohortTableFilter.ReadyForEstimation,
      billingPeriod = Some("Annual")
    )

    val cohortSpec = CohortSpec(
      "SupporterPlus2026",
      "",
      LocalDate.of(2026, 9, 19),
    )

    val today = LocalDate.of(2026, 7, 2)

    assertEquals(
      AmendmentEffectiveDateCalculator.amendmentEffectiveDateLowerBound(
        cohortItem: CohortItem,
        subscription: ZuoraSubscription,
        invoicePreview: ZuoraInvoiceList,
        cohortSpec: CohortSpec,
        today: LocalDate
      ),
      LocalDate.of(2026, 11, 10)
    )

    // Here the subscription is ready to be price risen one year after the end of the discount.
    // Discount ended in 2025-11-10.
    // We are still going to be price risen on 2026-12-20
  }

  test("1 year policy [05]") {

    /*
      05:

      This one is a manually modified variant of 05. We shifted the discount dates to end later.
      We expect the price rise date to not be the next billing date, but the one after

      Annually EUR
      Acquired on 26 Nov 2016
      Got that one to test the one year discount policy.

      The discount has dates
        "effectiveStartDate": "2024-12-30"
        "effectiveEndDate": "2025-12-30"

      The supporter plus rate plan has dates
        "effectiveStartDate": "2024-12-20"
        "effectiveEndDate": "2026-12-20"
     */

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/05/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/05/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/05/invoice-preview.json")

    val cohortItem = CohortItem(
      subscriptionName = subscription.subscriptionNumber,
      processingStage = CohortTableFilter.ReadyForEstimation,
      billingPeriod = Some("Annual")
    )

    val cohortSpec = CohortSpec(
      "SupporterPlus2026",
      "",
      LocalDate.of(2026, 9, 19),
    )

    val today = LocalDate.of(2026, 7, 2)

    assertEquals(
      AmendmentEffectiveDateCalculator.amendmentEffectiveDateLowerBound(
        cohortItem: CohortItem,
        subscription: ZuoraSubscription,
        invoicePreview: ZuoraInvoiceList,
        cohortSpec: CohortSpec,
        today: LocalDate
      ),
      LocalDate.of(2026, 12, 30)
    )

    // Here we expect the price rise date to be at least 2025-12-30 (end date of the discount plus one year).
    // This means that we cannot price rise on the next billing date of 2026-12-20
  }

  test("1 year policy [05] (with incorrect billing period)") {

    /*
      05:

      This one is a manually modified variant of 05. We shifted the discount dates to end later.
      We expect the price rise date to not be the next billing date, but the one after

      Annually EUR
      Acquired on 26 Nov 2016
      Got that one to test the one year discount policy.

      The discount has dates
        "effectiveStartDate": "2024-12-30"
        "effectiveEndDate": "2025-12-30"

      The supporter plus rate plan has dates
        "effectiveStartDate": "2024-12-20"
        "effectiveEndDate": "2026-12-20"
     */

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/05/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/05/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/05/invoice-preview.json")

    val cohortItem = CohortItem(
      subscriptionName = subscription.subscriptionNumber,
      processingStage = CohortTableFilter.ReadyForEstimation,
      billingPeriod = Some("Month")
    )

    val cohortSpec = CohortSpec(
      "SupporterPlus2026",
      "",
      LocalDate.of(2026, 9, 19),
    )

    val today = LocalDate.of(2026, 7, 2)

    assertEquals(
      AmendmentEffectiveDateCalculator.amendmentEffectiveDateLowerBound(
        cohortItem: CohortItem,
        subscription: ZuoraSubscription,
        invoicePreview: ZuoraInvoiceList,
        cohortSpec: CohortSpec,
        today: LocalDate
      ),
      LocalDate.of(2026, 9, 19)
    )

    // Here the billing period was set to month, to force a different date. In this case we
    // return the earliest possible date from the cohort specs
  }
}
