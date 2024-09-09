package pricemigrationengine.migrations
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._

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
      date <- charge.effectiveStartDate
    } yield date
  }

  def isUnderActiveCancellationSave(subscription: ZuoraSubscription, today: LocalDate): Boolean = {
    cancellationSaveEffectiveDate(subscription: ZuoraSubscription) match {
      case None       => false
      case Some(date) => (date == today) || today.isBefore(date)
    }
  }

  // ------------------------------------------------
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
          AmendmentDataFailure(s"Subscription ${subscriptionNumber} has a rate plan (${ratePlan}), but with no charge")
        )
      }
      case Some(ratePlanCharge) => Right(ratePlanCharge)
    }
  }

  def supporterPlusContributionRatePlanCharge(
      subscriptionNumber: String,
      ratePlan: ZuoraRatePlan
  ): Either[AmendmentDataFailure, ZuoraRatePlanCharge] = {
    ratePlan.ratePlanCharges.find(rpc => rpc.name.contains("Contribution")) match {
      case None => {
        Left(
          AmendmentDataFailure(s"Subscription ${subscriptionNumber} has a rate plan (${ratePlan}), but with no charge")
        )
      }
      case Some(ratePlanCharge) => Right(ratePlanCharge)
    }
  }

  // ------------------------------------------------
  // Notification helpers

  /*
    Date: September 2024
    Author: Pascal
    Comment Group: 602514a6-5e53

    These functions have been added to implement the extra fields added to EmailPayloadSubscriberAttributes
    as part of the set up of the SupporterPlus 2024 migration (see Comment Group: 602514a6-5e53)
   */

  def previousBaseAmount(subscription: ZuoraSubscription): Either[Failure, Option[BigDecimal]] = {
    for {
      ratePlan <- supporterPlusV2RatePlan(subscription)
      ratePlanCharge <- supporterPlusBaseRatePlanCharge(subscription.subscriptionNumber, ratePlan)
    } yield ratePlanCharge.price
  }

  def newBaseAmount(subscription: ZuoraSubscription): Either[Failure, Option[BigDecimal]] = {
    for {
      ratePlan <- supporterPlusV2RatePlan(subscription)
      billingPeriod <- ZuoraRatePlan.ratePlanToBillingPeriod(ratePlan).toRight(AmendmentDataFailure(""))
      ratePlanCharge <- supporterPlusBaseRatePlanCharge(subscription.subscriptionNumber, ratePlan)
      currency = ratePlanCharge.currency
      oldBaseAmountOpt <- previousBaseAmount(subscription)
      oldBaseAmount <- oldBaseAmountOpt.toRight(
        AmendmentDataFailure(
          s"(error: 164d8f1c-6dc6) could not extract base amount for subscription ${subscription.subscriptionNumber}"
        )
      )
      newPriceFromPriceGrid <- getNewPrice(billingPeriod, currency)
        .map(BigDecimal(_))
        .toRight(
          AmendmentDataFailure(
            s"(error: 611aedea-0478) could not getNewPrice for (billingPeriod, currency) and (${billingPeriod}, ${currency})"
          )
        )
    } yield {
      Some((oldBaseAmount * BigDecimal(1.27)).min(newPriceFromPriceGrid))
    }
  }

  def contributionAmount(subscription: ZuoraSubscription): Either[Failure, Option[BigDecimal]] = {
    for {
      ratePlan <- supporterPlusV2RatePlan(subscription)
      ratePlanCharge <- supporterPlusContributionRatePlanCharge(subscription.subscriptionNumber, ratePlan)
    } yield ratePlanCharge.price
  }

  def previousCombinedAmount(subscription: ZuoraSubscription): Either[Failure, Option[BigDecimal]] = {
    for {
      contributionAmountOpt <- contributionAmount(subscription)
      previousBaseAmountOpt <- previousBaseAmount(subscription)
    } yield (
      for {
        contributionAmount <- contributionAmountOpt
        previousBaseAmount <- previousBaseAmountOpt
      } yield contributionAmount + previousBaseAmount
    )
  }

  def newCombinedAmount(subscription: ZuoraSubscription): Either[Failure, Option[BigDecimal]] = {
    for {
      contributionAmountOpt <- contributionAmount(subscription)
      previousBaseAmountOpt <- newBaseAmount(subscription)
    } yield (
      for {
        contributionAmount <- contributionAmountOpt
        previousBaseAmount <- previousBaseAmountOpt
      } yield contributionAmount + previousBaseAmount
    )
  }

  def hasNonTrivialContribution(subscription: ZuoraSubscription): Either[Failure, Boolean] = {
    for {
      amountOpt <- contributionAmount(subscription: ZuoraSubscription)
      amount <- amountOpt.toRight(
        AmendmentDataFailure(
          s"(error: 232760f5) could not extract contribution amount for subscription ${subscription.subscriptionNumber}"
        )
      )
    } yield amount > 0
  }

  // -------------------------------------------------------------------
  // Braze names

  def brazeName(subscription: ZuoraSubscription): Either[Failure, String] = {
    for {
      status <- hasNonTrivialContribution(subscription: ZuoraSubscription)
    } yield {
      if (status) {
        "SV_SP2_Contributors_PriceRise2024"
      } else {
        "SV_SP2_PriceRise2024"
      }
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
      existingRatePlan <- supporterPlusV2RatePlan(subscription)
      existingBaseRatePlanCharge <- supporterPlusBaseRatePlanCharge(
        subscription.subscriptionNumber,
        existingRatePlan
      )
      billingPeriod <- ZuoraRatePlan
        .ratePlanToBillingPeriod(existingRatePlan)
        .toRight(
          AmendmentDataFailure(
            s"[17469705] Could not determine the billing period for subscription ${subscription.subscriptionNumber}"
          )
        )
    } yield {
      ZuoraSubscriptionUpdate(
        add = List(
          AddZuoraRatePlan(
            productRatePlanId = existingRatePlan.productRatePlanId,
            contractEffectiveDate = effectiveDate,
            chargeOverrides = List(
              ChargeOverride(
                productRatePlanChargeId = existingBaseRatePlanCharge.productRatePlanChargeId,
                billingPeriod = BillingPeriod.toString(billingPeriod),
                price = 120.0
              )
            )
          )
        ),
        remove = List(
          RemoveZuoraRatePlan(
            ratePlanId = existingRatePlan.id,
            contractEffectiveDate = effectiveDate
          )
        ),
        currentTerm = None,
        currentTermPeriodType = None
      )
    }
  }
}
