package pricemigrationengine.migrations
import pricemigrationengine.model.PriceCap
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._
import pricemigrationengine.util._

import java.time.LocalDate

object SupporterPlus2024Migration {

  // ------------------------------------------------
  // Static Data
  // ------------------------------------------------

  val maxLeadTime = 33
  val minLeadTime = 31

  val pricesMonthlyOld: Map[String, Double] = Map(
    "GBP" -> 10,
    "USD" -> 13,
    "CAD" -> 13,
    "AUD" -> 17,
    "NZD" -> 17,
    "EUR" -> 10
  )

  val pricesAnnualOld: Map[String, Double] = Map(
    "GBP" -> 95,
    "USD" -> 120,
    "CAD" -> 120,
    "AUD" -> 160,
    "NZD" -> 160,
    "EUR" -> 95
  )

  val pricesMonthlyNew: Map[String, Double] = Map(
    "GBP" -> 12,
    "USD" -> 15,
    "CAD" -> 15,
    "AUD" -> 20,
    "NZD" -> 20,
    "EUR" -> 12
  )

  val pricesAnnualNew: Map[String, Double] = Map(
    "GBP" -> 120,
    "USD" -> 150,
    "CAD" -> 150,
    "AUD" -> 200,
    "NZD" -> 200,
    "EUR" -> 120
  )

  // ------------------------------------------------
  // Data Functions
  // ------------------------------------------------

  // Prices

  def getOldPrice(billingPeriod: BillingPeriod, currency: String): Option[Double] = {
    billingPeriod match {
      case Monthly => pricesMonthlyOld.get(currency)
      case Annual  => pricesAnnualOld.get(currency)
      case _       => None
    }
  }

  def getNewPrice(billingPeriod: BillingPeriod, currency: String): Option[Double] = {
    billingPeriod match {
      case Monthly => pricesMonthlyNew.get(currency)
      case Annual  => pricesAnnualNew.get(currency)
      case _       => None
    }
  }

  // Cancellation Saves

  def cancellationSaveRatePlan(subscription: ZuoraSubscription): Option[ZuoraRatePlan] = {
    subscription.ratePlans.find(rp => rp.ratePlanName.contains("Cancellation Save Discount"))
  }

  def isInCancellationSave(subscription: ZuoraSubscription): Boolean = {
    cancellationSaveRatePlan(subscription: ZuoraSubscription).isDefined
  }

  def cancellationSaveEffectiveDate(subscription: ZuoraSubscription): Option[LocalDate] = {
    for {
      ratePlan <- cancellationSaveRatePlan(subscription)
      charge <- ratePlan.ratePlanCharges.headOption
      date <- charge.effectiveStartDate
    } yield date
  }

  // Subscription Data

  def supporterPlusV2RatePlan(subscription: ZuoraSubscription): Either[AmendmentDataFailure, ZuoraRatePlan] = {
    subscription.ratePlans.find(rp =>
      rp.ratePlanName.contains("Supporter Plus V2") && !rp.lastChangeType.contains("Remove")
    ) match {
      case None =>
        Left(
          AmendmentDataFailure(
            s"Subscription ${subscription.subscriptionNumber} doesn't have any `Add`ed rate plan with pattern `Supporter Plus V2`"
          )
        )
      case Some(ratePlan) => Right(ratePlan)
    }
  }

  def supporterPlusBaseRatePlanCharge(
      subscriptionNumber: String,
      ratePlan: ZuoraRatePlan
  ): Either[AmendmentDataFailure, ZuoraRatePlanCharge] = {
    ratePlan.ratePlanCharges.find(rpc => rpc.name.contains("Supporter Plus")) match {
      case None => {
        Left(
          AmendmentDataFailure(s"Subscription ${subscriptionNumber} has a rate plan, but with no charge")
        )
      }
      case Some(ratePlanCharge) => Right(ratePlanCharge)
    }
  }

  // ------------------------------------------------
  // Primary Interface
  // ------------------------------------------------

  def priceData(
      subscription: ZuoraSubscription
  ): Either[AmendmentDataFailure, PriceData] = {
    for {
      ratePlan <- supporterPlusV2RatePlan(subscription)
      billingPeriod <- ZuoraRatePlan.ratePlanToBillingPeriod(ratePlan).toRight(AmendmentDataFailure(""))
      ratePlanCharge <- supporterPlusBaseRatePlanCharge(subscription.subscriptionNumber, ratePlan)
      currency = ratePlanCharge.currency
      oldPrice <- ratePlanCharge.price.toRight(AmendmentDataFailure(""))
      newPrice <- getNewPrice(billingPeriod, currency).toRight(AmendmentDataFailure(""))
    } yield PriceData(currency, oldPrice, newPrice, BillingPeriod.toString(billingPeriod))
  }

  def zuoraUpdate(
      subscription: ZuoraSubscription,
      effectiveDate: LocalDate,
  ): Either[AmendmentDataFailure, ZuoraSubscriptionUpdate] = {
    for {
      ratePlan <- supporterPlusV2RatePlan(subscription)
      ratePlanChargeId <- ratePlan.ratePlanCharges
        .map(rpc => rpc.productRatePlanChargeId)
        .headOption
        .toRight(
          AmendmentDataFailure(
            s"[1862ada1] Could not determine the billing period for subscription ${subscription.subscriptionNumber}"
          )
        )
      billingPeriod <- ZuoraRatePlan
        .ratePlanToBillingPeriod(ratePlan)
        .toRight(
          AmendmentDataFailure(
            s"[17469705] Could not determine the billing period for subscription ${subscription.subscriptionNumber}"
          )
        )
    } yield {
      ZuoraSubscriptionUpdate(
        add = List(
          AddZuoraRatePlan(
            productRatePlanId = ratePlan.productRatePlanId,
            contractEffectiveDate = effectiveDate,
            chargeOverrides = List(
              ChargeOverride(
                productRatePlanChargeId = ratePlanChargeId,
                billingPeriod = BillingPeriod.toString(billingPeriod),
                price = 120.0
              )
            )
          )
        ),
        remove = List(
          RemoveZuoraRatePlan(
            ratePlanId = ratePlan.id,
            contractEffectiveDate = effectiveDate
          )
        ),
        currentTerm = None,
        currentTermPeriodType = None
      )
    }
  }
}
