package pricemigrationengine.migrations
import pricemigrationengine.model.PriceCap
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._
import pricemigrationengine.libs._

import java.time.LocalDate

object GuardianWeekly2025Migration {

  // ------------------------------------------------
  // Price capping
  // ------------------------------------------------

  val priceCap = 1.20 // 20%

  // ------------------------------------------------
  // Notification Timings
  // ------------------------------------------------

  val maxLeadTime = 49
  val minLeadTime = 36

  // ------------------------------------------------
  // Price Grid
  //
  // It is now a standard feature of modern migrations that we hardcode the price grid
  // into the migration. This is a manual step in the setting of the migration, but it has the
  // advantage of not relying on complex look up of the price catalogue. (In fact we could even migrate
  // to prices not present in the price catalogue)
  // ------------------------------------------------

  val pricesMonthlyDomestic: Map[String, BigDecimal] = Map(
    "GBP" -> BigDecimal(16.5), // 15 to 16.5
    "USD" -> BigDecimal(30), // 30 remains 30
    "CAD" -> BigDecimal(36), // 33 to 36
    "AUD" -> BigDecimal(44), // 40 to 44
    "NZD" -> BigDecimal(55), // 50 to 55
    "EUR" -> BigDecimal(29), // 26.5 to 29
  )

  val pricesQuarterlyDomestic: Map[String, BigDecimal] = Map(
    "GBP" -> BigDecimal(49.5), // 45 to 49.5
    "USD" -> BigDecimal(90), // 90 remains 90
    "CAD" -> BigDecimal(108), // 99 to 108
    "AUD" -> BigDecimal(132), // 120 to 132
    "NZD" -> BigDecimal(165), // 150 to 165
    "EUR" -> BigDecimal(87), // 79.5 to 87
  )

  val pricesAnnualDomestic: Map[String, BigDecimal] = Map(
    "GBP" -> BigDecimal(198), // 180 to 198
    "USD" -> BigDecimal(360), // 360 remains 360
    "CAD" -> BigDecimal(432), // 396 to 432
    "AUD" -> BigDecimal(528), // 480 to 528
    "NZD" -> BigDecimal(660), // 600 to 660
    "EUR" -> BigDecimal(348), // 318 to 348
  )

  val pricesMonthlyRestOfWorld: Map[String, BigDecimal] = Map(
    "GBP" -> BigDecimal(28), // 24.8 to 28
    "USD" -> BigDecimal(36), // 33 to 36
  )

  val pricesQuarterlyRestOfWorld: Map[String, BigDecimal] = Map(
    "GBP" -> BigDecimal(83.9), // 74.4 to 83.9
    "USD" -> BigDecimal(108), // 99 to 108
  )

  val pricesAnnualRestOfWorld: Map[String, BigDecimal] = Map(
    "GBP" -> BigDecimal(336), // 297.6 to 336
    "USD" -> BigDecimal(432), // 396 to 432
  )

  // ------------------------------------------------
  // Helpers
  // ------------------------------------------------

  def priceLookUp(
      localisation: SubscriptionLocalisation,
      billingPeriod: BillingPeriod,
      currency: String
  ): Option[BigDecimal] = {
    localisation match {
      case Domestic => {
        billingPeriod match {
          case Monthly    => pricesMonthlyDomestic.get(currency)
          case Quarterly  => pricesQuarterlyDomestic.get(currency)
          case SemiAnnual => None
          case Annual     => pricesAnnualDomestic.get(currency)
        }
      }
      case RestOfWorld => {
        billingPeriod match {
          case Monthly    => pricesMonthlyRestOfWorld.get(currency)
          case Quarterly  => pricesQuarterlyRestOfWorld.get(currency)
          case SemiAnnual => None
          case Annual     => pricesAnnualRestOfWorld.get(currency)
        }
      }
    }
  }

  def subscriptionToLastPriceMigrationDate(subscription: ZuoraSubscription): Option[LocalDate] = {
    // This will be moved to Subscription Introspection
    ???
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
      account: ZuoraAccount
  ): Either[DataExtractionFailure, PriceData] = {
    ???
  }

  def amendmentOrderPayload(
      orderDate: LocalDate,
      accountNumber: String,
      subscriptionNumber: String,
      effectDate: LocalDate,
      subscription: ZuoraSubscription,
      oldPrice: BigDecimal,
      estimatedNewPrice: BigDecimal,
      priceCap: BigDecimal
  ): Either[Failure, ZuoraAmendmentOrderPayload] = {
    ???
  }
}
