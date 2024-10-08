package pricemigrationengine.migrations
import pricemigrationengine.model._

import java.time.LocalDate

object DigiSubs2023Migration {

  val maxLeadTime = 33
  val minLeadTime = 31

  // -------------------------------------------
  // Support Data
  // -------------------------------------------

  case class TargetRatePlanDetails(ratePlanId: String, ratePlanChargeId: String)

  private val priceMapMonthlies: Map[Currency, BigDecimal] = Map(
    "GBP" -> BigDecimal(14.99),
    "USD" -> BigDecimal(24.99),
    "EUR" -> BigDecimal(18.99),
    "CAD" -> BigDecimal(27.44),
    "AUD" -> BigDecimal(26.99),
    "NZD" -> BigDecimal(26.99),
  )

  private val priceMapQuarterlies: Map[Currency, BigDecimal] = Map(
    "GBP" -> BigDecimal(44.94),
    "USD" -> BigDecimal(74.94),
    "EUR" -> BigDecimal(56.19),
    "CAD" -> BigDecimal(82.31),
    "AUD" -> BigDecimal(79.99),
    "NZD" -> BigDecimal(79.99),
  )

  private val priceMapAnnuals: Map[Currency, BigDecimal] = Map(
    "GBP" -> BigDecimal(149),
    "USD" -> BigDecimal(249),
    "EUR" -> BigDecimal(187),
    "CAD" -> BigDecimal(274),
    "AUD" -> BigDecimal(269.99),
    "NZD" -> BigDecimal(269.99),
  )

  private val billingPeriodToRatePlanId: Map[BillingPeriod, TargetRatePlanDetails] = Map(
    Monthly -> TargetRatePlanDetails(
      "2c92a0fb4edd70c8014edeaa4eae220a",
      "2c92a0fb4edd70c9014edeaa50342192"
    ), // Digital Pack Monthly (rate plan) (old)
    Quarterly -> TargetRatePlanDetails(
      "2c92a0fb4edd70c8014edeaa4e8521fe",
      "2c92a0fb4edd70c9014edeaa4fd42186"
    ), // Digital Pack Quarterly (rate plan) (old)
    Annual -> TargetRatePlanDetails(
      "2c92a0fb4edd70c8014edeaa4e972204",
      "2c92a0fb4edd70c9014edeaa5001218c"
    ), // Digital Pack Annual (rate plan) (old)
  )

  // -------------------------------------------
  // Support Functions
  // -------------------------------------------

  def newPriceLookup(
      currency: String,
      billingPeriod: BillingPeriod
  ): Either[DataExtractionFailure, BigDecimal] = {
    billingPeriod match {
      case Monthly =>
        priceMapMonthlies.get(currency) match {
          case None => Left(DataExtractionFailure(s"Could not determine a new monthly price for currency: ${currency}"))
          case Some(price) => Right(price)
        }
      case Quarterly =>
        priceMapQuarterlies.get(currency) match {
          case None =>
            Left(DataExtractionFailure(s"Could not determine a new quarterly price for currency: ${currency}"))
          case Some(price) => Right(price)
        }
      case Annual =>
        priceMapAnnuals.get(currency) match {
          case None => Left(DataExtractionFailure(s"Could not determine a new annual price for currency: ${currency}"))
          case Some(price) => Right(price)
        }
      case SemiAnnual => Left(DataExtractionFailure(s"There are no defined semi-annual prices for this migration"))
    }
  }

  def subscriptionRatePlan(subscription: ZuoraSubscription): Either[DataExtractionFailure, ZuoraRatePlan] = {
    // Takes a subscription and return the active rate plan
    // This function is specific to the current Migration, eg: DigiSubs2023, so we can be effective in the look up of
    // that rate plan
    subscription.ratePlans.find(_.productName == "Digital Pack") match {
      case None =>
        Left(DataExtractionFailure(s"Subscription ${subscription.subscriptionNumber} doesn't have any rate plan"))
      case Some(ratePlan) => Right(ratePlan)
    }
  }

  def subscriptionIsDiscounted(subscription: ZuoraSubscription): Boolean = {
    subscription.ratePlans.exists(rp => rp.productName == "Discounts")
  }

  def subscriptionRatePlanCharge(
      subscription: ZuoraSubscription,
      ratePlan: ZuoraRatePlan
  ): Either[DataExtractionFailure, ZuoraRatePlanCharge] = {
    // Takes a rate plan and return the active rate plan charges
    // Since it's migration specific we can be effective in the look up
    // Note that we also pass the subscription to the function in case we need to return a Failure object
    ratePlan.ratePlanCharges.headOption match {
      case None => {
        // Although not enforced by the signature of the function, for this error message to make sense we expect that
        // the rate plan belongs to the currency
        Left(
          DataExtractionFailure(s"Subscription ${subscription.subscriptionNumber} has a rate plan, but with no charge")
        )
      }
      case Some(ratePlanCharge) => Right(ratePlanCharge)
    }
  }

