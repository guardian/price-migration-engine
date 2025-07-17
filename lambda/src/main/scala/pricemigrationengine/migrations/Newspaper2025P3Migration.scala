package pricemigrationengine.migrations
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._
import pricemigrationengine.libs._
import pricemigrationengine.services.Zuora

import java.time.LocalDate
import ujson._
import upickle.default._
import zio.ZIO

sealed trait Newspaper2025P3DeliveryPattern
object Newspaper2025P3Everyday extends Newspaper2025P3DeliveryPattern
object Newspaper2025P3Sixday extends Newspaper2025P3DeliveryPattern
object Newspaper2025P3Weekend extends Newspaper2025P3DeliveryPattern
object Newspaper2025P3Saturday extends Newspaper2025P3DeliveryPattern

case class Newspaper2025P3ExtraAttributes(brandTitle: String, removeDiscount: Option[Boolean] = None)
object Newspaper2025P3ExtraAttributes {
  implicit val reader: Reader[Newspaper2025P3ExtraAttributes] = macroR

  // Each item of the migration is going to have a migration extended attributes object
  // with a brandTitle key and possibly a removeDiscount key.
  //
  // The value of the `brandTitle` key is to be sent to Braze, during the
  // notification handler to decide the labelling to the entity in the email. At that point the
  // attribute will be called `newspaper2025_phase3_brand_title`

  // usage:
  // val s = """{ "brandTitle": "the Guardian" }"""
  // val s = """{ "brandTitle": "the Guardian and the Observer" }"""
  // val s = """{ "brandTitle": "the Guardian", "removeDiscount": true }"""
  // val attributes: Newspaper2025P3ExtraAttributes = upickle.default.read[Newspaper2025P3ExtraAttributes](s)
}

case class Newspaper2025P3NotificationData(
    brandTitle: String,
)

object Newspaper2025P3Migration {

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

  val newPrices: Map[(Newspaper2025P3DeliveryPattern, BillingPeriod), BigDecimal] = Map(
    // Everyday
    (Newspaper2025P3Everyday, Monthly) -> BigDecimal(69.99),
    (Newspaper2025P3Everyday, Quarterly) -> BigDecimal(209.97),
    (Newspaper2025P3Everyday, SemiAnnual) -> BigDecimal(419.94),
    (Newspaper2025P3Everyday, Annual) -> BigDecimal(839.88),
    // Sixday
    (Newspaper2025P3Sixday, Monthly) -> BigDecimal(61.99),
    (Newspaper2025P3Sixday, Quarterly) -> BigDecimal(185.97),
    (Newspaper2025P3Sixday, SemiAnnual) -> BigDecimal(371.94),
    (Newspaper2025P3Sixday, Annual) -> BigDecimal(743.88),
    // Weekend
    (Newspaper2025P3Weekend, Monthly) -> BigDecimal(27.99),
    (Newspaper2025P3Weekend, Quarterly) -> BigDecimal(83.97),
    (Newspaper2025P3Weekend, SemiAnnual) -> BigDecimal(167.94),
    (Newspaper2025P3Weekend, Annual) -> BigDecimal(335.88),
    // Saturday
    (Newspaper2025P3Saturday, Monthly) -> BigDecimal(15.99),
    (Newspaper2025P3Saturday, Quarterly) -> BigDecimal(47.97),
    (Newspaper2025P3Saturday, SemiAnnual) -> BigDecimal(95.94),
    (Newspaper2025P3Saturday, Annual) -> BigDecimal(191.88),
  )

  // ------------------------------------------------
  // Helpers
  // ------------------------------------------------

  // (Comment Group: 571dac68)

  def getLabelFromMigrationExtraAttributes(item: CohortItem): Option[String] = {
    for {
      attributes <- item.migrationExtraAttributes
    } yield {
      val data: Newspaper2025P3ExtraAttributes =
        upickle.default.read[Newspaper2025P3ExtraAttributes](attributes)
      data.brandTitle
    }
  }

  def decideShouldRemoveDiscount(item: CohortItem): Boolean = {
    val flag_opt = (for {
      attributes <- item.migrationExtraAttributes
      data: Newspaper2025P3ExtraAttributes =
        upickle.default.read[Newspaper2025P3ExtraAttributes](attributes)
      removeDiscount <- data.removeDiscount
    } yield removeDiscount)
    flag_opt.getOrElse(false)
  }

  def getNotificationData(
      cohortSpec: CohortSpec,
      item: CohortItem
  ): ZIO[Zuora, Failure, Newspaper2025P3NotificationData] = {
    MigrationType(cohortSpec) match {
      case Newspaper2025P1 => {
        (for {
          brandTitle <- getLabelFromMigrationExtraAttributes(item)
        } yield Newspaper2025P3NotificationData(
          brandTitle
        )) match {
          case None =>
            ZIO.fail(
              DataExtractionFailure(
                s"Could not build Newspaper2025P3NotificationData for item ${item.toString}"
              )
            )
          case Some(d) => ZIO.succeed(d)
        }
      }
      case _ => ZIO.succeed(Newspaper2025P3NotificationData(""))
    }
  }

  def priceLookUp(
      deliveryPattern: Newspaper2025P3DeliveryPattern,
      billingPeriod: BillingPeriod
  ): Option[BigDecimal] = {
    newPrices.get((deliveryPattern, billingPeriod))
  }

  def subscriptionToLastPriceMigrationDate(subscription: ZuoraSubscription): Option[LocalDate] = {
    for {
      ratePlan <- SI2025RateplanFromSub.determineRatePlan(subscription)
      date <- SI2025Extractions.determineLastPriceMigrationDate(ratePlan)
    } yield date
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
      cohortItem: CohortItem,
      orderDate: LocalDate,
      accountNumber: String,
      subscriptionNumber: String,
      effectDate: LocalDate,
      zuora_subscription: ZuoraSubscription,
      oldPrice: BigDecimal,
      estimatedNewPrice: BigDecimal,
      priceCap: BigDecimal,
      invoiceList: ZuoraInvoiceList,
  ): Either[Failure, Value] = {
    ???
  }
}
