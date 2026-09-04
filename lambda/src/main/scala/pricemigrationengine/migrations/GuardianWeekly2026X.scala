package pricemigrationengine.migrations
import pricemigrationengine.model.{BillingPeriod, ZuoraRatePlan, _}

import java.time.LocalDate
import ujson._
import upickle.default._

import java.time.format.DateTimeFormatter

object GuardianWeekly2026X {
  type Currency = String

  val priceGridNewPricesMonthlies: Map[(Currency, SubscriptionLocalisation), BigDecimal] = Map(
    ("GBP", Domestic) -> BigDecimal(17.50),
    ("EUR", Domestic) -> BigDecimal(30.50),
    ("USD", RestOfWorld) -> BigDecimal(38.00),
    ("USD", Domestic) -> BigDecimal(33.00),
    ("CAD", Domestic) -> BigDecimal(39.50),
    ("AUD", Domestic) -> BigDecimal(48.00),
    ("NZD", Domestic) -> BigDecimal(60.00),
  )

  val priceGridNewPricesQuarterlies: Map[(Currency, SubscriptionLocalisation), BigDecimal] = Map(
    ("GBP", Domestic) -> BigDecimal(52),
    ("EUR", Domestic) -> BigDecimal(91.5),
    ("USD", RestOfWorld) -> BigDecimal(114),
    ("USD", Domestic) -> BigDecimal(99),
    ("CAD", Domestic) -> BigDecimal(118.5),
    ("AUD", Domestic) -> BigDecimal(144),
    ("NZD", Domestic) -> BigDecimal(180),
  )

  val priceGridNewPricesAnnuals: Map[(Currency, SubscriptionLocalisation), BigDecimal] = Map(
    ("GBP", Domestic) -> BigDecimal(208),
    ("EUR", Domestic) -> BigDecimal(366),
    ("USD", RestOfWorld) -> BigDecimal(456),
    ("USD", Domestic) -> BigDecimal(396),
    ("CAD", Domestic) -> BigDecimal(474.0),
    ("AUD", Domestic) -> BigDecimal(576),
    ("NZD", Domestic) -> BigDecimal(720),
  )

  def getNewPrice(
      billingPeriod: BillingPeriod,
      currency: Currency,
      localisation: SubscriptionLocalisation
  ): Option[BigDecimal] = {
    billingPeriod match {
      case Monthly    => priceGridNewPricesMonthlies.get(currency, localisation)
      case Quarterly  => priceGridNewPricesQuarterlies.get(currency, localisation)
      case SemiAnnual => None
      case Annual     => priceGridNewPricesAnnuals.get(currency, localisation)
    }
  }

  def getNewPrice(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      account: ZuoraAccount
  ): Option[BigDecimal] = {
    for {
      currencyAndLocalisation <- CurrencyAndLocalisation.determineSubscriptionCurrencyAndLocalisation(
        subscription,
        invoiceList,
        account
      )
      ratePlan <- SI2025RateplanFromSubAndInvoices.determineRatePlan(subscription, invoiceList)
      billingPeriod <- SI2025Extractions.determineBillingPeriod(ratePlan)
      newPrice <- getNewPrice(billingPeriod, currencyAndLocalisation.currency, currencyAndLocalisation.localisation)
    } yield newPrice
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
      billingPeriod <- SI2025Extractions
        .determineBillingPeriod(ratePlan)
        .map(logValue("billingPeriod"))
      newPrice <- getNewPrice(
        subscription,
        invoiceList,
        account
      ).map(logValue("newPrice"))
    } yield PriceData(currency, oldPrice, newPrice, BillingPeriod.toString(billingPeriod))
    priceDataOpt match {
      case Some(pricedata) => Right(pricedata)
      case None            =>
        Left(
          DataExtractionFailure(
            s"[38fed0ce] could not determine PriceData for subscription ${subscription.subscriptionNumber}"
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
      commsPrice: BigDecimal,
      invoiceList: ZuoraInvoiceList,
  ): Either[Failure, Value] = {
    // We have two notions of subscription here.
    // There is the Zuora subscription which is one of the arguments, and there is
    // the notion of subscription as defined in the Zuora Order API documentation,
    // which roughly translates to a collections of { actions / atomic mutations } in Zuora

    val order_opt = {
      for {
        ratePlan <- SI2025RateplanFromSubAndInvoices.determineRatePlan(zuora_subscription, invoiceList)
        billingPeriod <- ZuoraRatePlan.ratePlanToOptionalUniquelyDeterminedBillingPeriod(ratePlan)
      } yield {
        val subscriptionRatePlanId = ratePlan.id
        val removeProduct = ZuoraOrdersApiPrimitives.removeProduct(effectDate.toString, subscriptionRatePlanId)
        val triggerDateString = effectDate.toString
        val productRatePlanId = ratePlan.productRatePlanId
        val chargeOverrides = List(
          ZuoraOrdersApiPrimitives.chargeOverride(
            ratePlan.ratePlanCharges.headOption.get.productRatePlanChargeId,
            commsPrice,
            BillingPeriod.toString(billingPeriod)
          )
        )
        val addProduct = ZuoraOrdersApiPrimitives.addProduct(triggerDateString, productRatePlanId, chargeOverrides)
        val order_subscription =
          ZuoraOrdersApiPrimitives.subscription(subscriptionNumber, List(removeProduct), List(addProduct))
        ZuoraOrdersApiPrimitives.subscriptionUpdatePayload(
          orderDate.toString,
          accountNumber,
          order_subscription
        )
      }
    }

    order_opt match {
      case Some(order) => Right(order)
      case None        =>
        Left(
          DataExtractionFailure(
            s"[1cbce53d] Could not compute amendmentOrderPayload for subscription ${zuora_subscription.subscriptionNumber}"
          )
        )
    }
  }
}
