package pricemigrationengine.migrations
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._

import java.time.LocalDate

object SupporterPlus2024Migration {

  // ------------------------------------------------
  // Static Data
  // ------------------------------------------------

  val priceCap = 1.27

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

  def cancellationSaveDiscountRatePlan(subscription: ZuoraSubscription): Option[ZuoraRatePlan] = {
    subscription.ratePlans.find(rp => rp.ratePlanName.contains("Cancellation Save Discount"))
  }

  def hasCancellationSaveDiscount(subscription: ZuoraSubscription): Boolean = {
    cancellationSaveDiscountRatePlan(subscription: ZuoraSubscription).isDefined
  }

  def cancellationSaveDiscountEffectiveDate(subscription: ZuoraSubscription): Option[LocalDate] = {
    for {
      ratePlan <- cancellationSaveDiscountRatePlan(subscription)
      charge <- ratePlan.ratePlanCharges.headOption
      date <- charge.effectiveStartDate
    } yield date
  }

  def isUnderActiveCancellationSavePolicy(subscription: ZuoraSubscription, today: LocalDate): Boolean = {
    cancellationSaveDiscountEffectiveDate(subscription: ZuoraSubscription) match {
      case None       => false
      case Some(date) => today.isBefore(date.plusMonths(6))
    }
  }

  // ------------------------------------------------
  // Subscription Data

  def getSupporterPlusV2RatePlan(subscription: ZuoraSubscription): Either[DataExtractionFailure, ZuoraRatePlan] = {
    subscription.ratePlans.find(rp =>
      rp.ratePlanName.contains("Supporter Plus V2") && !rp.lastChangeType.contains("Remove")
    ) match {
      case None =>
        Left(
          DataExtractionFailure(
            s"Subscription ${subscription.subscriptionNumber} doesn't have any `Add`ed rate plan with pattern `Supporter Plus V2`"
          )
        )
      case Some(ratePlan) => Right(ratePlan)
    }
  }

  def getSupporterPlusBaseRatePlanCharge(
      subscriptionNumber: String,
      ratePlan: ZuoraRatePlan
  ): Either[DataExtractionFailure, ZuoraRatePlanCharge] = {
    ratePlan.ratePlanCharges.find(rpc => rpc.name.contains("Supporter Plus")) match {
      case None => {
        Left(
          DataExtractionFailure(s"Subscription ${subscriptionNumber} has a rate plan (${ratePlan}), but with no charge")
        )
      }
      case Some(ratePlanCharge) => Right(ratePlanCharge)
    }
  }

