package pricemigrationengine.handlers

import pricemigrationengine.Fixtures
import pricemigrationengine.handlers.EstimationHandlerSpec.testTime1
import zio.test._

import java.time.{LocalDate, LocalDateTime, OffsetDateTime, ZoneOffset}

object EstimationHandlerTest extends ZIOSpecDefault {

  private val absoluteEarliestStartDate = LocalDate.of(2020, 6, 2)
  private val testTime1 = OffsetDateTime.of(LocalDateTime.of(2022, 7, 10, 10, 2), ZoneOffset.ofHours(0)).toInstant

  override def spec: Spec[TestEnvironment, Any] =
    suite("spreadEarliestStartDate")(
      test("gives default value for a quarterly subscription") {
        for {
          _ <- TestClock.setTime(testTime1)
          _ <- TestRandom.feedInts(1)
          earliestStartDate <- EstimationHandler.spreadEarliestStartDate(
            subscription = Fixtures.subscriptionFromJson("NewspaperVoucher/QuarterlyVoucher/Subscription.json"),
            invoicePreview = Fixtures.invoiceListFromJson("NewspaperVoucher/QuarterlyVoucher/InvoicePreview.json"),
            absoluteEarliestStartDate
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
            absoluteEarliestStartDate
          )
        } yield assertTrue(earliestStartDate == LocalDate.of(2020, 7, 2))
      }
    )
}
