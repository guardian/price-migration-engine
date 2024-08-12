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

  val pricesMonthly: Map[String, Double] = Map(
    "GBP" -> 12,
    "USD" -> 15,
    "CAD" -> 15,
    "AUD" -> 20,
    "NZD" -> 20,
    "EUR" -> 12
  )

  val pricesAnnual: Map[String, Double] = Map(
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

  def getPrice(billingPeriod: BillingPeriod, currency: String): Option[Double] = {
    billingPeriod match {
      case Monthly => pricesMonthly.get(currency)
      case Annual  => pricesAnnual.get(currency)
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

  // ------------------------------------------------
  // Primary Interface
  // ------------------------------------------------

  def priceData(
      subscription: ZuoraSubscription,
      account: ZuoraAccount
  ): Either[AmendmentDataFailure, PriceData] = ???

  def zuoraUpdate(
      subscription: ZuoraSubscription,
      effectiveDate: LocalDate,
  ): Either[AmendmentDataFailure, ZuoraSubscriptionUpdate] = ???

}
