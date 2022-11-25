package pricemigrationengine.model

import pricemigrationengine.model.ChargeOverride.fromRatePlanCharge

import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import pricemigrationengine.model.Either._
import pricemigrationengine.model.ZuoraProductCatalogue.{productPricingMap}
import upickle.default.{ReadWriter, macroRW}

case class ZuoraSubscriptionUpdate(
    add: Seq[AddZuoraRatePlan],
    remove: Seq[RemoveZuoraRatePlan],
    currentTerm: Option[Int],
    currentTermPeriodType: Option[String]
)

/*
  Out of consideration for our users, we have the overall policy to not increase subscriptions charges
  by more than a certain amount relatively to the old price. At the time these lines are written, we cap it to
  20% increase. This means that if the current/old price is 100 and the new price computed after applying a ew rate plan
  if 130, then we limit to 120.

  To encode this cap, we introduce ChargeCap, which essentially carries a capped price. This object is passed, optionally,
  to any function that would need to know about it.

  Note that the Amendment Handler does indeed pass a ChargeCap down, this is mostly to check that the price calculated
  after rate plan update is within parameters. That price is the cohort item estimated price.

  The Estimation Handler is slightly more subtle. we do not at first know the old price (which will need to be computed as part of the estimation)
  In this case, we pass down a ChargeCap.builderFromMultiplier which is going to compute the relevant ChargeCap when the old price is known
  and will use it to compute the estimated price.
 */

case class ChargeCap(item: Option[CohortItem], priceCap: BigDecimal)

object ChargeCap {

  def fromOldPriceAndMultiplier(item: Option[CohortItem], oldPrice: BigDecimal, multiplier: Double): ChargeCap =
    ChargeCap(item, oldPrice * multiplier)

  type ChargeCapBuilderFromMultiplier = (BigDecimal) => ChargeCap

  def builderFromMultiplier(multiplier: Double): ChargeCapBuilderFromMultiplier = (oldPrice: BigDecimal) =>
    fromOldPriceAndMultiplier(None, oldPrice, multiplier)
}

object ZuoraSubscriptionUpdate {
  private val zoneABCPlanNames = List("Guardian Weekly Zone A", "Guardian Weekly Zone B", "Guardian Weekly Zone C")

  implicit val rw: ReadWriter[ZuoraSubscriptionUpdate] = macroRW

  /** Takes all non-discount rate plans participating in the invoice list on the given date, and replaces them with
    * their current equivalent. This has the effect of updating their prices to the current ones in the product
    * catalogue.
    *
    * If the billing period of a rate plan charge is different to the billing period of the corresponding product rate
    * plan charge, we have to add a charge override to the update to adjust the price of the charge to the length of its
    * billing period.
    */
  def updateOfRatePlansToCurrent(
      account: ZuoraAccount,
      catalogue: ZuoraProductCatalogue,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      effectiveDate: LocalDate,
      cargeCap: Option[ChargeCap]
  ): Either[AmendmentDataFailure, ZuoraSubscriptionUpdate] = {

    val ratePlans = (for {
      invoiceItem <- ZuoraInvoiceItem.items(invoiceList, subscription, effectiveDate)
      ratePlanCharge <- ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toSeq
      price <- ratePlanCharge.price.toSeq
      if price > 0
      ratePlan <- ZuoraRatePlan.ratePlan(subscription, ratePlanCharge).toSeq
    } yield ratePlan).distinct

    if (ratePlans.isEmpty)
      Left(AmendmentDataFailure(s"No rate plans to update for subscription ${subscription.subscriptionNumber}"))
    else if (ratePlans.size > 1)
      Left(AmendmentDataFailure(s"Multiple rate plans to update: ${ratePlans.map(_.id)}"))
    else {
      val pricingData = productPricingMap(catalogue)

      val isZoneABC = subscription.ratePlans.filter(zoneABCPlanNames contains _.productName)

      ratePlans
        .map(
          if (isZoneABC.nonEmpty)
            AddZuoraRatePlan.fromRatePlanGuardianWeekly(account, catalogue, effectiveDate, cargeCap)
          else AddZuoraRatePlan.fromRatePlan(pricingData, effectiveDate, cargeCap)
        )
        .sequence
        .map { add =>
          val isTermTooShort = subscription.termEndDate.isBefore(effectiveDate)
          ZuoraSubscriptionUpdate(
            add,
            remove = ratePlans.map(ratePlan => RemoveZuoraRatePlan(ratePlan.id, effectiveDate)),
            currentTerm =
              if (isTermTooShort)
                Some(subscription.termStartDate.until(effectiveDate, DAYS).toInt)
              else None,
            currentTermPeriodType = if (isTermTooShort) Some("Day") else None
          )
        }
    }
  }
}

case class AddZuoraRatePlan(
    productRatePlanId: String,
    contractEffectiveDate: LocalDate,
    chargeOverrides: Seq[ChargeOverride] = Nil
)

object AddZuoraRatePlan {
  implicit val rw: ReadWriter[AddZuoraRatePlan] = macroRW

  def alterZuoraPricings(pricings: Set[ZuoraPricing], cappedPrice: BigDecimal): Set[ZuoraPricing] =
    pricings.map(pricing =>
      ZuoraPricing(
        pricing.currency,
        pricing.price.map(price => List(price, cappedPrice).min),
        pricing.price.map(price => price > cappedPrice).getOrElse(false)
      )
    )

