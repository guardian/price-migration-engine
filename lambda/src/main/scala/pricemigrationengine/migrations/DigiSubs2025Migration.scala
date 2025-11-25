package pricemigrationengine.migrations
import pricemigrationengine.model.{BillingPeriod, ZuoraRatePlan, _}
import pricemigrationengine.services.Zuora

import java.time.LocalDate
import ujson._
import upickle.default._
import zio.ZIO

import scala.math.BigDecimal.RoundingMode

object DigiSubs2025Migration {

  val maxLeadTime = 35
  val minLeadTime = 33

  val priceGrid: Map[(BillingPeriod, String), BigDecimal] = Map(
    // Monthly
    (Monthly, "GBP") -> BigDecimal(18.0),
    (Monthly, "EUR") -> BigDecimal(20.0),
    (Monthly, "USD") -> BigDecimal(28.0),
    (Monthly, "CAD") -> BigDecimal(30.0),
    (Monthly, "AUD") -> BigDecimal(30.0),
    (Monthly, "NZD") -> BigDecimal(30.0),
    // Quarterly
    (Quarterly, "GBP") -> BigDecimal(54.0),
    (Quarterly, "EUR") -> BigDecimal(60.0),
    (Quarterly, "USD") -> BigDecimal(84.0),
    (Quarterly, "CAD") -> BigDecimal(90.0),
    (Quarterly, "AUD") -> BigDecimal(90.0),
    (Quarterly, "NZD") -> BigDecimal(90.0),
    // Annual
    (Annual, "GBP") -> BigDecimal(180.0),
    (Annual, "EUR") -> BigDecimal(200.0),
    (Annual, "USD") -> BigDecimal(280.0),
    (Annual, "CAD") -> BigDecimal(300.0),
    (Annual, "AUD") -> BigDecimal(300.0),
    (Annual, "NZD") -> BigDecimal(300.0),
  )

  def priceGrid(billingPeriod: BillingPeriod, currency: String): Option[BigDecimal] = {
    priceGrid.get((billingPeriod, currency))
  }

  def priceData(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
  ): Either[DataExtractionFailure, PriceData] = ???

  def amendmentOrderPayload(
      cohortItem: CohortItem,
      orderDate: LocalDate,
      accountNumber: String,
      subscriptionNumber: String,
      effectDate: LocalDate,
      zuora_subscription: ZuoraSubscription,
      commsPrice: BigDecimal,
      invoiceList: ZuoraInvoiceList,
  ): Either[Failure, Value] = ???
}
