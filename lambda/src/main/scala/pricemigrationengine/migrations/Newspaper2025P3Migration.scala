package pricemigrationengine.migrations
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._
import pricemigrationengine.libs._
import pricemigrationengine.services.Zuora

import java.time.LocalDate
import ujson._
import upickle.default._
import zio.ZIO

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

  //

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
      productType: Newspaper2025P1ProductType,
      plusType: Newspaper2025P1PlusType,
      billingPeriod: BillingPeriod
  ): Option[BigDecimal] = {
    ???
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
