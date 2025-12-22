package pricemigrationengine.migrations
import pricemigrationengine.model.{BillingPeriod, ZuoraRatePlan, _}
import pricemigrationengine.services.Zuora

import java.time.LocalDate
import ujson._
import upickle.default._
import zio.ZIO

import scala.math.BigDecimal.RoundingMode

object DigiSubs2025Migration {

  val maxLeadTime = 35
  val minLeadTime = 33

  val priceGrid: Map[(BillingPeriod, String), BigDecimal] = Map(
    // Monthly
    (Monthly, "GBP") -> BigDecimal(18.0),
    (Monthly, "EUR") -> BigDecimal(20.0),
    (Monthly, "USD") -> BigDecimal(28.0),
    (Monthly, "CAD") -> BigDecimal(30.0),
    (Monthly, "AUD") -> BigDecimal(30.0),
    (Monthly, "NZD") -> BigDecimal(30.0),
    // Quarterly
    (Quarterly, "GBP") -> BigDecimal(54.0),
    (Quarterly, "EUR") -> BigDecimal(60.0),
    (Quarterly, "USD") -> BigDecimal(84.0),
    (Quarterly, "CAD") -> BigDecimal(90.0),
    (Quarterly, "AUD") -> BigDecimal(90.0),
    (Quarterly, "NZD") -> BigDecimal(90.0),
    // Annual
    (Annual, "GBP") -> BigDecimal(180.0),
    (Annual, "EUR") -> BigDecimal(200.0),
    (Annual, "USD") -> BigDecimal(280.0),
    (Annual, "CAD") -> BigDecimal(300.0),
    (Annual, "AUD") -> BigDecimal(300.0),
    (Annual, "NZD") -> BigDecimal(300.0),
  )

  def logValue[T](label: String)(value: T): T = {
    println(s"$label: $value")
    value
  }

  def priceData(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
  ): Either[DataExtractionFailure, PriceData] = {
    println(s"[7909e3a4] subscription: ${subscription}")
    println(s"[1f0e0b1a] invoiceList: ${invoiceList}")
    val priceDataOpt = for {
      ratePlan <- SI2025RateplanFromSubAndInvoices
        .determineRatePlan(subscription, invoiceList)
        .map(logValue("ratePlan"))
      currency <- SI2025Extractions.determineCurrency(ratePlan).map(logValue("currency"))
      billingPeriod <- SI2025Extractions.determineBillingPeriod(ratePlan).map(logValue("billingPeriod"))
      oldPrice = logValue("oldPrice")(SI2025Extractions.determineOldPrice(ratePlan))
      newPrice <- priceGrid.get((billingPeriod, currency)).map(logValue("newPrice"))
    } yield PriceData(currency, oldPrice, newPrice, BillingPeriod.toString(billingPeriod))
    priceDataOpt match {
      case Some(pricedata) => Right(pricedata)
      case None            =>
        Left(
          DataExtractionFailure(
            s"[399494ef] Could not determine PriceData for subscription ${subscription.subscriptionNumber}"
          )
        )
    }
  }

  def brazeName(cohortItem: CohortItem): Option[String] = {
    /*
      Canvases:
          For all USD supporters:
              SV_DP_PriceRiseUSAnnuals011225
              c1c16ea3-0290-4bb7-a0cf-57a7ccf174f0

          For non-USD supporters paying monthly and quarterly:
              SV_DPPriceRiseUKROWMonthlies011225
              d2d8f5a3-daec-4ce4-b78c-c367a4736262

          For non-USD supporters paying annually:
              SV_DP_PriceRiseUKROWAnnuals011225
              a6dde8ce-0c38-4b00-844a-b10bed1c6a25
     */

    for {
      billingPeriod <- cohortItem.billingPeriod
      currency <- cohortItem.currency
    } yield {
      (currency, billingPeriod) match {
        case ("USD", _)     => "SV_DP_PriceRiseUSAnnuals011225"
        case (_, "Month")   => "SV_DPPriceRiseUKROWMonthlies011225"
        case (_, "Quarter") => "SV_DPPriceRiseUKROWMonthlies011225"
        case (_, "Annual")  => "SV_DP_PriceRiseUKROWAnnuals011225"
        case _              => throw new Exception(s"[0a2e8eb6] unexpected case, cohort item: ${cohortItem}")
      }
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
            s"[4580e80b] Could not compute amendmentOrderPayload for subscription ${zuora_subscription.subscriptionNumber}"
          )
        )
    }

  }
}
