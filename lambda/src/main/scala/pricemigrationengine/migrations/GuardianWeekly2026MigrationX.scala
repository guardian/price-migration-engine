package pricemigrationengine.migrations
import pricemigrationengine.model.{BillingPeriod, ZuoraRatePlan, _}

import java.time.LocalDate
import ujson._
import upickle.default._

import java.time.format.DateTimeFormatter

object GuardianWeekly2026MigrationX {
  type Currency = String

  val priceGridNewPricesMonthlies: Map[(Currency, PricingLocalisation), BigDecimal] = Map(
    ("GBP", Domestic) -> BigDecimal(17.50),
    ("GBP", RestOfWorld) -> BigDecimal(17.50),
    ("EUR", Domestic) -> BigDecimal(30.50),
    ("USD", RestOfWorld) -> BigDecimal(38.00),
    ("USD", Domestic) -> BigDecimal(33.00),
    ("CAD", Domestic) -> BigDecimal(39.50),
    ("AUD", Domestic) -> BigDecimal(48.00),
    ("NZD", Domestic) -> BigDecimal(60.00),
  )

  val priceGridNewPricesQuarterlies: Map[(Currency, PricingLocalisation), BigDecimal] = Map(
    ("GBP", Domestic) -> BigDecimal(52),
    ("GBP", RestOfWorld) -> BigDecimal(52),
    ("EUR", Domestic) -> BigDecimal(91.5),
    ("USD", RestOfWorld) -> BigDecimal(114),
    ("USD", Domestic) -> BigDecimal(99),
    ("CAD", Domestic) -> BigDecimal(118.5),
    ("AUD", Domestic) -> BigDecimal(144),
    ("NZD", Domestic) -> BigDecimal(180),
  )

  val priceGridNewPricesSemiAnnuals: Map[(Currency, PricingLocalisation), BigDecimal] = Map(
    ("GBP", Domestic) -> BigDecimal(104),
    ("GBP", RestOfWorld) -> BigDecimal(104),
    ("EUR", Domestic) -> BigDecimal(183.0),
    ("USD", RestOfWorld) -> BigDecimal(228),
    ("USD", Domestic) -> BigDecimal(198),
    ("CAD", Domestic) -> BigDecimal(237.0),
    ("AUD", Domestic) -> BigDecimal(288),
    ("NZD", Domestic) -> BigDecimal(360),
  )

  val priceGridNewPricesAnnuals: Map[(Currency, PricingLocalisation), BigDecimal] = Map(
    ("GBP", Domestic) -> BigDecimal(208),
    ("GBP", RestOfWorld) -> BigDecimal(208),
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
      localisation: PricingLocalisation
  ): Option[BigDecimal] = {
    billingPeriod match {
      case Monthly    => priceGridNewPricesMonthlies.get(currency, localisation)
      case Quarterly  => priceGridNewPricesQuarterlies.get(currency, localisation)
      case SemiAnnual => priceGridNewPricesSemiAnnuals.get(currency, localisation)
      case Annual     => priceGridNewPricesAnnuals.get(currency, localisation)
    }
  }

