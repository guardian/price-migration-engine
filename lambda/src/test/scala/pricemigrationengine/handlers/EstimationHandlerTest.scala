package pricemigrationengine.handlers

import pricemigrationengine.Fixtures
import zio.test._

import java.time.LocalDate

object EstimationHandlerTest extends ZIOSpecDefault {

  private val absoluteEarliestStartDate = LocalDate.of(2020, 6, 2)

  override def spec: Spec[TestEnvironment, Any] =
    suite("spreadEarliestStartDate")(
      test("gives default value for a quarterly subscription") {
        for {
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
