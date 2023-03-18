package pricemigrationengine.handlers

import pricemigrationengine.Fixtures
import pricemigrationengine.model.{CohortSpec, EstimationResult, SuccessfulEstimationResult}
import zio.test._

import java.time.{LocalDate, LocalDateTime, OffsetDateTime, ZoneOffset}

object EstimationHandlerTest extends ZIOSpecDefault {

  private val absoluteEarliestStartDate = LocalDate.of(2020, 6, 2)
  private val testTime1 = OffsetDateTime.of(LocalDateTime.of(2022, 7, 10, 10, 2), ZoneOffset.ofHours(0)).toInstant

  override def spec: Spec[TestEnvironment, Any] = {
    suite("spreadEarliestStartDate")(
      test("gives default value for a quarterly subscription") {
        for {
          _ <- TestClock.setTime(testTime1)
          _ <- TestRandom.feedInts(1)
          earliestStartDate <- EstimationHandler.spreadEarliestStartDate(
            subscription = Fixtures.subscriptionFromJson("NewspaperVoucher/QuarterlyVoucher/Subscription.json"),
            invoicePreview = Fixtures.invoiceListFromJson("NewspaperVoucher/QuarterlyVoucher/InvoicePreview.json"),
            CohortSpec("Cohort1", "Campaign1", LocalDate.of(2000, 1, 1), absoluteEarliestStartDate)
          )
        } yield assertTrue(earliestStartDate == LocalDate.of(2020, 6, 2))
      },
      test("gives randomised value for a monthly subscription") {
        for {
          _ <- TestClock.setTime(testTime1)
          _ <- TestRandom.feedInts(1)
          earliestStartDate <- EstimationHandler.spreadEarliestStartDate(
            subscription = Fixtures.subscriptionFromJson("NewspaperVoucher/Monthly/Subscription.json"),
            invoicePreview = Fixtures.invoiceListFromJson("NewspaperVoucher/Monthly/InvoicePreview.json"),
            CohortSpec("Cohort1", "Campaign1", LocalDate.of(2000, 1, 1), absoluteEarliestStartDate)
          )
        } yield assertTrue(earliestStartDate == LocalDate.of(2020, 7, 2))
      }
    )
    suite("membership estimations")(
      test("Estimation uses uncapped price") {

        // The name of the cohort here is import to trigger the membership2023_Batch1 code path
        val cohortSpec =
          CohortSpec("Membership2023_Batch1", "Campaign1", LocalDate.of(2000, 1, 1), LocalDate.of(2023, 5, 1))
        val startDate = cohortSpec.earliestPriceMigrationStartDate

        val account = Fixtures.accountFromJson("Membership2023/account.json")
        val catalogue = Fixtures.productCatalogueFromJson("Membership2023/catalogue.json")
        val subscription = Fixtures.subscriptionFromJson("Membership2023/subscription.json")
        val invoicePreview = Fixtures.invoiceListFromJson("Membership2023/invoice-preview.json")

        // In the test, the actual startDate and the cohort's cohortSpec earliestPriceMigrationStartDate are same (which is fine)
        val estimationResult = EstimationResult(account, catalogue, subscription, invoicePreview, startDate, cohortSpec)

        assertTrue(
          estimationResult == Right(
            SuccessfulEstimationResult("SUBSCRIPTION-NUMBER", LocalDate.of(2023, 5, 13), "GBP", 5, 7, "Month")
          )
        )
      }
    )
  }
}