  def getNewPrice(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      account: ZuoraAccount
  ): Option[BigDecimal] = {
    for {
      currencyAndLocalisation <- CurrencyAndLocalisation
        .determineSubscriptionCurrencyAndLocalisation(
          subscription,
          invoiceList,
          account
        )
        .map(logValue("[74e1a4ba] currencyAndLocalisation"))
      ratePlan <- SI2025RateplanFromSubAndInvoices
        .determineRatePlan(subscription, invoiceList)
        .map(logValue("[8ba0a9b7] ratePlan"))
      billingPeriod <- SI2025Extractions.determineBillingPeriod(ratePlan).map(logValue("[e08640ec] billingPeriod"))
      newPrice <- getNewPrice(billingPeriod, currencyAndLocalisation.currency, currencyAndLocalisation.localisation)
        .map(logValue("[ad3a4306] new price"))
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
      cohortSpec: CohortSpec,
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
      commsPrice = logValue("commsPrice")(EstimationHandlerHelper.commsPrice(cohortSpec, oldPrice, newPrice))
    } yield PriceData(currency, oldPrice, newPrice, commsPrice, BillingPeriod.toString(billingPeriod))
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
      zuoraSubscription: ZuoraSubscription,
      commsPrice: BigDecimal,
      invoiceList: ZuoraInvoiceList,
      account: ZuoraAccount
  ): Either[Failure, Value] = {
    // We have two notions of subscription here.
    // There is the Zuora subscription which is one of the arguments, and there is
    // the notion of subscription as defined in the Zuora Order API documentation,
    // which roughly translates to a collections of { actions / atomic mutations } in Zuora

    // For Print 2026 GW sub, we need to check the CohortItem's ex_gw2026_maintain_structure
    // field, to decide whether to perform a restructuration or not.

    if (cohortItem.ex_gw2026_maintain_structure.contains("true")) {
      (for {
        ratePlan <- SI2025RateplanFromSubAndInvoices.determineRatePlan(zuoraSubscription, invoiceList)
        billingPeriod <- ZuoraRatePlan.ratePlanToOptionalUniquelyDeterminedBillingPeriod(ratePlan)
      } yield {
        val subscriptionRatePlanId = ratePlan.id
        val removeProduct = ZuoraOrdersApiPrimitives.removeProduct(effectDate.toString, subscriptionRatePlanId)
        val triggerDateString = effectDate.toString

        // Here we do an "in place" price rise, therefore we are targeting the productRatePlanId that the active rate plan already has
        val targetProductRatePlanId = ratePlan.productRatePlanId

        // Here we know that the product only has one charge, so we read it from the first ratePlanCharge
        // With that said, we are going to check that the number of rate plan charges is what we expect
        if (ratePlan.ratePlanCharges.size != 1) {
          throw new Exception(
            s"[978885ed] subscription number: ${subscriptionNumber}, active rate plan (id: ${subscriptionRatePlanId}) has more than one charge, which is unexpected for this product"
          )
        }

        // We can use .get here because we have checked that we have a rateplan charge
        val targetProductRatePlanChargeId = ratePlan.ratePlanCharges.headOption.get.productRatePlanChargeId

        // We have just one charge for the add product payload fragment
        val chargeOverrides = List(
          ZuoraOrdersApiPrimitives.chargeOverride(
            targetProductRatePlanChargeId,
            commsPrice,
            BillingPeriod.toString(billingPeriod)
          )
        )

        val addProduct =
          ZuoraOrdersApiPrimitives.addProduct(triggerDateString, targetProductRatePlanId, chargeOverrides)

        val orderSubscription =
          ZuoraOrdersApiPrimitives.subscription(subscriptionNumber, List(removeProduct), List(addProduct))

        ZuoraOrdersApiPrimitives.subscriptionUpdatePayload(
          orderDate.toString,
          accountNumber,
          orderSubscription
        )
      }).toRight(
        DataExtractionFailure(
          s"[4580e80b] Could not compute amendmentOrderPayload (maintain structure) for subscription ${zuoraSubscription.subscriptionNumber}"
        )
      )
    } else {
      (for {
        ratePlan <- SI2025RateplanFromSubAndInvoices.determineRatePlan(zuoraSubscription, invoiceList)
        billingPeriod <- ZuoraRatePlan.ratePlanToOptionalUniquelyDeterminedBillingPeriod(ratePlan)
        currency <- SI2025Extractions.determineCurrency(ratePlan)
        currencyAndLocalization <- CurrencyAndLocalisation.determineSubscriptionCurrencyAndLocalisation(
          zuoraSubscription,
          invoiceList,
          account
        )
        distribution <- GuardianWeeklyHelper.subscriptionToFinancePercentageDistribution(
          billingPeriod,
          currencyAndLocalization.currency,
          currencyAndLocalization.localisation
        )
        mapping = GuardianWeeklyHelper.subscriptionToProductRatePlanChargeIdMapping(zuoraSubscription)
        legs <- T6xLegChargeOverride.decideT6xLegChargeOverrides(
          distribution,
          mapping,
          billingPeriod: BillingPeriod,
          commsPrice,
        )
      } yield {
        val subscriptionRatePlanId = ratePlan.id
        val removeProduct = ZuoraOrdersApiPrimitives.removeProduct(effectDate.toString, subscriptionRatePlanId)
        val triggerDateString = effectDate.toString
        val productRatePlanId = ratePlan.productRatePlanId
        val chargeOverrides = ZuoraOrdersApiPrimitives.t6xLegsToChargeOverrides(legs)
        val addProduct = ZuoraOrdersApiPrimitives.addProduct(triggerDateString, productRatePlanId, chargeOverrides)
        val orderSubscription =
          ZuoraOrdersApiPrimitives.subscription(subscriptionNumber, List(removeProduct), List(addProduct))
        ZuoraOrdersApiPrimitives.subscriptionUpdatePayload(
          orderDate.toString,
          accountNumber,
          orderSubscription
        )
      }).toRight(
        DataExtractionFailure(
          s"[64637c14] Could not compute amendmentOrderPayload (retructuration) for subscription ${zuoraSubscription.subscriptionNumber}"
        )
      )
    }
  }
}
