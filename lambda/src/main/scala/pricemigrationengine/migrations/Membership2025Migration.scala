package pricemigrationengine.migrations
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._
import pricemigrationengine.services.Zuora
import pricemigrationengine.model.BillingPeriod

import java.time.LocalDate
import ujson._

object Membership2025Migration {

  val maxLeadTime = 35
  val minLeadTime = 33

  val priceGridNewPrices: Map[(BillingPeriod, String), BigDecimal] = Map(
    (Monthly, "GBP") -> BigDecimal(10.0),
    (Monthly, "USD") -> BigDecimal(13.0),
    (Monthly, "EUR") -> BigDecimal(12.0),
    (Monthly, "AUD") -> BigDecimal(17.0),
    (Monthly, "CAD") -> BigDecimal(15.0),
    (Monthly, "NZD") -> BigDecimal(17.0),
    (Monthly, "ROW") -> BigDecimal(13.0),
    (Annual, "GBP") -> BigDecimal(100.0),
    (Annual, "USD") -> BigDecimal(129.0),
    (Annual, "EUR") -> BigDecimal(120.0),
    (Annual, "AUD") -> BigDecimal(170.0),
    (Annual, "CAD") -> BigDecimal(150.0),
    (Annual, "NZD") -> BigDecimal(170.0),
    (Annual, "ROW") -> BigDecimal(129.0),
  )

  // -----------------------------------------------------

  def priceData(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
  ): Either[DataExtractionFailure, PriceData] = {
    val priceDataOpt = for {
      _ <- Some(1)
      currency = "GBP"
      billingPeriod = Monthly
      oldPrice = BigDecimal(3.0)
      newPrice = BigDecimal(10.0)
    } yield PriceData(currency, oldPrice, newPrice, BillingPeriod.toString(billingPeriod))
    priceDataOpt match {
      case Some(pricedata) => Right(pricedata)
      case None =>
        Left(
          DataExtractionFailure(
            s"[85bebc63] Could not determine PriceData for subscription ${subscription.subscriptionNumber}"
          )
        )
    }
  }

  def amendmentOrderPayload(
      cohortItem: CohortItem,
      orderDate: LocalDate,
      accountNumber: String,
      subscriptionNumber: String,
      effectDate: LocalDate,
      zuora_subscription: ZuoraSubscription,
      oldPrice: BigDecimal,
      commsPrice: BigDecimal,
      invoiceList: ZuoraInvoiceList,
  ): Either[Failure, Value] = ???
}
