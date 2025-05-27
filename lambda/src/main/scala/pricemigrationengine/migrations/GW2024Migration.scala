package pricemigrationengine.migrations
import pricemigrationengine.model.PriceCap
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._
import pricemigrationengine.libs._

import java.time.LocalDate

object GW2024Migration {

  // ------------------------------------------------
  // Static Data
  // ------------------------------------------------

  val priceCap = 1.25

  val maxLeadTime = 49
  val minLeadTime = 36

  val priceMapMonthlies: Map[String, BigDecimal] = Map(
    "GBP" -> BigDecimal(15),
    "USD" -> BigDecimal(30),
    "CAD" -> BigDecimal(33),
    "AUD" -> BigDecimal(40),
    "NZD" -> BigDecimal(50),
    "EUR" -> BigDecimal(26.5),
    "ROW (USD)" -> BigDecimal(33)
  )

  val priceMapQuarterlies: Map[String, BigDecimal] = Map(
    "GBP" -> BigDecimal(45),
    "USD" -> BigDecimal(90),
    "CAD" -> BigDecimal(99),
    "AUD" -> BigDecimal(120),
    "NZD" -> BigDecimal(150),
    "EUR" -> BigDecimal(79.5),
    "ROW (USD)" -> BigDecimal(99)
  )

  val priceMapAnnuals: Map[String, BigDecimal] = Map(
    "GBP" -> BigDecimal(180),
    "USD" -> BigDecimal(360),
    "CAD" -> BigDecimal(396),
    "AUD" -> BigDecimal(480),
    "NZD" -> BigDecimal(600),
    "EUR" -> BigDecimal(318),
    "ROW (USD)" -> BigDecimal(396)
  )

  /*

      Date: 27 March 2024
      Author: Pascal

      I have downloaded all the Zuora subscriptions using the subscription
      numbers from the migration cohort ids and extracted the rate plan names. Found:

      GW Oct 18 - Monthly - Domestic
      GW Oct 18 - Quarterly - Domestic
      GW Oct 18 - Annual - Domestic
      GW Oct 18 - Monthly - ROW
      GW Oct 18 - Quarterly - ROW
      GW Oct 18 - Annual - ROW
      Guardian Weekly Quarterly
      Guardian Weekly Annual
      Guardian Weekly 1 Year

      It is not necessary to do this as part of preparing and writing the code for a migration,
      but doing so gives us an exact list of the rate plan names that we are looking for
      and this simplifies the extraction of the rate plan that is going to be upgraded for
      each subscription.
   */

  val migrationRatePlanNames: List[String] = List(
    "GW Oct 18 - Monthly - Domestic",
    "GW Oct 18 - Quarterly - Domestic",
    "GW Oct 18 - Annual - Domestic",
    "GW Oct 18 - Monthly - ROW",
    "GW Oct 18 - Quarterly - ROW",
    "GW Oct 18 - Annual - ROW",
    "Guardian Weekly Quarterly",
    "Guardian Weekly Annual",
    "Guardian Weekly 1 Year",
  )

  // ------------------------------------------------
  // Data Extraction and Manipulation
  // ------------------------------------------------

  def getNewPrice(billingPeriod: BillingPeriod, extendedCurrency: Currency): Option[BigDecimal] = {
    billingPeriod match {
      case Monthly   => priceMapMonthlies.get(extendedCurrency)
      case Quarterly => priceMapQuarterlies.get(extendedCurrency)
      case Annual    => priceMapAnnuals.get(extendedCurrency)
      case _         => None
    }
  }

  // Note about the difference between lastChangeTypeIsAdd and lastChangeTypeIsNotRemove
  // Sadly `lastChangeType` is not always defined on all rate plans. The situation is:
  //     - Not defined                    -> Active rate plan
  //     - Defined and value is "Add"     -> Active rate plan
  //     - Defined and value is "Removed" -> Non active rate plan

  def lastChangeTypeIsAdd(ratePlan: ZuoraRatePlan): Boolean = {
    ratePlan.lastChangeType.contains("Add")
  }

  def lastChangeTypeIsNotRemove(ratePlan: ZuoraRatePlan): Boolean = {
    !ratePlan.lastChangeType.contains("Remove")
  }

  def subscriptionToMigrationRatePlans(subscription: ZuoraSubscription): List[ZuoraRatePlan] = {
    subscription.ratePlans
      .filter(rp => lastChangeTypeIsNotRemove(rp))
      .filter(rp => migrationRatePlanNames.contains(rp.ratePlanName))
  }

  def subscriptionToMigrationRatePlan(subscription: ZuoraSubscription): Option[ZuoraRatePlan] = {
    /*
      Date: 27 March 2024.
      Author: Pascal

      In theory, this code is slightly incorrect, because the Zuora data model
      doesn't prevent the presence of two active (non Discounts) rate plans with
      distinct productRatePlanIds. Because of the way we compute subscriptionToMigrationRatePlans
      using actual rate plan names, this would be two active rate plans from the list. This is
      unlikely but not impossible.

      For argument's sake, if the above special case ever happened, then this code would be selecting
      and price rising only the first of such rate plans.

      In practice I have downloaded all the subscriptions of this migration and checked that
      they all have just one active productRatePlanId. (With that said, and sadly, the
      actual JSON blob does sometimes have several instances of the rate rate plan. This is
      not a problem though, because the ZuoraSubscriptionUpdate's RemoveZuoraRatePlan refers to
      a productRatePlanId and not the rate plan's own id)
     */

    subscriptionToMigrationRatePlans(subscription).headOption
  }

