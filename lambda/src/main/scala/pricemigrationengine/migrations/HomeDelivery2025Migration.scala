package pricemigrationengine.migrations
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._
import pricemigrationengine.libs._
import pricemigrationengine.services.Zuora

import java.time.LocalDate
import ujson._
import upickle.default._
import zio.ZIO

sealed trait HomeDelivery2025DeliveryPattern
object HomeDelivery2025Everyday extends HomeDelivery2025DeliveryPattern
object HomeDelivery2025Sixday extends HomeDelivery2025DeliveryPattern
object HomeDelivery2025Weekend extends HomeDelivery2025DeliveryPattern
object HomeDelivery2025Saturday extends HomeDelivery2025DeliveryPattern

case class HomeDelivery2025ExtraAttributes(brandTitle: String, removeDiscount: Option[Boolean] = None)
object HomeDelivery2025ExtraAttributes {
  implicit val reader: Reader[HomeDelivery2025ExtraAttributes] = macroR

  // Each item of the migration is going to have a migration extended attributes object
  // with a brandTitle key. The value of this key is to be sent to Braze, during the
  // notification handler to decide the labelling to the entity in the email. At that point the
  // attribute will be called `brand_title`

  // usage
  // val s = """{ "brandTitle": "the Guardian" }"""
  // val s = """{ "brandTitle": "the Guardian and the Observer" }"""
  // val s = """{ "brandTitle": "the Guardian", "removeDiscount": true }"""
  // val attributes: HomeDelivery2025ExtraAttributes = upickle.default.read[HomeDelivery2025ExtraAttributes](s)
}

// (Comment Group: 571dac68)

case class HomeDelivery2025NotificationData(
    brandTitle: String,
)

object HomeDelivery2025Migration {

  // ------------------------------------------------
  // Price capping
  // ------------------------------------------------

  val priceCap = 1.20

  // ------------------------------------------------
  // Notification Timings
  // ------------------------------------------------

  val maxLeadTime = 35
  val minLeadTime = 33

  // ------------------------------------------------
  // Price Grid
  //
  // It is now a standard feature of modern migrations that we hardcode the price grid
  // into the migration. This is a manual step in the setting of the migration, but it has the
  // advantage of not relying on complex look up of the price catalogue. (In fact we could even migrate
  // to prices not present in the price catalogue)
  // ------------------------------------------------

  val newPrices: Map[(HomeDelivery2025DeliveryPattern, BillingPeriod), BigDecimal] = Map(
    // Everyday
    (HomeDelivery2025Everyday, Monthly) -> BigDecimal(83.99),
    (HomeDelivery2025Everyday, Quarterly) -> BigDecimal(251.97),
    (HomeDelivery2025Everyday, SemiAnnual) -> BigDecimal(503.94),
    (HomeDelivery2025Everyday, Annual) -> BigDecimal(1007.88),
    // Sixday
    (HomeDelivery2025Sixday, Monthly) -> BigDecimal(73.99),
    (HomeDelivery2025Sixday, Quarterly) -> BigDecimal(221.97),
    (HomeDelivery2025Sixday, SemiAnnual) -> BigDecimal(443.94),
    (HomeDelivery2025Sixday, Annual) -> BigDecimal(887.88),
    // Weekend
    (HomeDelivery2025Weekend, Monthly) -> BigDecimal(34.99),
    (HomeDelivery2025Weekend, Quarterly) -> BigDecimal(104.97),
    (HomeDelivery2025Weekend, SemiAnnual) -> BigDecimal(209.94),
    (HomeDelivery2025Weekend, Annual) -> BigDecimal(419.88),
    // Saturday
    (HomeDelivery2025Saturday, Monthly) -> BigDecimal(20.99),
    (HomeDelivery2025Saturday, Quarterly) -> BigDecimal(62.97),
    (HomeDelivery2025Saturday, SemiAnnual) -> BigDecimal(125.94),
    (HomeDelivery2025Saturday, Annual) -> BigDecimal(251.88),
  )

  // ------------------------------------------------
  // Helpers
  // ------------------------------------------------

  def getLabelFromMigrationExtraAttributes(item: CohortItem): Option[String] = {
    for {
      attributes <- item.migrationExtraAttributes
    } yield {
      val data: HomeDelivery2025ExtraAttributes =
        upickle.default.read[HomeDelivery2025ExtraAttributes](attributes)
      data.brandTitle
    }
  }

