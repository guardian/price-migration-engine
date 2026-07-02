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

  test("price data") {
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

  test("1 year policy") {
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

  test("1 year policy") {
    // 02:
    // Monthly, GBP
    // Acquired in 2 Aug 2017, used to test the basic 1 year policy (in this case it is irrelevant)

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/02/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/02/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/02/invoice-preview.json")

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
      LocalDate.of(2026, 9, 19)
    )

    // Here the subscription is ready to be price risen as soon as possible. The lowerbound
    // is equal to the CohortSpec's earliest amendment effective date.
  }
}
