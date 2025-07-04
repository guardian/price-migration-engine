package pricemigrationengine.migrations
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._
import pricemigrationengine.libs._
import pricemigrationengine.services.Zuora

import java.time.LocalDate
import ujson._
import upickle.default._
import zio.ZIO

sealed trait HomeDelivery2025ProductType
object HomeDelivery2025Voucher extends HomeDelivery2025ProductType
object HomeDelivery2025Subcard extends HomeDelivery2025ProductType
object HomeDelivery2025HomeDelivery2025 extends HomeDelivery2025ProductType

sealed trait HomeDelivery2025PlusType
object HomeDelivery2025EverydayPlus extends HomeDelivery2025PlusType
object HomeDelivery2025SixdayPlus extends HomeDelivery2025PlusType

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

  val pricesHomeDelivery2025EverydayPlus: Map[BillingPeriod, BigDecimal] = Map(
    Monthly -> BigDecimal(83.99),
  )

  val pricesHomeDelivery2025SixdayPlus: Map[BillingPeriod, BigDecimal] = Map(
    Monthly -> BigDecimal(73.99),
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
      productType: HomeDelivery2025ProductType,
      plusType: HomeDelivery2025PlusType,
      billingPeriod: BillingPeriod
  ): Option[BigDecimal] = {
    productType match {
      case HomeDelivery2025Voucher => {
        plusType match {
          case HomeDelivery2025EverydayPlus => pricesVouncherEverydayPlus.get(billingPeriod)
          case HomeDelivery2025SixdayPlus   => pricesVouncherSixdayPlus.get(billingPeriod)
        }
      }
      case HomeDelivery2025Subcard => {
        plusType match {
          case HomeDelivery2025EverydayPlus => pricesSubCardEverydayPlus.get(billingPeriod)
          case HomeDelivery2025SixdayPlus   => pricesSubCardSixdayPlus.get(billingPeriod)
        }
      }
      case HomeDelivery2025HomeDelivery2025 => {
        plusType match {
          case HomeDelivery2025EverydayPlus => pricesHomeDelivery2025EverydayPlus.get(billingPeriod)
          case HomeDelivery2025SixdayPlus   => pricesHomeDelivery2025SixdayPlus.get(billingPeriod)
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