  def decideShouldRemoveDiscount(item: CohortItem): Boolean = {
    val flag_opt = (for {
      attributes <- item.migrationExtraAttributes
      data: Newspaper2025ExtraAttributes =
        upickle.default.read[Newspaper2025ExtraAttributes](attributes)
      removeDiscount <- data.removeDiscount
    } yield removeDiscount)
    flag_opt.getOrElse(false)
  }

  // (Comment Group: 571dac68)

  def getNotificationData(
      cohortSpec: CohortSpec,
      item: CohortItem
  ): ZIO[Zuora, Failure, HomeDelivery2025NotificationData] = {
    MigrationType(cohortSpec) match {
      case HomeDelivery2025 => {
        (for {
          brandTitle <- getLabelFromMigrationExtraAttributes(item)
        } yield HomeDelivery2025NotificationData(
          brandTitle
        )) match {
          case None =>
            ZIO.fail(
              DataExtractionFailure(
                s"Could not build HomeDelivery2025NotificationData for item ${item.toString}"
              )
            )
          case Some(d) => ZIO.succeed(d)
        }
      }
      case _ => ZIO.succeed(HomeDelivery2025NotificationData(""))
    }
  }

  def priceLookUp(
      deliveryFrequency: HomeDelivery2025DeliveryPattern,
      billingPeriod: BillingPeriod
  ): Option[BigDecimal] = {
    newPrices.get((deliveryFrequency, billingPeriod))
  }

  def subscriptionToLastPriceMigrationDate(subscription: ZuoraSubscription): Option[LocalDate] = {
    for {
      ratePlan <- SI2025RateplanFromSub.determineRatePlan(subscription)
      date <- SI2025Extractions.determineLastPriceMigrationDate(ratePlan)
    } yield date
  }

  def decideDeliveryPattern(ratePlan: ZuoraRatePlan): Option[HomeDelivery2025DeliveryPattern] = {
    // I have checked every subscription and the only product names that come up are
    // Newspaper Voucher
    // Newspaper Digital Voucher
    // Newspaper Delivery
    // And I checked with Marketing that the below mapping is correct and in particular there doesn't
    // seem to be any subscription that uses the [2025 - Price Grid - Sub Card] part of the pricing grid,
    // what would map to Newspaper2025P1Subcard
    ratePlan.ratePlanName match {
      case "Everyday" => Some(HomeDelivery2025Everyday)
      case "Weekend"  => Some(HomeDelivery2025Weekend)
      case "Sixday"   => Some(HomeDelivery2025Sixday)
      case "Saturday" => Some(HomeDelivery2025Saturday)
      case _          => None
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
      account: ZuoraAccount
  ): Either[DataExtractionFailure, PriceData] = {
    val priceDataOpt: Option[PriceData] = for {
      ratePlan <- SI2025RateplanFromSubAndInvoices.determineRatePlan(subscription, invoiceList)
      currency <- SI2025Extractions.determineCurrency(ratePlan)
      oldPrice = SI2025Extractions.determineOldPrice(ratePlan)
      billingPeriod <- SI2025Extractions.determineBillingPeriod(ratePlan)
      deliveryPattern <- decideDeliveryPattern(ratePlan)
      newPrice <- priceLookUp(deliveryPattern, billingPeriod)
    } yield PriceData(currency, oldPrice, newPrice, BillingPeriod.toString(billingPeriod))
    priceDataOpt match {
      case Some(pricedata) => Right(pricedata)
      case None =>
        Left(
          DataExtractionFailure(
            s"[93a96aee] Could not determine PriceData for subscription ${subscription.subscriptionNumber}"
          )
        )
    }
  }

  def amendmentOrderPayload(
      orderDate: LocalDate,
      accountNumber: String,
      subscriptionNumber: String,
      effectDate: LocalDate,
      subscription: ZuoraSubscription,
      oldPrice: BigDecimal,
      estimatedNewPrice: BigDecimal,
      priceCap: BigDecimal,
      invoiceList: ZuoraInvoiceList,
  ): Either[Failure, Value] = {
    ???
  }
}
