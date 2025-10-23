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

  def brazeName(cohortItem: CohortItem): Option[String] = {
    /*
      - SV_MB_PriceRiseMonthly_2025 (MONTHLY)
        f0ee9579-7f59-41aa-9d4a-41dfc0b4edfa

      - SV_MB_PriceRiseAnnual_2025 (ANNUAL)
        78d377ca-9f32-4ae2-b8da-c5c678fde5b6
     */

    for {
      billingPeriod <- cohortItem.billingPeriod
    } yield {
      billingPeriod match {
        case "Month"  => "SV_MB_PriceRiseMonthly_2025"
        case "Annual" => "SV_MB_PriceRiseAnnual_2025"
        case _        => throw new Exception("[5cd26ca0] unexpected Membership2025 cohort item billing period")
      }
    }
  }

  // -----------------------------------------------------

  def logValue[T](label: String)(value: T): T = {
    println(s"$label: $value")
    value
  }

  def priceData(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
  ): Either[DataExtractionFailure, PriceData] = {
    println(s"[7909e3a4] subscription: ${subscription}")
    println(s"[1f0e0b1a] invoiceList: ${invoiceList}")
    val priceDataOpt = for {
      ratePlan <- SI2025RateplanFromSubAndInvoices
        .determineRatePlan(subscription, invoiceList)
        .map(logValue("ratePlan"))
      currency <- SI2025Extractions.determineCurrency(ratePlan).map(logValue("currency"))
      billingPeriod <- SI2025Extractions.determineBillingPeriod(ratePlan).map(logValue("billingPeriod"))
      oldPrice = logValue("oldPrice")(SI2025Extractions.determineOldPrice(ratePlan))
      newPrice <- priceGridNewPrices.get((billingPeriod, currency)).map(logValue("newPrice"))
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
