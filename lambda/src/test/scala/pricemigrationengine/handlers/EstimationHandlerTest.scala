package pricemigrationengine.handlers

import pricemigrationengine.Fixtures
import pricemigrationengine.model.{CohortSpec, EstimationResult, EstimationData}
import zio.test._
import pricemigrationengine.util.{Date, StartDates}

import java.time.{LocalDate, LocalDateTime, OffsetDateTime, ZoneOffset}

object EstimationHandlerTest extends ZIOSpecDefault {

  private val absoluteEarliestStartDate = LocalDate.of(2020, 6, 2)
  private val testTime1 = OffsetDateTime.of(LocalDateTime.of(2022, 7, 10, 10, 2), ZoneOffset.ofHours(0)).toInstant

  override def spec: Spec[TestEnvironment, Any] = {
    suite("EstimationHandlerTest")(
      test("spreadEarliestStartDate: gives default value for a quarterly subscription") {
        val today = LocalDate.of(2000, 1, 1)
        for {
          _ <- TestClock.setTime(testTime1)
          _ <- TestRandom.feedInts(1)
          earliestStartDate <- StartDates.decideStartDate(
            subscription = Fixtures.subscriptionFromJson("NewspaperVoucher/QuarterlyVoucher/Subscription.json"),
            invoicePreview = Fixtures.invoiceListFromJson("NewspaperVoucher/QuarterlyVoucher/InvoicePreview.json"),
            CohortSpec("Cohort1", "Campaign1", LocalDate.of(2000, 1, 1), absoluteEarliestStartDate),
            today
          )
        } yield assertTrue(earliestStartDate == LocalDate.of(2020, 7, 2))
      },
      test("spreadEarliestStartDate: gives randomised value for a monthly subscription") {
        val today = LocalDate.of(2000, 1, 1)
        for {
          _ <- TestClock.setTime(testTime1)
          _ <- TestRandom.feedInts(1)
          earliestStartDate <- StartDates.decideStartDate(
            subscription = Fixtures.subscriptionFromJson("NewspaperVoucher/Monthly/Subscription.json"),
            invoicePreview = Fixtures.invoiceListFromJson("NewspaperVoucher/Monthly/InvoicePreview.json"),
            CohortSpec("Cohort1", "Campaign1", LocalDate.of(2000, 1, 1), absoluteEarliestStartDate),
            today
          )
        } yield assertTrue(earliestStartDate == LocalDate.of(2020, 7, 2))
      },
      test("membership estimations: Estimation uses uncapped price, Membership2023_Batch1") {

        // The name of the cohort here is import to trigger the membership2023_Batch1 code path
        val cohortSpec =
          CohortSpec("Membership2023_Batch1", "Campaign1", LocalDate.of(2000, 1, 1), LocalDate.of(2023, 5, 1))
        val startDate = cohortSpec.earliestPriceMigrationStartDate

        val account = Fixtures.accountFromJson("Membership2023/Batch1/GBP/account.json")
        val catalogue = Fixtures.productCatalogueFromJson("Membership2023/Batch1/GBP/catalogue.json")
        val subscription = Fixtures.subscriptionFromJson("Membership2023/Batch1/GBP/subscription.json")
        val invoicePreview = Fixtures.invoiceListFromJson("Membership2023/Batch1/GBP/invoice-preview.json")

        // In the test, the actual startDate and the cohort's cohortSpec earliestPriceMigrationStartDate are same (which is fine)
        val estimationResult = EstimationResult(account, catalogue, subscription, invoicePreview, startDate, cohortSpec)

        assertTrue(
          estimationResult == Right(
            EstimationData("SUBSCRIPTION-NUMBER", LocalDate.of(2023, 5, 13), "GBP", 5, 7, "Month")
          )
        )
      },
      test("membership estimations: Estimation uses uncapped price, Membership2023_Batch2") {
        // Similar to the previous test (and in particular we do reuse Batch1's fixtures as they are structurally
        // indistinguishable from Batch1), but shifting by a month for Batch2.

        val cohortSpec =
          CohortSpec("Membership2023_Batch2", "Campaign1", LocalDate.of(2000, 1, 1), LocalDate.of(2023, 6, 1))
        val startDate = cohortSpec.earliestPriceMigrationStartDate

        val account = Fixtures.accountFromJson("Membership2023/Batch1/GBP/account.json")
        val catalogue = Fixtures.productCatalogueFromJson("Membership2023/Batch1/GBP/catalogue.json")
        val subscription = Fixtures.subscriptionFromJson("Membership2023/Batch1/GBP/subscription.json")
        val invoicePreview = Fixtures.invoiceListFromJson("Membership2023/Batch1/GBP/invoice-preview.json")

        // In the test, the actual startDate and the cohort's cohortSpec earliestPriceMigrationStartDate are same (which is fine)
        val estimationResult = EstimationResult(account, catalogue, subscription, invoicePreview, startDate, cohortSpec)

        // Compared to Membership2023_Batch1, we move the earliest possible start date by a month,
        // from LocalDate.of(2023, 5, 1) to LocalDate.of(2023, 6, 1) will result in the actual subscription start
        // date be postponed by a month (from LocalDate.of(2023, 5, 13) to LocalDate.of(2023, 6, 13)).
        assertTrue(
          estimationResult == Right(
            EstimationData("SUBSCRIPTION-NUMBER", LocalDate.of(2023, 6, 13), "GBP", 5, 7, "Month")
          )
        )
      },
      test("membership estimations: Estimation uses uncapped price, Membership2023_Batch3 (GBP)") {
        // Batch 3 is the annual runs

        val cohortSpec =
          CohortSpec("Membership2023_Batch3", "Campaign1", LocalDate.of(2000, 1, 1), LocalDate.of(2023, 7, 1))
        val startDate = cohortSpec.earliestPriceMigrationStartDate

        val account = Fixtures.accountFromJson("Membership2023/Batch3/GBP/account.json")
        val catalogue = Fixtures.productCatalogueFromJson("Membership2023/Batch3/GBP/catalogue.json")
        val subscription = Fixtures.subscriptionFromJson("Membership2023/Batch3/GBP/subscription.json")
        val invoicePreview = Fixtures.invoiceListFromJson("Membership2023/Batch3/GBP/invoice-preview.json")

        val estimationResult = EstimationResult(account, catalogue, subscription, invoicePreview, startDate, cohortSpec)

        assertTrue(
          estimationResult == Right(
            EstimationData("SUBSCRIPTION-NUMBER", LocalDate.of(2024, 1, 20), "GBP", 49, 75, "Annual")
          )
        )
      },
      test("membership estimations: Estimation uses uncapped price, Membership2023_Batch3 (USD)") {
        // Batch 3 is the annual runs

        val cohortSpec =
          CohortSpec("Membership2023_Batch3", "Campaign1", LocalDate.of(2000, 1, 1), LocalDate.of(2023, 7, 1))
        val startDate = cohortSpec.earliestPriceMigrationStartDate

        val account = Fixtures.accountFromJson("Membership2023/Batch3/USD/account.json")
        val catalogue = Fixtures.productCatalogueFromJson("Membership2023/Batch3/USD/catalogue.json")
        val subscription = Fixtures.subscriptionFromJson("Membership2023/Batch3/USD/subscription.json")
        val invoicePreview = Fixtures.invoiceListFromJson("Membership2023/Batch3/USD/invoice-preview.json")

        val estimationResult = EstimationResult(account, catalogue, subscription, invoicePreview, startDate, cohortSpec)

        assertTrue(
          estimationResult == Right(
            EstimationData("SUBSCRIPTION-NUMBER", LocalDate.of(2024, 1, 20), "USD", 69, 120, "Annual")
          )
        )
      },
      test("during estimation, we correctly prevent start dates that are too close: datesMax") {
        val date1 = LocalDate.of(2023, 4, 1)
        val date2 = LocalDate.of(2023, 4, 2)
        assertTrue(Date.datesMax(date1, date1) == date1)
        assertTrue(Date.datesMax(date1, date2) == date2)
      },
      test(
        "during estimation, we correctly prevent start dates that are too close: decideEarliestStartDate (legacy case, part 1)"
      ) {
        val today = LocalDate.of(2023, 4, 1)
        val cohortSpec = CohortSpec("Cohort1", "Campaign1", LocalDate.of(2000, 1, 1), LocalDate.of(2022, 5, 1))

        // today is: 2023-04-01
        // The Cohort's earliestPriceMigrationStartDate is 2022-05-01
        // (Today + 36 days) is after earliestPriceMigrationStartDate
        // The earliest start date needs to be 36 days ahead of today (35 days min time + 1) -> 2023-05-07

        assertTrue(StartDates.cohortSpecLowerBound(cohortSpec, today) == LocalDate.of(2023, 5, 7))
      },
      test(
        "during estimation, we correctly prevent start dates that are too close: decideEarliestStartDate (legacy case, part 2)"
      ) {

        val today = LocalDate.of(2020, 4, 1)
        val cohortSpec = CohortSpec("Cohort1", "Campaign1", LocalDate.of(2000, 1, 1), LocalDate.of(2022, 5, 1))

        // today is: 2020-04-01
        // The Cohort's earliestPriceMigrationStartDate is 2022-05-01
        // earliestPriceMigrationStartDate is after (today + 36 days)
        // The earliest start date can be earliestPriceMigrationStartDate

        assertTrue(StartDates.cohortSpecLowerBound(cohortSpec, today) == cohortSpec.earliestPriceMigrationStartDate)
      },
      test(
        "during estimation, we correctly prevent start dates that are too close: decideEarliestStartDate (membership)"
      ) {

        val today = LocalDate.of(2023, 4, 1)
        val cohortSpec =
          CohortSpec("Membership2023_Batch1", "Campaign1", LocalDate.of(2000, 1, 1), LocalDate.of(2022, 5, 1))

        // today is: 2023-04-01
        // The Cohort's earliestPriceMigrationStartDate is 2022-05-01
        // (Today + 32 days) is after earliestPriceMigrationStartDate
        // The earliest start date needs to be 32 days ahead of today -> 2023-05-05

        assertTrue(StartDates.cohortSpecLowerBound(cohortSpec, today) == LocalDate.of(2023, 5, 3))
      },
      test("EstimationResult is correct for SupporterPlus2023V1V2 (monthly standard)") {

        val cohortSpec =
          CohortSpec("SupporterPlus2023V1V2", "Campaign1", LocalDate.of(2023, 7, 14), LocalDate.of(2023, 8, 21))

        val account = Fixtures.accountFromJson("SupporterPlus2023V1V2/monthly-standard/account.json")
        val catalogue = Fixtures.productCatalogueFromJson("SupporterPlus2023V1V2/monthly-standard/catalogue.json")
        val subscription = Fixtures.subscriptionFromJson("SupporterPlus2023V1V2/monthly-standard/subscription.json")
        val invoicePreview = Fixtures.invoiceListFromJson("SupporterPlus2023V1V2/monthly-standard/invoice-preview.json")

        val estimationResult = EstimationResult(
          account = account,
          catalogue = catalogue,
          subscription = subscription,
          invoiceList = invoicePreview,
          startDateLowerBound = LocalDate.of(2023, 8, 21),
          cohortSpec = cohortSpec,
        ).toOption.get

        assertTrue(
          estimationResult ==
            EstimationData(
              subscriptionName = "SUBSCRIPTION-NUMBER",
              startDate = LocalDate.of(2023, 9, 1),
              currency = "GBP",
              oldPrice = BigDecimal(10.0),
              estimatedNewPrice = BigDecimal(10),
              billingPeriod = "Month"
            )
        )
      },
      test("EstimationResult is correct for SupporterPlus2023V1V2 (monthly contribution)") {

        val cohortSpec =
          CohortSpec("SupporterPlus2023V1V2", "Campaign1", LocalDate.of(2023, 7, 14), LocalDate.of(2023, 8, 21))

        val account = Fixtures.accountFromJson("SupporterPlus2023V1V2/monthly-contribution/account.json")
        val catalogue = Fixtures.productCatalogueFromJson("SupporterPlus2023V1V2/monthly-contribution/catalogue.json")
        val subscription = Fixtures.subscriptionFromJson("SupporterPlus2023V1V2/monthly-contribution/subscription.json")
        val invoicePreview =
          Fixtures.invoiceListFromJson("SupporterPlus2023V1V2/monthly-contribution/invoice-preview.json")

        val estimationResult = EstimationResult(
          account = account,
          catalogue = catalogue,
          subscription = subscription,
          invoiceList = invoicePreview,
          startDateLowerBound = LocalDate.of(2023, 8, 21),
          cohortSpec = cohortSpec,
        ).toOption.get

        assertTrue(
          estimationResult ==
            EstimationData(
              subscriptionName = "SUBSCRIPTION-NUMBER",
              startDate = LocalDate.of(2023, 9, 3),
              currency = "GBP",
              oldPrice = BigDecimal(25.0),
              estimatedNewPrice = BigDecimal(10),
              billingPeriod = "Month"
            )
        )
      },
      test("EstimationResult is correct for SupporterPlus2023V1V2 (annual standard)") {

        val cohortSpec =
          CohortSpec("SupporterPlus2023V1V2", "Campaign1", LocalDate.of(2023, 7, 14), LocalDate.of(2023, 8, 21))

        val account = Fixtures.accountFromJson("SupporterPlus2023V1V2/annual-standard/account.json")
        val catalogue = Fixtures.productCatalogueFromJson("SupporterPlus2023V1V2/annual-standard/catalogue.json")
        val subscription = Fixtures.subscriptionFromJson("SupporterPlus2023V1V2/annual-standard/subscription.json")
        val invoicePreview = Fixtures.invoiceListFromJson("SupporterPlus2023V1V2/annual-standard/invoice-preview.json")

        val estimationResult = EstimationResult(
          account = account,
          catalogue = catalogue,
          subscription = subscription,
          invoiceList = invoicePreview,
          startDateLowerBound = LocalDate.of(2023, 8, 21),
          cohortSpec = cohortSpec,
        ).toOption.get

        assertTrue(
          estimationResult ==
            EstimationData(
              subscriptionName = "SUBSCRIPTION-NUMBER",
              startDate = LocalDate.of(2024, 7, 2),
              currency = "USD",
              oldPrice = BigDecimal(120),
              estimatedNewPrice = BigDecimal(120),
              billingPeriod = "Annual"
            )
        )
      },
      test("EstimationResult is correct for SupporterPlus2023V1V2 (annual contribution)") {

        val cohortSpec =
          CohortSpec("SupporterPlus2023V1V2", "Campaign1", LocalDate.of(2023, 7, 14), LocalDate.of(2023, 8, 21))

        val account = Fixtures.accountFromJson("SupporterPlus2023V1V2/annual-contribution/account.json")
        val catalogue = Fixtures.productCatalogueFromJson("SupporterPlus2023V1V2/annual-contribution/catalogue.json")
        val subscription = Fixtures.subscriptionFromJson("SupporterPlus2023V1V2/annual-contribution/subscription.json")
        val invoicePreview =
          Fixtures.invoiceListFromJson("SupporterPlus2023V1V2/annual-contribution/invoice-preview.json")

        val estimationResult = EstimationResult(
          account = account,
          catalogue = catalogue,
          subscription = subscription,
          invoiceList = invoicePreview,
          startDateLowerBound = LocalDate.of(2023, 8, 21),
          cohortSpec = cohortSpec,
        ).toOption.get

        assertTrue(
          estimationResult ==
            EstimationData(
              subscriptionName = "SUBSCRIPTION-NUMBER",
              startDate = LocalDate.of(2024, 6, 28),
              currency = "GBP",
              oldPrice = BigDecimal(120),
              estimatedNewPrice = BigDecimal(95),
              billingPeriod = "Annual"
            )
        )
      },
      test(
        "EstimationResult is correct for SupporterPlus2023V1V2 (the curious case of a monthly with incorrect old price)"
      ) {

        val cohortSpec =
          CohortSpec("SupporterPlus2023V1V2", "Campaign1", LocalDate.of(2023, 7, 14), LocalDate.of(2023, 8, 21))

        val account = Fixtures.accountFromJson("SupporterPlus2023V1V2/z-monthly-extra-case-1/account.json")
        val catalogue = Fixtures.productCatalogueFromJson("SupporterPlus2023V1V2/z-monthly-extra-case-1/catalogue.json")
        val subscription =
          Fixtures.subscriptionFromJson("SupporterPlus2023V1V2/z-monthly-extra-case-1/subscription.json")
        val invoicePreview =
          Fixtures.invoiceListFromJson("SupporterPlus2023V1V2/z-monthly-extra-case-1/invoice-preview.json")

        val estimationResult = EstimationResult(
          account = account,
          catalogue = catalogue,
          subscription = subscription,
          invoiceList = invoicePreview,
          startDateLowerBound = LocalDate.of(2023, 8, 21),
          cohortSpec = cohortSpec,
        ).toOption.get

        assertTrue(
          estimationResult ==
            EstimationData(
              subscriptionName = "SUBSCRIPTION-NUMBER",
              startDate = LocalDate.of(2023, 9, 8),
              currency = "GBP",
              oldPrice = BigDecimal(10.0),
              estimatedNewPrice = BigDecimal(10),
              billingPeriod = "Month"
            )
        )
      },
    )
  }
}
