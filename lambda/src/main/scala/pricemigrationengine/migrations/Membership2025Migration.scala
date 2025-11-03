package pricemigrationengine.migrations
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._
import pricemigrationengine.services.Zuora
import pricemigrationengine.model.BillingPeriod

import java.time.LocalDate
import ujson._

object Membership2025Migration {

  val maxLeadTime = 35
  val minLeadTime = 33

  val priceGridNewPrices: Map[(BillingPeriod, String), BigDecimal] = Map(
    (Monthly, "GBP") -> BigDecimal(10.0),
    (Monthly, "USD") -> BigDecimal(13.0),
    (Monthly, "EUR") -> BigDecimal(12.0),
    (Monthly, "AUD") -> BigDecimal(17.0),
    (Monthly, "CAD") -> BigDecimal(15.0),
    (Monthly, "NZD") -> BigDecimal(17.0),
    (Monthly, "ROW") -> BigDecimal(13.0),
    (Annual, "GBP") -> BigDecimal(100.0),
    (Annual, "USD") -> BigDecimal(129.0),
    (Annual, "EUR") -> BigDecimal(120.0),
    (Annual, "AUD") -> BigDecimal(170.0),
    (Annual, "CAD") -> BigDecimal(150.0),
    (Annual, "NZD") -> BigDecimal(170.0),
    (Annual, "ROW") -> BigDecimal(129.0),
  )

  def brazeName(cohortItem: CohortItem): Option[String] = {
    /*
      - SV_MB_PriceRiseMonthly_2025 (MONTHLY)
        f0ee9579-7f59-41aa-9d4a-41dfc0b4edfa

      - SV_MB_PriceRiseAnnual_2025 (ANNUAL)
        78d377ca-9f32-4ae2-b8da-c5c678fde5b6

      - SV_MB_PriceRiseUSA_2025
        a0c5fba6-0496-443b-ae1c-ea2b5c4135b2

        The US customers canvas, takes priority over the first two
     */

    for {
      billingPeriod <- cohortItem.billingPeriod
      country <- cohortItem.ex_membership2025_country
    } yield {
      (country, billingPeriod) match {
        case ("United States", _) => "SV_MB_PriceRiseUSA_2025"
        case (_, "Month")         => "SV_MB_PriceRiseMonthly_2025"
        case (_, "Annual")        => "SV_MB_PriceRiseAnnual_2025"
        case _ => throw new Exception("[5cd26ca0] unexpected Membership2025 cohort item billing period")
      }
    }
  }

  // -----------------------------------------------------

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
      newPrice <- priceGridNewPrices.get((billingPeriod, currency)).map(logValue("newPrice"))
    } yield PriceData(currency, oldPrice, newPrice, BillingPeriod.toString(billingPeriod))
    priceDataOpt match {
      case Some(pricedata) => Right(pricedata)
      case None            =>
        Left(
          DataExtractionFailure(
            s"[85bebc63] Could not determine PriceData for subscription ${subscription.subscriptionNumber}"
          )
        )
    }
  }

  def decideTargetProductRatePlanId(sourceProductRatePlan: String): String = {
    // This function performs the following mapping:
    //
    // [Non Founder Supporter - monthly] (2c92a0f94c547592014c69f5b0ff4f7e) -> [Supporter - monthly (2023 Price)] (8a1287c586832d250186a2040b1548fe)
    // [Non Founder Supporter - annual]  (2c92a0fb4c5481db014c69f4a1e03bbd) -> [Supporter - monthly (2023 Price)] (8a129ce886834fa90186a20c3ee70b6a)
    // [Supporter - monthly (2023 Price)] -> (itself)
    // [Supporter - annual (2023 Price)]  -> (itself)

    sourceProductRatePlan match {
      case "2c92a0f94c547592014c69f5b0ff4f7e" => "8a1287c586832d250186a2040b1548fe"
      case "2c92a0fb4c5481db014c69f4a1e03bbd" => "8a129ce886834fa90186a20c3ee70b6a"
      case _                                  => sourceProductRatePlan
    }
  }

  def decideTargetProductRatePlanChargeId(targetProductRatePlanId: String): String = {
    // [Supporter - monthly (2023 Price)] (8a1287c586832d250186a2040b1548fe) -> (8a12800986832d1d0186a20bf5136471) # monthly rate plan charge Id
    // [Supporter - annual (2023 Price)]  (8a129ce886834fa90186a20c3ee70b6a) -> (8a129ce886834fa90186a20c3f4f0b6c) # annual rate plan charge Id
    targetProductRatePlanId match {
      case "8a1287c586832d250186a2040b1548fe" => "8a12800986832d1d0186a20bf5136471"
      case "8a129ce886834fa90186a20c3ee70b6a" => "8a129ce886834fa90186a20c3f4f0b6c"
      case _                                  => throw new Exception("[47607ece] How did this happen ? 🤔")
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
        val targetProductRatePlanId = decideTargetProductRatePlanId(ratePlan.productRatePlanId)

        // Here we know that the product only has one charge, so we read it from the first ratePlanCharge
        // With that said, we are going to check a couple of assertions and error if we are not meeting them
        if (ratePlan.ratePlanCharges.size != 1) {
          throw new Exception(
            s"[c5736744] subscription number: ${subscriptionNumber}, active rate plan (id: ${subscriptionRatePlanId}) has more than one charge, which is unexpected for this product"
          )
        }
        val targetProductRatePlanChargeId = decideTargetProductRatePlanChargeId(targetProductRatePlanId)

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
            s"[ee2a0cdb] Could not compute amendmentOrderPayload for subscription ${zuora_subscription.subscriptionNumber}"
          )
        )
    }
  }

}