  def getSupporterPlusContributionRatePlanCharge(
      subscriptionNumber: String,
      ratePlan: ZuoraRatePlan
  ): Either[DataExtractionFailure, ZuoraRatePlanCharge] = {
    ratePlan.ratePlanCharges.find(rpc => rpc.name.contains("Contribution")) match {
      case None => {
        Left(
          DataExtractionFailure(s"Subscription ${subscriptionNumber} has a rate plan (${ratePlan}), but with no charge")
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
      ratePlan <- getSupporterPlusV2RatePlan(subscription)
      ratePlanCharge <- getSupporterPlusBaseRatePlanCharge(subscription.subscriptionNumber, ratePlan)
    } yield ratePlanCharge.price
  }

  def newBaseAmount(subscription: ZuoraSubscription): Either[Failure, Option[BigDecimal]] = {
    for {
      ratePlan <- getSupporterPlusV2RatePlan(subscription)
      billingPeriod <- ZuoraRatePlan.ratePlanToBillingPeriod(ratePlan).toRight(DataExtractionFailure(""))
      ratePlanCharge <- getSupporterPlusBaseRatePlanCharge(subscription.subscriptionNumber, ratePlan)
      currency = ratePlanCharge.currency
      oldBaseAmountOpt <- previousBaseAmount(subscription)
      oldBaseAmount <- oldBaseAmountOpt.toRight(
        DataExtractionFailure(
          s"(error: 164d8f1c-6dc6) could not extract base amount for subscription ${subscription.subscriptionNumber}"
        )
      )
      newPriceFromPriceGrid <- getNewPrice(billingPeriod, currency)
        .map(BigDecimal(_))
        .toRight(
          DataExtractionFailure(
            s"(error: 611aedea-0478) could not getNewPrice for (billingPeriod, currency) and (${billingPeriod}, ${currency})"
          )
        )
    } yield {
      Some((oldBaseAmount * BigDecimal(1.27)).min(newPriceFromPriceGrid))
    }
  }

  def contributionAmount(subscription: ZuoraSubscription): Either[Failure, Option[BigDecimal]] = {
    for {
      ratePlan <- getSupporterPlusV2RatePlan(subscription)
      ratePlanCharge <- getSupporterPlusContributionRatePlanCharge(subscription.subscriptionNumber, ratePlan)
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
        DataExtractionFailure(
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
  ): Either[Failure, PriceData] = {
    for {
      ratePlan <- getSupporterPlusV2RatePlan(subscription)
      billingPeriod <- ZuoraRatePlan.ratePlanToBillingPeriod(ratePlan).toRight(DataExtractionFailure(""))
      ratePlanCharge <- getSupporterPlusBaseRatePlanCharge(subscription.subscriptionNumber, ratePlan)
      currency = ratePlanCharge.currency
      oldPrice <- ratePlanCharge.price.toRight(DataExtractionFailure(""))
      newPrice <- getNewPrice(billingPeriod, currency).toRight(DataExtractionFailure(""))
    } yield PriceData(currency, oldPrice, newPrice, BillingPeriod.toString(billingPeriod))
  }

  def zuoraUpdate(
      subscription: ZuoraSubscription,
      effectiveDate: LocalDate,
      oldPrice: BigDecimal,
      estimatedNewPrice: BigDecimal,
      priceCap: BigDecimal
  ): Either[Failure, ZuoraSubscriptionUpdate] = {
    for {
      existingRatePlan <- getSupporterPlusV2RatePlan(subscription)
      existingBaseRatePlanCharge <- getSupporterPlusBaseRatePlanCharge(
        subscription.subscriptionNumber,
        existingRatePlan
      )
      existingContributionRatePlanCharge <- getSupporterPlusContributionRatePlanCharge(
        subscription.subscriptionNumber,
        existingRatePlan
      )
      existingContributionPrice <- existingContributionRatePlanCharge.price.toRight(
        DataExtractionFailure(
          s"[22405076] Could not extract existing contribution price for subscription ${subscription.subscriptionNumber}"
        )
      )
      billingPeriod <- ZuoraRatePlan
        .ratePlanToBillingPeriod(existingRatePlan)
        .toRight(
          DataExtractionFailure(
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
                price = PriceCap.priceCapForNotification(oldPrice, estimatedNewPrice, priceCap)
              ),
              ChargeOverride(
                productRatePlanChargeId = existingContributionRatePlanCharge.productRatePlanChargeId,
                billingPeriod = BillingPeriod.toString(billingPeriod),
                price = existingContributionPrice
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

  // ------------------------------------------------
  // Orders API Payloads
  // ------------------------------------------------

  // The two function below have same names but different signatures.
  // The second one calls the first one.

  def amendmentOrdersPayload(
      orderDate: LocalDate,
      accountNumber: String,
      subscriptionNumber: String,
      effectDate: LocalDate,
      removeRatePlanId: String,
      productRatePlanId: String,
      existingBaseProductRatePlanChargeId: String,
      existingContributionRatePlanChargeId: String,
      newBaseAmount: BigDecimal,
      newContributionAmount: BigDecimal
  ): ZuoraAmendmentOrderPayload = {
    val triggerDates = List(
      ZuoraAmendmentOrderPayloadOrderActionTriggerDate("ContractEffective", effectDate),
      ZuoraAmendmentOrderPayloadOrderActionTriggerDate("ServiceActivation", effectDate),
      ZuoraAmendmentOrderPayloadOrderActionTriggerDate("CustomerAcceptance", effectDate)
    )
    val actionRemove = ZuoraAmendmentOrderPayloadOrderActionRemove(
      triggerDates = triggerDates,
      removeProduct = ZuoraAmendmentOrderPayloadOrderActionRemoveProduct(removeRatePlanId)
    )
    val actionAdd = ZuoraAmendmentOrderPayloadOrderActionAdd(
      triggerDates = triggerDates,
      addProduct = ZuoraAmendmentOrderPayloadOrderActionAddProduct(
        productRatePlanId = productRatePlanId,
        chargeOverrides = List(
          ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride(
            productRatePlanChargeId = existingBaseProductRatePlanChargeId,
            pricing = Map("recurringFlatFee" -> Map("listPrice" -> newBaseAmount))
          ),
          ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride(
            productRatePlanChargeId = existingContributionRatePlanChargeId,
            pricing = Map("recurringFlatFee" -> Map("listPrice" -> newContributionAmount))
          )
        )
      )
    )
    val orderActions = List(actionRemove, actionAdd)
    val subscriptions = List(
      ZuoraAmendmentOrderPayloadSubscription(subscriptionNumber, orderActions)
    )
    val processingOptions = ZuoraAmendmentOrderPayloadProcessingOptions(runBilling = false, collectPayment = false)
    ZuoraAmendmentOrderPayload(
      orderDate = orderDate,
      existingAccountNumber = accountNumber,
      subscriptions = subscriptions,
      processingOptions = processingOptions
    )
  }

  def amendmentOrderPayload(
      orderDate: LocalDate,
      accountNumber: String,
      subscriptionNumber: String,
      effectDate: LocalDate,
      subscription: ZuoraSubscription,
      oldPrice: BigDecimal,
      estimatedNewPrice: BigDecimal,
      priceCap: BigDecimal
  ): Either[Failure, ZuoraAmendmentOrderPayload] = {
    for {
      existingRatePlan <- getSupporterPlusV2RatePlan(subscription)
      existingBaseRatePlanCharge <- getSupporterPlusBaseRatePlanCharge(
        subscription.subscriptionNumber,
        existingRatePlan
      )
      existingContributionRatePlanCharge <- getSupporterPlusContributionRatePlanCharge(
        subscription.subscriptionNumber,
        existingRatePlan
      )
      existingContributionPrice <- existingContributionRatePlanCharge.price.toRight(
        DataExtractionFailure(
          s"[e4e702b6] Could not extract existing contribution price for subscription ${subscription.subscriptionNumber}"
        )
      )
    } yield amendmentOrdersPayload(
      orderDate = orderDate,
      accountNumber = accountNumber,
      subscriptionNumber = subscriptionNumber,
      effectDate = effectDate,
      removeRatePlanId = existingRatePlan.id,
      productRatePlanId = existingRatePlan.productRatePlanId,
      existingBaseProductRatePlanChargeId = existingBaseRatePlanCharge.productRatePlanChargeId,
      existingContributionRatePlanChargeId = existingContributionRatePlanCharge.productRatePlanChargeId,
      newBaseAmount = PriceCap.priceCapForNotification(oldPrice, estimatedNewPrice, priceCap),
      newContributionAmount = existingContributionPrice
    )
  }
}
