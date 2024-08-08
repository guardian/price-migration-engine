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

  val maxLeadTime = 49
  val minLeadTime = 36

  // ------------------------------------------------
  // Data Functions
  // ------------------------------------------------

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
      date <- charge.triggerDate
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
