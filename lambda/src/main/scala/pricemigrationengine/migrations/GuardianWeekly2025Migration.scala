package pricemigrationengine.migrations
import pricemigrationengine.libs.SI2025Extractions
import pricemigrationengine.model.{BillingPeriod, ZuoraRatePlan, _}
import pricemigrationengine.libs._

import java.time.LocalDate
import ujson._
import upickle.default._

import java.time.format.DateTimeFormatter

case class GuardianWeekly2025ExtraAttributes(
    earliestMigrationDate: Option[LocalDate] = None,
    removeDiscount: Option[Boolean] = None
)
object GuardianWeekly2025ExtraAttributes {

  implicit val localDateReader: Reader[LocalDate] =
    readwriter[String].bimap[LocalDate](
      // write (not used here)
      date => date.format(DateTimeFormatter.ISO_LOCAL_DATE),
      // read
      str => LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE)
    )

  implicit val reader: Reader[GuardianWeekly2025ExtraAttributes] = macroR

  // usage
  // val s = """{ "earliestMigrationDate": "2025-10-06" }"""
  // val attributes: GuardianWeekly2025ExtraAttributes = upickle.default.read[GuardianWeekly2025ExtraAttributes](s)
}

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

  def getEarliestMigrationDateFromMigrationExtraAttributes(item: CohortItem): Option[LocalDate] = {
    for {
      attributes <- item.migrationExtraAttributes
      data: GuardianWeekly2025ExtraAttributes =
        upickle.default.read[GuardianWeekly2025ExtraAttributes](attributes)
      date <- data.earliestMigrationDate
    } yield date
  }

  def decideShouldRemoveDiscount(item: CohortItem): Boolean = {
    val flag_opt = (for {
      attributes <- item.migrationExtraAttributes
      data: GuardianWeekly2025ExtraAttributes =
        upickle.default.read[GuardianWeekly2025ExtraAttributes](attributes)
      removeDiscount <- data.removeDiscount
    } yield removeDiscount)
    flag_opt.getOrElse(false)
  }

  def computeStartDateLowerBound4(lowerBound: LocalDate, item: CohortItem): LocalDate = {
    val dateFromCohortItem = getEarliestMigrationDateFromMigrationExtraAttributes(item)
    dateFromCohortItem match {
      case Some(date) => Date.datesMax(lowerBound, date)
      case None       => lowerBound
    }
  }

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

  def subscriptionToLastPriceMigrationDate(
      subscription: ZuoraSubscription,
  ): Option[LocalDate] = {
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

  def logValue[T](label: String)(value: T): T = {
    println(s"$label: $value")
    value
  }

  def priceData(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      account: ZuoraAccount
  ): Either[DataExtractionFailure, PriceData] = {
    val priceDataOpt: Option[PriceData] = for {
      _ <- Some(()).map(logValue("initialization"))
      ratePlan <- SI2025RateplanFromSubAndInvoices
        .determineRatePlan(subscription, invoiceList)
        .map(logValue("ratePlan"))
      currency <- SI2025Extractions
        .determineCurrency(ratePlan)
        .map(logValue("currency"))
      oldPrice = logValue("oldPrice")(SI2025Extractions.determineOldPrice(ratePlan))
      localisation <- SubscriptionLocalisation
        .determineSubscriptionLocalisation(subscription, invoiceList, account)
        .map(logValue("localisation"))
      billingPeriod <- SI2025Extractions
        .determineBillingPeriod(ratePlan)
        .map(logValue("billingPeriod"))
      newPrice <- priceLookUp(
        localisation,
        billingPeriod: BillingPeriod,
        currency: String
      ).map(logValue("newPrice"))
    } yield PriceData(currency, oldPrice, newPrice, BillingPeriod.toString(billingPeriod))
    priceDataOpt match {
      case Some(pricedata) => Right(pricedata)
      case None =>
        Left(
          DataExtractionFailure(s"Could not determine PriceData for subscription ${subscription.subscriptionNumber}")
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

    // We have two notions of subscription here.
    // There is the Zuora subscription which is one of the arguments, and there is
    // the notion of subscription as defined in the Zuora Order API documentation,
    // which roughly translates to a collections of { actions / atomic mutations } in Zuora

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
          val chargeOverrides = List(
            ZuoraOrdersApiPrimitives.chargeOverride(
              ratePlan.ratePlanCharges.headOption.get.productRatePlanChargeId,
              PriceCap.cappedPrice(oldPrice, estimatedNewPrice, priceCap),
              BillingPeriod.toString(billingPeriod)
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
      } else {
        for {
          ratePlan <- SI2025RateplanFromSubAndInvoices.determineRatePlan(zuora_subscription, invoiceList)
          billingPeriod <- ZuoraRatePlan.ratePlanToBillingPeriod(ratePlan)
          discount <- SI2025Extractions.getDiscountByRatePlanName(zuora_subscription, "Percentage")
        } yield {
          val subscriptionRatePlanId = ratePlan.id
          val removeProduct = ZuoraOrdersApiPrimitives.removeProduct(effectDate.toString, subscriptionRatePlanId)
          val removeDiscount = ZuoraOrdersApiPrimitives.removeProduct(effectDate.toString, discount.id)
          val triggerDateString = effectDate.toString
          val productRatePlanId = ratePlan.productRatePlanId // We are upgrading on the same rate plan.
          val chargeOverrides = List(
            ZuoraOrdersApiPrimitives.chargeOverride(
              ratePlan.ratePlanCharges.headOption.get.productRatePlanChargeId,
              PriceCap.cappedPrice(oldPrice, estimatedNewPrice, priceCap),
              BillingPeriod.toString(billingPeriod)
            )
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
            s"[e5bd98d7] Could not compute amendmentOrderPayload for subscription ${zuora_subscription.subscriptionNumber}"
          )
        )
    }
  }
}
