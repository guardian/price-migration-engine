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

case class Newspaper2025ExtraAttributes(brandTitle: String, removeDiscount: Option[Boolean] = None)
object Newspaper2025ExtraAttributes {
  implicit val reader: Reader[Newspaper2025ExtraAttributes] = macroR

  // Each item of the migration is going to have a migration extended attributes object
  // with a brandTitle key and possibly a removeDiscount key.
  //
  // The value of the `brandTitle` key is to be sent to Braze, during the
  // notification handler to decide the labelling to the entity in the email. At that point the
  // attribute will be called `newspaper2025_brand_title`

  // usage:
  // val s = """{ "brandTitle": "the Guardian" }"""
  // val s = """{ "brandTitle": "the Guardian and the Observer" }"""
  // val s = """{ "brandTitle": "the Guardian", "removeDiscount": true }"""
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

  // (Comment Group: 571dac68)

  def getLabelFromMigrationExtraAttributes(item: CohortItem): Option[String] = {
    for {
      attributes <- item.migrationExtraAttributes
    } yield {
      val data: Newspaper2025ExtraAttributes =
        upickle.default.read[Newspaper2025ExtraAttributes](attributes)
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
    for {
      ratePlan <- SI2025RateplanFromSub.determineRatePlan(subscription)
      date <- SI2025Extractions.determineLastPriceMigrationDate(ratePlan)
    } yield date
  }

  def decideProductType(ratePlan: ZuoraRatePlan): Option[Newspaper2025P1ProductType] = {
    // I have checked every subscription and the only product names that come up are
    // Newspaper Voucher
    // Newspaper Digital Voucher
    // Newspaper Delivery
    // And I checked with Marketing that the below mapping is correct and in particular there doesn't
    // seem to be any subscription that uses the [2025 - Price Grid - Sub Card] part of the pricing grid,
    // what would map to Newspaper2025P1Subcard
    ratePlan.productName match {
      case "Newspaper Voucher"         => Some(Newspaper2025P1Voucher) // confirmed
      case "Newspaper Digital Voucher" => Some(Newspaper2025P1Voucher) // confirmed
      case "Newspaper Delivery"        => Some(Newspaper2025P1HomeDelivery) // confirmed
      case _                           => None
    }
  }

  def decidePlusType(ratePlan: ZuoraRatePlan): Option[Newspaper2025P1PlusType] = {
    ratePlan.ratePlanName match {
      case "Sixday+"   => Some(Newspaper2025P1SixdayPlus)
      case "Everyday+" => Some(Newspaper2025P1EverydayPlus)
      case _           => None
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
      productType <- decideProductType(ratePlan)
      plusType <- decidePlusType(ratePlan)
      newPrice <- priceLookUp(productType, plusType, billingPeriod)
    } yield PriceData(currency, oldPrice, newPrice, BillingPeriod.toString(billingPeriod))
    priceDataOpt match {
      case Some(pricedata) => Right(pricedata)
      case None =>
        Left(
          DataExtractionFailure(
            s"[d2307855] Could not determine PriceData for subscription ${subscription.subscriptionNumber}"
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
      estimatedNewPrice: BigDecimal,
      priceCap: BigDecimal,
      invoiceList: ZuoraInvoiceList,
  ): Either[Failure, Value] = {

    val order_opt = for {
      ratePlan <- SI2025RateplanFromSubAndInvoices.determineRatePlan(zuora_subscription, invoiceList)
    } yield {
      val subscriptionRatePlanId = ratePlan.id
      val removeProduct = ZuoraOrdersApiPrimitives.removeProduct(effectDate.toString, subscriptionRatePlanId)
      val triggerDateString = effectDate.toString
      val productRatePlanId = ratePlan.productRatePlanId // We are upgrading on the same rate plan.
      val chargeOverrides = List(
        ZuoraOrdersApiPrimitives.chargeOverride(
          ratePlan.ratePlanCharges.headOption.get.productRatePlanChargeId,
          PriceCap.cappedPrice(oldPrice, estimatedNewPrice, priceCap)
        )
      )
      val addProduct = ZuoraOrdersApiPrimitives.addProduct(triggerDateString, productRatePlanId, chargeOverrides)
      val order_subscription =
        ZuoraOrdersApiPrimitives.subscription(subscriptionNumber, List(removeProduct), List(addProduct))
      ZuoraOrdersApiPrimitives.replace_a_product_in_a_subscription(
        orderDate.toString,
        accountNumber,
        order_subscription
      )
    }

    order_opt match {
      case Some(order) => Right(order)
      case None =>
        Left(
          DataExtractionFailure(
            s"[4f62efe5] Could not compute amendmentOrderPayload for subscription ${zuora_subscription.subscriptionNumber}"
          )
        )
    }

  }
}
