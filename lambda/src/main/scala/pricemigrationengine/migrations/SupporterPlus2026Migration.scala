package pricemigrationengine.migrations
import pricemigrationengine.model.{BillingPeriod, ZuoraRatePlan, _}
import pricemigrationengine.services.Zuora

import java.time.LocalDate
import ujson._
import upickle.default._
import zio.ZIO

import scala.util.Random

object SupporterPlus2026Migration {

  // ------------------------------------------------
  // Notification Timings
  // ------------------------------------------------

  val maxLeadTime = 35
  val minLeadTime = 33

  // ------------------------------------------------
  // Price Grid
  // ------------------------------------------------

  val priceGridNewPrices: Map[(BillingPeriod, String), BigDecimal] = Map(
    (Monthly, "GBP") -> BigDecimal(14.0),
    (Monthly, "USD") -> BigDecimal(18.0),
    (Monthly, "EUR") -> BigDecimal(14.0),
    (Monthly, "AUD") -> BigDecimal(25.0),
    (Monthly, "CAD") -> BigDecimal(18.0),
    (Monthly, "NZD") -> BigDecimal(25.0),
    (Annual, "GBP") -> BigDecimal(140.0),
    (Annual, "USD") -> BigDecimal(180.0),
    (Annual, "EUR") -> BigDecimal(140.0),
    (Annual, "AUD") -> BigDecimal(250.0),
    (Annual, "CAD") -> BigDecimal(180.0),
    (Annual, "NZD") -> BigDecimal(250.0),
  )

  // ------------------------------------------------
  // Helpers
  // ------------------------------------------------

  def monthliesOverSixWeeks(migrationDate: LocalDate, billingPeriod: BillingPeriod): LocalDate = {
    // This function takes a migration date and  billing period and either return the same
    // migration date, or return the migration date plus a number of months randomly chosen between
    // 0 and 1. This is to ensure that the migration of monthlies is done over 6 weeks.

    // The notifications are starting on 15 July. If we migrated all the monthlies over 1 month,
    // the monthly price rise dates would be between 19 August and 18th September
    // If the migration date falls between 19 August and 30th August, we make a random choice and then
    // possibly add one month.

    (migrationDate, billingPeriod) match {
      case (_, Annual)                                          => migrationDate
      case (date, _) if date.isAfter(LocalDate.of(2026, 8, 31)) => date
      case (date, _)                                            => {
        val shift = Random.nextInt(2) // decide a random integer in the interval [0, 1]
        date.plusMonths(shift)
      }
    }
  }

  // ------------------------------------------------
  // Primary Functions:
  //
  // The primary functions are the main functions that
  // are implemented by the *Migration module.
  //
  // - priceData is used in the Estimation handler
  // - amendmentOrderPayload is used in the Amendment handler
  // ------------------------------------------------

  def priceData(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
  ): Either[DataExtractionFailure, PriceData] = {
    // not yet implemented
    ???
  }

  def amendmentOrderPayload(cohortItem: CohortItem): Either[Failure, Value] = {
    // not yet implemented
    ???
  }
}
