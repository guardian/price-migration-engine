package pricemigrationengine.migrations
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._
import pricemigrationengine.libs._
import pricemigrationengine.services.Zuora

import java.time.LocalDate
import ujson._
import upickle.default._
import zio.ZIO

sealed trait HomeDelivery2025DeliveryFrequency
object HomeDelivery2025Everyday extends HomeDelivery2025DeliveryFrequency
object HomeDelivery2025Sixday extends HomeDelivery2025DeliveryFrequency
object HomeDelivery2025Weekend extends HomeDelivery2025DeliveryFrequency
object HomeDelivery2025Saturday extends HomeDelivery2025DeliveryFrequency

case class HomeDelivery2025ExtraAttributes(brandTitle: String)
object HomeDelivery2025ExtraAttributes {
  implicit val reader: Reader[HomeDelivery2025ExtraAttributes] = macroR

  // Each item of the migration is going to have a migration extended attributes object
  // with a brandTitle key. The value of this key is to be sent to Braze, during the
  // notification handler to decide the labelling to the entity in the email. At that point the
  // attribute will be called `brand_title`

  // usage
  // val s = """{ "brandTitle": "the Guardian" }"""
  // val s = """{ "brandTitle": "the Guardian and the Observer" }"""
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

  val newPrices: Map[(HomeDelivery2025DeliveryFrequency, BillingPeriod), BigDecimal] = Map(
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
      deliveryFrequency: HomeDelivery2025DeliveryFrequency,
      billingPeriod: BillingPeriod
  ): Option[BigDecimal] = {
    newPrices.get((deliveryFrequency, billingPeriod))
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
      priceCap: BigDecimal,
      invoiceList: ZuoraInvoiceList,
  ): Either[Failure, Value] = {
    ???
  }
}
