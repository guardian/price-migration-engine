package pricemigrationengine.migrations
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._
import pricemigrationengine.libs._
import pricemigrationengine.services.Zuora

import java.time.LocalDate
import ujson._
import upickle.default._
import zio.ZIO

sealed trait Newspaper2025P1ProductType
object Newspaper2025P1Voucher extends Newspaper2025P1ProductType
object Newspaper2025P1Subcard extends Newspaper2025P1ProductType
object Newspaper2025P1HomeDelivery extends Newspaper2025P1ProductType

sealed trait Newspaper2025P1PlusType
object Newspaper2025P1EverydayPlus extends Newspaper2025P1PlusType
object Newspaper2025P1SixdayPlus extends Newspaper2025P1PlusType

case class Newspaper2025ExtraAttributes(brandTitle: String)
object Newspaper2025ExtraAttributes {
  implicit val reader: Reader[Newspaper2025ExtraAttributes] = macroR

  // Each item of the migration is going to have a migration extended attributes object
  // with a brandTitle key. The value of this key is to be sent to Braze, during the
  // notification handler to decide the labelling to the entity in the email. At that point the
  // attribute will be called `brand_title`

  // usage
  // val s = """{ "brandTitle": "the Guardian" }"""
  // val s = """{ "brandTitle": "the Guardian and the Observer" }"""
  // val attributes: Newspaper2025ExtraAttributes = upickle.default.read[Newspaper2025ExtraAttributes](s)
}

// (Comment Group: 571dac68)

case class Newspaper2025P1NotificationData(
    brandTitle: String,
)

object Newspaper2025P1Migration {

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

  def getLabelFromMigrationExtraAttributes(item: CohortItem): Option[String] = {
    for {
      attributes <- item.migrationExtraAttributes
    } yield {
      val data: Newspaper2025ExtraAttributes =
        upickle.default.read[Newspaper2025ExtraAttributes](attributes)
      data.brandTitle
    }
  }

  // (Comment Group: 571dac68)

  def getNotificationData(
      cohortSpec: CohortSpec,
      item: CohortItem
  ): ZIO[Zuora, Failure, Newspaper2025P1NotificationData] = {
    MigrationType(cohortSpec) match {
      case Newspaper2025P1 => {
        (for {
          brandTitle <- getLabelFromMigrationExtraAttributes(item)
        } yield Newspaper2025P1NotificationData(
          brandTitle
        )) match {
          case None =>
            ZIO.fail(
              DataExtractionFailure(
                s"Could not build Newspaper2025P1NotificationData for item ${item.toString}"
              )
            )
          case Some(d) => ZIO.succeed(d)
        }
      }
      case _ => ZIO.succeed(Newspaper2025P1NotificationData(""))
    }
  }

  def priceLookUp(
      productType: Newspaper2025P1ProductType,
      plusType: Newspaper2025P1PlusType,
      billingPeriod: BillingPeriod
  ): Option[BigDecimal] = {
    productType match {
      case Newspaper2025P1Voucher => {
        plusType match {
          case Newspaper2025P1EverydayPlus => pricesVouncherEverydayPlus.get(billingPeriod)
          case Newspaper2025P1SixdayPlus   => pricesVouncherSixdayPlus.get(billingPeriod)
        }
      }
      case Newspaper2025P1Subcard => {
        plusType match {
          case Newspaper2025P1EverydayPlus => pricesSubCardEverydayPlus.get(billingPeriod)
          case Newspaper2025P1SixdayPlus   => pricesSubCardSixdayPlus.get(billingPeriod)
        }
      }
      case Newspaper2025P1HomeDelivery => {
        plusType match {
          case Newspaper2025P1EverydayPlus => pricesHomeDeliveryEverydayPlus.get(billingPeriod)
          case Newspaper2025P1SixdayPlus   => pricesHomeDeliverySixdayPlus.get(billingPeriod)
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
      priceCap: BigDecimal,
      invoiceList: ZuoraInvoiceList,
  ): Either[Failure, Value] = {
    ???
  }
}
