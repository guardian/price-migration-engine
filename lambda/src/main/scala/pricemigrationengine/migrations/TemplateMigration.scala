package pricemigrationengine.migrations

import pricemigrationengine.model._

import java.time.LocalDate
import ujson._

object TemplateMigration {

  // -----------------------------------------------------------
  // Standard variant. Adapt as required.
  // -----------------------------------------------------------

  def priceData(
      cohortSpec: CohortSpec,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList
  ): Either[DataExtractionFailure, PriceData] = {
    val priceDataOpt: Option[PriceData] = for {
      ratePlan <- SI2025RateplanFromSubAndInvoices.determineRatePlan(subscription, invoiceList)
      currency <- SI2025Extractions.determineCurrency(ratePlan)
      oldPrice = SI2025Extractions.determineOldPrice(ratePlan: ZuoraRatePlan)
      newPrice = BigDecimal(
        2.71
      ) // Should replace this by a call to the migration's own `determineNewPrice()` the price grid lookup
      commsPrice = BigDecimal(
        2.55
      ) // should replace this by the value coming from price data
      billingPeriod <- SI2025Extractions.determineBillingPeriod(ratePlan)
    } yield PriceData(currency, oldPrice, newPrice, commsPrice, BillingPeriod.toString(billingPeriod))
    priceDataOpt match {
      case Some(pricedata) => Right(pricedata)
      case None            =>
        Left(
          DataExtractionFailure(s"Could not determine PriceData for subscription ${subscription.subscriptionNumber}")
        )
    }
  }

  // -----------------------------------------------------------
  // Standard variant. We update a single leg (rate plan charge).
  // -----------------------------------------------------------

  def amendmentOrderPayload_v1(
      cohortItem: CohortItem,
      orderDate: LocalDate,
      accountNumber: String,
      subscriptionNumber: String,
      effectDate: LocalDate,
      zuoraSubscription: ZuoraSubscription,
      commsPrice: BigDecimal,
      invoiceList: ZuoraInvoiceList,
  ): Either[Failure, Value] = {

    // We have two notions of subscription here.
    // There is the Zuora subscription which is one of the arguments, and there is
    // the notion of subscription as defined in the Zuora Order API documentation,
    // which roughly translates to a collections of { actions / atomic mutations } in Zuora

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
        s"[4580e80b] Could not compute amendmentOrderPayload for subscription ${zuoraSubscription.subscriptionNumber}"
      )
    )
  }

  // -----------------------------------------------------------
  // Newspaper multi legged Finance driven.
  // 1. Compute the subscription productRatePlanChargeIdMapping with NewspaperHelper.ratePlanToProductRatePlanChargeIdMapping
  // 2. Get the Finance distribution with NewspaperHelper.subscriptionToFinancePercentageDistribution
  // 3. Compute the legs by combining the mapping and the distribution with T6xLegChargeOverride.decideT6xLegChargeOverrides
  // 4. Compute the chargeOverrides with ZuoraOrdersApiPrimitives.t6xLegsToChargeOverrides
  // -----------------------------------------------------------

  def amendmentOrderPayload_v2(
      cohortItem: CohortItem,
      orderDate: LocalDate,
      accountNumber: String,
      subscriptionNumber: String,
      effectDate: LocalDate,
      zuoraSubscription: ZuoraSubscription,
      oldPrice: BigDecimal,
      commsPrice: BigDecimal,
      invoiceList: ZuoraInvoiceList,
  ): Either[Failure, Value] = {
    // This version of `amendmentOrderPayload`, applied to subscriptions with the active rate plan having
    // several charges (one per delivery day), is using ZuoraOrdersApiPrimitives.ratePlanChargesToChargeOverrides
    // which maps the rate plan's rate plan charges to an array of charge overrides json objects.

    (for {
      ratePlan <- SI2025RateplanFromSubAndInvoices.determineRatePlan(zuoraSubscription, invoiceList)
      productRatePlanChargeIdMapping = NewspaperHelper.ratePlanToProductRatePlanChargeIdMapping(ratePlan)
      billingPeriod <- ZuoraRatePlan.ratePlanToOptionalUniquelyDeterminedBillingPeriod(ratePlan)
      distribution <- NewspaperHelper.subscriptionToFinancePercentageDistribution(zuoraSubscription, orderDate).toOption
      legs <- T6xLegChargeOverride.decideT6xLegChargeOverrides(
        distribution,
        productRatePlanChargeIdMapping,
        billingPeriod,
        commsPrice
      )
    } yield {
      val subscriptionRatePlanId = ratePlan.id
      val removeProduct = ZuoraOrdersApiPrimitives.removeProduct(effectDate.toString, subscriptionRatePlanId)
      val triggerDateString = effectDate.toString
      val productRatePlanId = ratePlan.productRatePlanId // We are upgrading on the same rate plan.
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
        s"[9f480e70] Could not compute amendmentOrderPayload for subscription ${zuoraSubscription.subscriptionNumber}"
      )
    )
  }
}
