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
      priceCorrectionFactor: BigDecimal
  ): Either[AmendmentDataFailure, ZuoraSubscriptionUpdate] = {

    val activeRatePlans = (for {
      invoiceItem <- ZuoraInvoiceItem.items(invoiceList, subscription, effectiveDate)
      ratePlanCharge <- ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toSeq
      price <- ratePlanCharge.price.toSeq
      if price > 0
      ratePlan <- ZuoraRatePlan.ratePlan(subscription, ratePlanCharge).toSeq
    } yield ratePlan).distinct

    if (activeRatePlans.isEmpty)
      Left(AmendmentDataFailure(s"No rate plans to update for subscription ${subscription.subscriptionNumber}"))
    else if (activeRatePlans.size > 1)
      Left(AmendmentDataFailure(s"Multiple rate plans to update: ${activeRatePlans.map(_.id)}"))
    else {
      val isZoneABC = activeRatePlans.filter(zoneABCPlanNames contains _.productName)
      val pricingData = productPricingMap(catalogue)

      activeRatePlans
        .map(
          if (isZoneABC.nonEmpty)
            AddZuoraRatePlan.fromRatePlanGuardianWeekly(account, catalogue, effectiveDate, priceCorrectionFactor)
          else AddZuoraRatePlan.fromRatePlan(pricingData, effectiveDate, priceCorrectionFactor)
        )
        .sequence
        .map { add =>
          val isTermTooShort = subscription.termEndDate.isBefore(effectiveDate)
          ZuoraSubscriptionUpdate(
            add,
            remove = activeRatePlans.map(ratePlan => RemoveZuoraRatePlan(ratePlan.id, effectiveDate)),
            currentTerm =
              if (isTermTooShort)
                Some(subscription.termStartDate.until(effectiveDate, DAYS).toInt)
              else None,
            currentTermPeriodType = if (isTermTooShort) Some("Day") else None
          )
        }
    }
  }

  def updateOfRatePlansToCurrent_Membership2023_Monthlies(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      effectiveDate: LocalDate,
  ): Either[AmendmentDataFailure, ZuoraSubscriptionUpdate] = {

    // This variant has a simpler signature than its classic counterpart.

    val activeRatePlans = (for {
      invoiceItem <- ZuoraInvoiceItem.items(invoiceList, subscription, effectiveDate)
      ratePlanCharge <- ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toSeq
      price <- ratePlanCharge.price.toSeq
      if price > 0
      ratePlan <- ZuoraRatePlan.ratePlan(subscription, ratePlanCharge).toSeq
    } yield ratePlan).distinct

    if (activeRatePlans.isEmpty)
      Left(AmendmentDataFailure(s"No rate plans to update for subscription ${subscription.subscriptionNumber}"))
    else if (activeRatePlans.size > 1)
      Left(AmendmentDataFailure(s"Multiple rate plans to update: ${activeRatePlans.map(_.id)}"))
    else {

      // At this point we know that we have exactly one activeRatePlans
      val activeRatePlan = activeRatePlans.head

      // In the case of Membership Batch 1 and 2 (monthlies), things are now more simple. We can hardcode the rate plan
      Right(
        ZuoraSubscriptionUpdate(
          add = List(AddZuoraRatePlan("8a1287c586832d250186a2040b1548fe", effectiveDate)),
          remove = List(RemoveZuoraRatePlan(activeRatePlan.id, effectiveDate)),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    }
  }

  def updateOfRatePlansToCurrent_Membership2023_Annuals(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      effectiveDate: LocalDate,
  ): Either[AmendmentDataFailure, ZuoraSubscriptionUpdate] = {

    // This variant has a simpler signature than its classic counterpart.

    val activeRatePlans = (for {
      invoiceItem <- ZuoraInvoiceItem.items(invoiceList, subscription, effectiveDate)
      ratePlanCharge <- ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toSeq
      price <- ratePlanCharge.price.toSeq
      if price > 0
      ratePlan <- ZuoraRatePlan.ratePlan(subscription, ratePlanCharge).toSeq
    } yield ratePlan).distinct

    if (activeRatePlans.isEmpty)
      Left(AmendmentDataFailure(s"No rate plans to update for subscription ${subscription.subscriptionNumber}"))
    else if (activeRatePlans.size > 1)
      Left(AmendmentDataFailure(s"Multiple rate plans to update: ${activeRatePlans.map(_.id)}"))
    else {

      // At this point we know that we have exactly one activeRatePlans
      val activeRatePlan = activeRatePlans.head

      // Batch 3 (annuals)
      Right(
        ZuoraSubscriptionUpdate(
          add = List(AddZuoraRatePlan("8a129ce886834fa90186a20c3ee70b6a", effectiveDate)),
          remove = List(RemoveZuoraRatePlan(activeRatePlan.id, effectiveDate)),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    }
  }

  def updateOfRatePlansToCurrent_SupporterPlus2023V1V2_Annuals(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      effectiveDate: LocalDate,
  ): Either[AmendmentDataFailure, ZuoraSubscriptionUpdate] = {

    // This variant has a simpler signature than its classic counterpart.

    val activeRatePlans = (for {
      invoiceItem <- ZuoraInvoiceItem.items(invoiceList, subscription, effectiveDate)
      ratePlanCharge <- ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toSeq
      price <- ratePlanCharge.price.toSeq
      if price > 0
      ratePlan <- ZuoraRatePlan.ratePlan(subscription, ratePlanCharge).toSeq
    } yield ratePlan).distinct

    if (activeRatePlans.isEmpty)
      Left(AmendmentDataFailure(s"No rate plans to update for subscription ${subscription.subscriptionNumber}"))
    else if (activeRatePlans.size > 1)
      Left(AmendmentDataFailure(s"Multiple rate plans to update: ${activeRatePlans.map(_.id)}"))
    else {

      // At this point we know that we have exactly one activeRatePlans
      val activeRatePlan = activeRatePlans.head

      Right(
        ZuoraSubscriptionUpdate(
          add = List(AddZuoraRatePlan("8a128ed885fc6ded01860228f77e3d5a", effectiveDate)),
          remove = List(RemoveZuoraRatePlan(activeRatePlan.id, effectiveDate)),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
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

  def fromRatePlan(
      pricingData: ZuoraPricingData,
      contractEffectiveDate: LocalDate,
      priceCorrectionFactor: BigDecimal
  )(
      ratePlan: ZuoraRatePlan
  ): Either[AmendmentDataFailure, AddZuoraRatePlan] = {
    for {
      chargeOverrides <- ChargeOverride.fromRatePlan(pricingData, ratePlan, priceCorrectionFactor)
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
      priceCorrectionFactor: BigDecimal
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
            chargePair.chargeFromProduct,
            chargePair.chargeFromSubscription,
            priceCorrectionFactor
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
      ratePlan: ZuoraRatePlan,
      priceCorrectionFactor: BigDecimal
  ): Either[AmendmentDataFailure, Seq[ChargeOverride]] =
    (for {
      ratePlanCharge <- ratePlan.ratePlanCharges
      productRatePlanCharge <- pricingData.get(ratePlanCharge.productRatePlanChargeId).toSeq
    } yield fromRatePlanCharge(productRatePlanCharge, ratePlanCharge, priceCorrectionFactor)).sequence
      .map(_.flatten)

  def fromRatePlanCharge(
      productRatePlanCharge: ZuoraProductRatePlanCharge,
      ratePlanCharge: ZuoraRatePlanCharge,
      priceCorrectionFactor: BigDecimal
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

    } yield {
      Some(ChargeOverride(productRatePlanCharge.id, billingPeriod, price * priceCorrectionFactor))
    }
}
