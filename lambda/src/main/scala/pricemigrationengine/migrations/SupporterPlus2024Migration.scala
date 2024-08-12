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

  def subscriptionRatePlan(subscription: ZuoraSubscription): Either[AmendmentDataFailure, ZuoraRatePlan] = {
    subscription.ratePlans.find(rp => rp.ratePlanName.contains("Supporter Plus V2")) match {
      case None =>
        Left(
          AmendmentDataFailure(
            s"Subscription ${subscription.subscriptionNumber} doesn't have any rate plan with pattern `Supporter Plus V2`"
          )
        )
      case Some(ratePlan) => Right(ratePlan)
    }
  }

  def subscriptionRatePlanCharge(
      subscriptionNumber: String,
      ratePlan: ZuoraRatePlan
  ): Either[AmendmentDataFailure, ZuoraRatePlanCharge] = {
    ratePlan.ratePlanCharges.headOption match {
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
      ratePlan <- subscriptionRatePlan(subscription)
      billingPeriod <- ZuoraRatePlan.ratePlanToBillingPeriod(ratePlan).toRight(AmendmentDataFailure(""))
      ratePlanCharge <- subscriptionRatePlanCharge(subscription.subscriptionNumber, ratePlan)
      currency = ratePlanCharge.currency
      oldPrice <- getOldPrice(billingPeriod, currency).toRight(AmendmentDataFailure(""))
      newPrice <- getNewPrice(billingPeriod, currency).toRight(AmendmentDataFailure(""))
    } yield PriceData(currency, oldPrice, newPrice, BillingPeriod.toString(billingPeriod))
  }

  def zuoraUpdate(
      subscription: ZuoraSubscription,
      effectiveDate: LocalDate,
  ): Either[AmendmentDataFailure, ZuoraSubscriptionUpdate] = ???

}