  def getPriceFromRatePlanCharge(
      subscription: ZuoraSubscription,
      ratePlanCharge: ZuoraRatePlanCharge
  ): Either[DataExtractionFailure, BigDecimal] = {
    // This function takes a rate plan charge and returns the price
    // We also pass the subscription in case we need to return a Failure object
    ratePlanCharge.price match {
      case None => {
        Left(
          DataExtractionFailure(
            s"Subscription ${subscription.subscriptionNumber} has a rate plan charge, but with no currency"
          )
        )
      }
      case Some(price) => Right(price)
    }
  }

  def getBillingPeriodFromRatePlanCharge(
      subscription: ZuoraSubscription,
      ratePlanCharge: ZuoraRatePlanCharge
  ): Either[DataExtractionFailure, BillingPeriod] = {
    // This function takes a rate plan charge and returns the price
    // We also pass the subscription in case we need to return a Failure object
    ratePlanCharge.billingPeriod match {
      case None => {
        Left(
          DataExtractionFailure(
            s"Subscription ${subscription.subscriptionNumber} has a rate plan charge, but with no currency"
          )
        )
      }
      case Some(period) => Right(BillingPeriod.fromString(period))
    }
  }

  def getBillingPeriodFromSubscription(
      subscription: ZuoraSubscription
  ): Either[DataExtractionFailure, BillingPeriod] = {
    for {
      ratePlan <- subscriptionRatePlan(subscription)
      ratePlanCharge <- subscriptionRatePlanCharge(subscription, ratePlan)
      billingPeriod <- getBillingPeriodFromRatePlanCharge(subscription, ratePlanCharge)
    } yield billingPeriod
  }

  // -------------------------------------------
  // Main Functions
  // -------------------------------------------

  def priceData(
      subscription: ZuoraSubscription,
  ): Either[DataExtractionFailure, PriceData] = {

    // This function computes a
    // PriceData(currency: Currency, oldPrice: BigDecimal, newPrice: BigDecimal, billingPeriod: String)
    // for a subscription

    for {
      ratePlan <- subscriptionRatePlan(subscription)
      ratePlanCharge <- subscriptionRatePlanCharge(subscription, ratePlan)
      currency = ratePlanCharge.currency
      billingPeriod <- getBillingPeriodFromRatePlanCharge(subscription, ratePlanCharge)
      oldPrice <- getPriceFromRatePlanCharge(subscription, ratePlanCharge)
      newPrice <- newPriceLookup(currency, billingPeriod)
    } yield PriceData(currency, oldPrice, newPrice, BillingPeriod.toString(billingPeriod))
  }

  def zuoraUpdate(
      subscription: ZuoraSubscription,
      effectiveDate: LocalDate,
  ): Either[DataExtractionFailure, ZuoraSubscriptionUpdate] = {

    // This function essentially compute the data required for the amendment, it returns
    // a Either[..., ZuoraSubscriptionUpdate] which indicates the rate plans that are removed and the ones
    // that are added to the subscription. It is possible to override the price in the rate
    // plan charges as charge overrides.

    // Note that AddZuoraRatePlan requires the product catalogue ratePlanId and ratePlanChargeId, but that
    // RemoveZuoraRatePlan requires the specific id of the rate plan carried by the subscription
    // (which is not the same as the catalogue's ratePlanId)

    for {
      existingRatePlan <- subscriptionRatePlan(subscription)
      existingRatePlanCharge <- subscriptionRatePlanCharge(subscription, existingRatePlan)
      currency = existingRatePlanCharge.currency
      oldPrice <- getPriceFromRatePlanCharge(subscription, existingRatePlanCharge)
      billingPeriod <- getBillingPeriodFromSubscription(subscription)
      newPrice <- newPriceLookup(currency, billingPeriod)
      targetRatePlanDetails <- billingPeriodToRatePlanId.get(billingPeriod) match {
        case None =>
          Left(
            DataExtractionFailure(
              s"Could not extract ratePlan for billing period: ${BillingPeriod.toString(billingPeriod)}"
            )
          )
        case Some(period) => Right(period)
      }
    } yield ZuoraSubscriptionUpdate(
      add = List(
        AddZuoraRatePlan(
          targetRatePlanDetails.ratePlanId,
          effectiveDate,
          List(
            ChargeOverride(
              targetRatePlanDetails.ratePlanChargeId,
              BillingPeriod.toString(billingPeriod),
              newPrice
            )
          )
        )
      ),
      remove = List(
        RemoveZuoraRatePlan(
          existingRatePlan.id,
          effectiveDate
        )
      ),
      currentTerm = None,
      currentTermPeriodType = None
    )
  }
}