  def alterZuoraProductRatePlanCharge(
      rpc: ZuoraProductRatePlanCharge,
      chargeCapOpt: Option[ChargeCap]
  ): ZuoraProductRatePlanCharge = {
    chargeCapOpt match {
      case None => rpc
      case Some(chargeCap) =>
        ZuoraProductRatePlanCharge(rpc.id, rpc.billingPeriod, alterZuoraPricings(rpc.pricing, chargeCap.priceCap))
    }
  }

  def fromRatePlan(
      pricingData: ZuoraPricingData,
      contractEffectiveDate: LocalDate,
      chargeCap: Option[ChargeCap]
  )(
      ratePlan: ZuoraRatePlan
  ): Either[AmendmentDataFailure, AddZuoraRatePlan] = {

    def alterPricingData(zuoraPricingData: ZuoraPricingData, chargeCap: Option[ChargeCap]): ZuoraPricingData = {
      zuoraPricingData.toSeq.map { case (chargeId, charge) =>
        (chargeId, alterZuoraProductRatePlanCharge(charge, chargeCap))
      }.toMap
    }

    for {
      chargeOverrides <- ChargeOverride.fromRatePlan(alterPricingData(pricingData, chargeCap), ratePlan)
    } yield AddZuoraRatePlan(
      productRatePlanId = ratePlan.productRatePlanId,
      contractEffectiveDate,
      chargeOverrides
    )
  }

  def fromRatePlanGuardianWeekly(
      account: ZuoraAccount,
      catalogue: ZuoraProductCatalogue,
      contractEffectiveDate: LocalDate,
      chargeCap: Option[ChargeCap]
  )(
      ratePlan: ZuoraRatePlan
  ): Either[AmendmentDataFailure, AddZuoraRatePlan] =
    for {
      guardianWeekly <- GuardianWeekly.getNewRatePlanCharges(
        account,
        catalogue,
        ratePlan.ratePlanCharges
      )
      chargeOverrides <- guardianWeekly.chargePairs
        .map(chargePair =>
          fromRatePlanCharge(
            alterZuoraProductRatePlanCharge(chargePair.chargeFromProduct, chargeCap),
            chargePair.chargeFromSubscription
          )
        )
        .sequence
        .map(_.flatten)
    } yield AddZuoraRatePlan(
      productRatePlanId = guardianWeekly.productRatePlan.id,
      contractEffectiveDate,
      chargeOverrides
    )
}

case class RemoveZuoraRatePlan(
    ratePlanId: String,
    contractEffectiveDate: LocalDate
)

object RemoveZuoraRatePlan {
  implicit val rw: ReadWriter[RemoveZuoraRatePlan] = macroRW
}

case class ChargeOverride(
    productRatePlanChargeId: ZuoraProductRatePlanChargeId,
    billingPeriod: String,
    price: BigDecimal
)

object ChargeOverride {
  implicit val rw: ReadWriter[ChargeOverride] = macroRW

  def fromRatePlan(
      pricingData: ZuoraPricingData,
      ratePlan: ZuoraRatePlan
  ): Either[AmendmentDataFailure, Seq[ChargeOverride]] =
    (for {
      ratePlanCharge <- ratePlan.ratePlanCharges
      productRatePlanCharge <- pricingData.get(ratePlanCharge.productRatePlanChargeId).toSeq
    } yield fromRatePlanCharge(productRatePlanCharge, ratePlanCharge)).sequence
      .map(_.flatten)

  /** <p>A <code>ChargeOverride</code> is defined iff the billing period of the given <code>ProductRatePlanCharge</code>
    * is different to the billing period of the given <code>RatePlanCharge</code>.</p>
    *
    * <p>If there isn't enough information in the given <code>ProductRatePlanCharge</code> and
    * <code>RatePlanCharge</code> to determine whether or not a <code>ChargeOverride</code> should be defined, the
    * result will be an AmendmentDataFailure.</p>
    */
  def fromRatePlanCharge(
      productRatePlanCharge: ZuoraProductRatePlanCharge,
      ratePlanCharge: ZuoraRatePlanCharge
  ): Either[AmendmentDataFailure, Option[ChargeOverride]] =
    for {
      billingPeriod <- ratePlanCharge.billingPeriod.toRight(
        AmendmentDataFailure(s"Rate plan charge ${ratePlanCharge.number} has no billing period")
      )
      productRatePlanChargeBillingPeriod <- productRatePlanCharge.billingPeriod.toRight(
        AmendmentDataFailure(s"Product rate plan charge ${ratePlanCharge.number} has no billing period")
      )

      productRatePlanChargePrice <- ZuoraPricing
        .pricing(productRatePlanCharge, ratePlanCharge.currency)
        .flatMap(_.price)
        .toRight(
          AmendmentDataFailure(
            s"Product rate plan charge ${productRatePlanCharge.id} has no price for currency ${ratePlanCharge.currency}"
          )
        )

      price <- AmendmentData.adjustedForBillingPeriod(
        productRatePlanChargePrice,
        Some(billingPeriod),
        Some(productRatePlanChargeBillingPeriod)
      )

      hasBeenPriceCapped = ZuoraPricing
        .pricing(productRatePlanCharge, ratePlanCharge.currency)
        .fold(false)(_.hasBeenPriceCapped)

    } yield {
      if (hasBeenPriceCapped) Some(ChargeOverride(productRatePlanCharge.id, billingPeriod, price))
      else if (billingPeriod == productRatePlanChargeBillingPeriod) None
      else Some(ChargeOverride(productRatePlanCharge.id, billingPeriod, price))
    }
}
