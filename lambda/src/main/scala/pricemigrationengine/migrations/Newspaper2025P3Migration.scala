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

case class Newspaper2025P3ExtraAttributes(
    brandTitle: String,
    removeDiscount: Option[Boolean] = None,
    earliestMigrationDate: Option[LocalDate] = None
)
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
  // val s = """{ "brandTitle": "the Guardian", "earliestMigrationDate": "2025-10-06" }"""
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

  def getEarliestMigrationDateFromMigrationExtraAttributes(item: CohortItem): Option[LocalDate] = {
    for {
      attributes <- item.migrationExtraAttributes
      data: Newspaper2025P3ExtraAttributes =
        upickle.default.read[Newspaper2025P3ExtraAttributes](attributes)
      date <- data.earliestMigrationDate
    } yield date
  }

  def computeStartDateLowerBound4(lowerBound: LocalDate, item: CohortItem): LocalDate = {
    val dateFromCohortItem = getEarliestMigrationDateFromMigrationExtraAttributes(item)
    dateFromCohortItem match {
      case Some(date) => Date.datesMax(lowerBound, date)
      case None       => lowerBound
    }
  }

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
      case Newspaper2025P3 => {
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

  def decideDeliveryPattern(ratePlan: ZuoraRatePlan): Option[Newspaper2025P3DeliveryPattern] = {
    ratePlan.ratePlanName.trim match {
      case "Everyday" => Some(Newspaper2025P3Everyday)
      case "Weekend"  => Some(Newspaper2025P3Weekend)
      case "Sixday"   => Some(Newspaper2025P3Sixday)
      case "Saturday" => Some(Newspaper2025P3Saturday)
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
            s"[a149987a] Could not determine PriceData for subscription ${subscription.subscriptionNumber}"
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

    // This version of `amendmentOrderPayload`, applied to subscriptions with the active rate plan having
    // several charges (one per delivery day), is using ZuoraOrdersApiPrimitives.ratePlanChargesToChargeOverrides
    // which maps the rate plan's rate plan charges to an array of charge overrides json objects.

    // The important preliminary here, which wasn't needed in the simpler case of a single rate plan charge
    // in the case of GuardianWeekly2025, for instance, is the price ratio from the old price to the new price
    // (both carried by the cohort item).

    // Note that we do use `get` here. The cohort items always get them from the estimation step, but in the
    // abnormal case it would not, we want the process to error and alarm.
    val priceRatio = estimatedNewPrice / oldPrice

    val order_opt = {
      if (!decideShouldRemoveDiscount(cohortItem)) {
        for {
          ratePlan <- SI2025RateplanFromSubAndInvoices.determineRatePlan(zuora_subscription, invoiceList)
          billingPeriod <- ZuoraRatePlan.ratePlanToBillingPeriod(ratePlan)
        } yield {
          val subscriptionRatePlanId = ratePlan.id
          val removeProduct = ZuoraOrdersApiPrimitives.removeProduct(effectDate.toString, subscriptionRatePlanId)
          val triggerDateString = effectDate.toString
          val productRatePlanId = ratePlan.productRatePlanId // We are upgrading on the same rate plan.
          val chargeOverrides: List[Value] = ZuoraOrdersApiPrimitives.ratePlanChargesToChargeOverrides(
            ratePlan.ratePlanCharges,
            priceRatio,
            BillingPeriod.toString(billingPeriod)
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
      } else {
        for {
          ratePlan <- SI2025RateplanFromSubAndInvoices.determineRatePlan(zuora_subscription, invoiceList)
          billingPeriod <- ZuoraRatePlan.ratePlanToBillingPeriod(ratePlan)
          discount <- SI2025Extractions.getPercentageOrAdjustementDiscount(zuora_subscription)
        } yield {
          val subscriptionRatePlanId = ratePlan.id
          val removeProduct = ZuoraOrdersApiPrimitives.removeProduct(effectDate.toString, subscriptionRatePlanId)
          val removeDiscount = ZuoraOrdersApiPrimitives.removeProduct(effectDate.toString, discount.id)
          val triggerDateString = effectDate.toString
          val productRatePlanId = ratePlan.productRatePlanId // We are upgrading on the same rate plan.
          val chargeOverrides: List[Value] = ZuoraOrdersApiPrimitives.ratePlanChargesToChargeOverrides(
            ratePlan.ratePlanCharges,
            priceRatio,
            BillingPeriod.toString(billingPeriod)
          )
          val addProduct = ZuoraOrdersApiPrimitives.addProduct(triggerDateString, productRatePlanId, chargeOverrides)
          val order_subscription =
            ZuoraOrdersApiPrimitives.subscription(
              subscriptionNumber,
              List(removeProduct, removeDiscount),
              List(addProduct)
            )
          ZuoraOrdersApiPrimitives.replace_a_product_in_a_subscription(
            orderDate.toString,
            accountNumber,
            order_subscription
          )
        }
      }
    }

    order_opt match {
      case Some(order) => Right(order)
      case None =>
        Left(
          DataExtractionFailure(
            s"[9f480e70] Could not compute amendmentOrderPayload for subscription ${zuora_subscription.subscriptionNumber}"
          )
        )
    }
  }
}
