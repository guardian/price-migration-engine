package pricemigrationengine.migrations
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._
import pricemigrationengine.libs._

import java.time.LocalDate
import ujson._

sealed trait Newspaper2025ProductType
object Voucher extends Newspaper2025ProductType
object Subcard extends Newspaper2025ProductType
object HomeDelivery extends Newspaper2025ProductType

sealed trait Newspaper2025Frequency
object EverydayPlus extends Newspaper2025Frequency
object SixdayPlus extends Newspaper2025Frequency

object Newspaper2025Migration {

  // ------------------------------------------------
  // Price capping
  // ------------------------------------------------

  val priceCap = 1.20 // TODO: Not signed off yet

  // ------------------------------------------------
  // Notification Timings
  // ------------------------------------------------

  val maxLeadTime = 37
  val minLeadTime = 35

  // ------------------------------------------------
  // Price Grid
  //
  // It is now a standard feature of modern migrations that we hardcode the price grid
  // into the migration. This is a manual step in the setting of the migration, but it has the
  // advantage of not relying on complex look up of the price catalogue. (In fact we could even migrate
  // to prices not present in the price catalogue)
  // ------------------------------------------------

  val pricesVouncherEverydayPlus: Map[BillingPeriod, BigDecimal] = Map(
    Monthly -> BigDecimal(69.99),
    Quarterly -> BigDecimal(209.97),
    SemiAnnual -> BigDecimal(419.94),
    Annual -> BigDecimal(839.88),
  )

  val pricesVouncherSixdayPlus: Map[BillingPeriod, BigDecimal] = Map(
    Monthly -> BigDecimal(61.99),
    Quarterly -> BigDecimal(185.97),
    SemiAnnual -> BigDecimal(371.94),
    Annual -> BigDecimal(743.88),
  )

  val pricesSubCardEverydayPlus: Map[BillingPeriod, BigDecimal] = Map(
    Monthly -> BigDecimal(69.99),
    Quarterly -> BigDecimal(209.97),
  )

  val pricesSubCardSixdayPlus: Map[BillingPeriod, BigDecimal] = Map(
    Monthly -> BigDecimal(61.99),
    Quarterly -> BigDecimal(185.97),
  )

  val pricesHomeDeliveryEverydayPlus: Map[BillingPeriod, BigDecimal] = Map(
    Monthly -> BigDecimal(83.99),
  )

  val pricesHomeDeliverySixdayPlus: Map[BillingPeriod, BigDecimal] = Map(
    Monthly -> BigDecimal(73.99),
  )

  // ------------------------------------------------
  // Helpers
  // ------------------------------------------------

  def priceLookUp(
      productType: Newspaper2025ProductType,
      frequency: Newspaper2025Frequency,
      billingPeriod: BillingPeriod
  ): Option[BigDecimal] = {
    productType match {
      case Voucher => {
        frequency match {
          case EverydayPlus => pricesVouncherEverydayPlus.get(billingPeriod)
          case SixdayPlus   => pricesVouncherSixdayPlus.get(billingPeriod)
        }
      }
      case Subcard => {
        frequency match {
          case EverydayPlus => pricesSubCardEverydayPlus.get(billingPeriod)
          case SixdayPlus   => pricesSubCardSixdayPlus.get(billingPeriod)
        }
      }
      case HomeDelivery => {
        frequency match {
          case EverydayPlus => pricesHomeDeliveryEverydayPlus.get(billingPeriod)
          case SixdayPlus   => pricesHomeDeliverySixdayPlus.get(billingPeriod)
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
  ): Either[Failure, Value] = {
    ???
  }
}