  def subscriptionToCurrency(
      subscription: ZuoraSubscription
  ): Option[Currency] = {
    for {
      ratePlan <- subscriptionToMigrationRatePlan(subscription)
      currency <- ZuoraRatePlan.ratePlanToCurrency(ratePlan: ZuoraRatePlan)
    } yield currency
  }

  def isROW(subscription: ZuoraSubscription, account: ZuoraAccount): Option[Boolean] = {
    for {
      ratePlan <- subscriptionToMigrationRatePlan(subscription)
      currency <- ZuoraRatePlan.ratePlanToCurrency(ratePlan)
    } yield {
      val country = account.soldToContact.country
      currency == "USD" && country != "United States"
    }
  }

  def subscriptionToExtendedCurrency(
      subscription: ZuoraSubscription,
      account: ZuoraAccount
  ): Option[Currency] = {
    for {
      currency <- subscriptionToCurrency(subscription)
      isROW <- isROW(subscription, account)
    } yield if (isROW) "ROW (USD)" else currency
  }

  def subscriptionToBillingPeriod(subscription: ZuoraSubscription): Option[BillingPeriod] = {
    for {
      ratePlan <- subscriptionToMigrationRatePlan(subscription)
      billingPeriod <- ZuoraRatePlan.ratePlanToBillingPeriod(ratePlan)
    } yield billingPeriod
  }

  def getNewPrice(subscription: ZuoraSubscription, account: ZuoraAccount): Option[BigDecimal] = {
    for {
      billingPeriod <- subscriptionToBillingPeriod(subscription)
      extendedCurrency <- subscriptionToExtendedCurrency(subscription, account)
      price <- getNewPrice(billingPeriod, extendedCurrency)
    } yield price
  }

  def subscriptionToLastPriceMigrationDate(subscription: ZuoraSubscription): Option[LocalDate] = {
    Some(
      subscriptionToMigrationRatePlans(subscription)
        .flatMap(ratePlan => ratePlan.ratePlanCharges)
        .flatMap(rpc => rpc.originalOrderDate)
        .foldLeft(LocalDate.of(2000, 1, 1))((acc, date) => Date.datesMax(acc, date))
    )
  }

  def zuoraRatePlanToRatePlanChargeId(zuoraRatePlan: ZuoraRatePlan): Option[String] = {
    zuoraRatePlan.ratePlanCharges.map(rpc => rpc.productRatePlanChargeId).headOption
  }

  // ------------------------------------------------
  // Primary Functions
  // ------------------------------------------------

  def priceData(
      subscription: ZuoraSubscription,
      account: ZuoraAccount
  ): Either[DataExtractionFailure, PriceData] = {
    val priceDataOpt = for {
      currency <- subscriptionToCurrency(subscription)
      ratePlan <- subscriptionToMigrationRatePlan(subscription)
      oldPrice = ZuoraRatePlan.ratePlanToRatePlanPrice(ratePlan)
      newPrice <- getNewPrice(subscription, account)
      billingPeriod <- subscriptionToBillingPeriod(subscription)
    } yield PriceData(currency, oldPrice, newPrice, BillingPeriod.toString(billingPeriod))
    priceDataOpt match {
      case Some(pricedata) => Right(pricedata)
      case None =>
        Left(
          DataExtractionFailure(s"Could not determine PriceData for subscription ${subscription.subscriptionNumber}")
        )
    }
  }

  def zuoraUpdate(
      subscription: ZuoraSubscription,
      effectiveDate: LocalDate,
      oldPrice: BigDecimal,
      estimatedNewPrice: BigDecimal,
      priceCap: BigDecimal
  ): Either[DataExtractionFailure, ZuoraSubscriptionUpdate] = {
    for {
      ratePlan <- subscriptionToMigrationRatePlan(subscription).toRight(
        DataExtractionFailure(
          s"[a4d99cf3] Could not determine the Zuora migration rate plan for subscription ${subscription.subscriptionNumber}"
        )
      )
      ratePlanChargeId <- zuoraRatePlanToRatePlanChargeId(ratePlan).toRight(
        DataExtractionFailure(
          s"[105f6c88] Could not determine the rate plan charge id for rate plan ${ratePlan}"
        )
      )
      billingPeriod <- subscriptionToBillingPeriod(subscription).toRight(
        DataExtractionFailure(
          s"[17469705] Could not determine the billing period for subscription ${subscription.subscriptionNumber}"
        )
      )
    } yield ZuoraSubscriptionUpdate(
      add = List(
        AddZuoraRatePlan(
          productRatePlanId = ratePlan.productRatePlanId,
          contractEffectiveDate = effectiveDate,
          chargeOverrides = List(
            ChargeOverride(
              productRatePlanChargeId = ratePlanChargeId,
              billingPeriod = BillingPeriod.toString(billingPeriod),
              price = PriceCap.priceCapForNotification(oldPrice, estimatedNewPrice, priceCap)
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
