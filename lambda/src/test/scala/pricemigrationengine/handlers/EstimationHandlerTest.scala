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
          earliestStartDate <- StartDates.startDateLowerBound(
            subscription =
              Fixtures.subscriptionFromJson("Handlers/EstimationHandler/QuarterlyVoucher/Subscription.json"),
            invoicePreview =
              Fixtures.invoiceListFromJson("Handlers/EstimationHandler/QuarterlyVoucher/InvoicePreview.json"),
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
          earliestStartDate <- StartDates.startDateLowerBound(
            subscription = Fixtures.subscriptionFromJson("Handlers/EstimationHandler/Monthly/Subscription.json"),
            invoicePreview = Fixtures.invoiceListFromJson("Handlers/EstimationHandler/Monthly/InvoicePreview.json"),
            CohortSpec("Cohort1", "Campaign1", LocalDate.of(2000, 1, 1), absoluteEarliestStartDate),
            today
          )
        } yield assertTrue(earliestStartDate == LocalDate.of(2020, 7, 2))
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
      }
    )
  }
}
