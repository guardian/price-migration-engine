package pricemigrationengine.model

import pricemigrationengine.migrations.GuardianWeeklyMigration
import pricemigrationengine.model.ChargeOverride.fromRatePlanCharge

import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import pricemigrationengine.model.Either._
import pricemigrationengine.model.ZuoraProductCatalogue.productPricingMap
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

  def zuoraUpdate(
      account: ZuoraAccount,
      catalogue: ZuoraProductCatalogue,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      effectiveDate: LocalDate,
      enforcedPrice: Option[BigDecimal]
  ): Either[DataExtractionFailure, ZuoraSubscriptionUpdate] = {

    val activeRatePlans = (for {
      invoiceItem <- ZuoraInvoiceItem.items(invoiceList, subscription, effectiveDate)
      ratePlanCharge <- ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toSeq
      price <- ratePlanCharge.price.toSeq
      if price > 0
      ratePlan <- ZuoraRatePlan.ratePlanChargeToMatchingRatePlan(subscription, ratePlanCharge).toSeq
    } yield ratePlan).distinct

    if (activeRatePlans.isEmpty)
      Left(DataExtractionFailure(s"No rate plans to update for subscription ${subscription.subscriptionNumber}"))
    else if (activeRatePlans.size > 1)
      Left(DataExtractionFailure(s"Multiple rate plans to update: ${activeRatePlans.map(_.id)}"))
    else {
      val isZoneABC = activeRatePlans.filter(zoneABCPlanNames contains _.productName)
      val pricingData = productPricingMap(catalogue)

      activeRatePlans
        .map(
          if (isZoneABC.nonEmpty)
            AddZuoraRatePlan.fromRatePlanGuardianWeekly(account, catalogue, effectiveDate, enforcedPrice)
          else AddZuoraRatePlan.fromRatePlan(pricingData, effectiveDate, enforcedPrice)
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
      enforcedPrice: Option[BigDecimal]
  )(
      ratePlan: ZuoraRatePlan
  ): Either[DataExtractionFailure, AddZuoraRatePlan] = {
    for {
      chargeOverrides <- ChargeOverride.fromRatePlan(pricingData, ratePlan, enforcedPrice)
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
      enforcedPrice: Option[BigDecimal]
  )(
      ratePlan: ZuoraRatePlan
  ): Either[DataExtractionFailure, AddZuoraRatePlan] =
    for {
      guardianWeekly <- GuardianWeeklyMigration.getNewRatePlanCharges(
        account,
        catalogue,
        ratePlan.ratePlanCharges
      )
      chargeOverrides <- guardianWeekly.chargePairs
        .map(chargePair =>
          fromRatePlanCharge(
            chargePair.chargeFromProduct,
            chargePair.chargeFromSubscription,
            enforcedPrice
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
      enforcedPrice: Option[BigDecimal]
  ): Either[DataExtractionFailure, Seq[ChargeOverride]] =
    (for {
      ratePlanCharge <- ratePlan.ratePlanCharges
      productRatePlanCharge <- pricingData.get(ratePlanCharge.productRatePlanChargeId).toSeq
    } yield fromRatePlanCharge(productRatePlanCharge, ratePlanCharge, enforcedPrice)).sequence
      .map(_.flatten)

  def fromRatePlanCharge(
      productRatePlanCharge: ZuoraProductRatePlanCharge,
      ratePlanCharge: ZuoraRatePlanCharge,
      enforcedPrice: Option[BigDecimal]
  ): Either[DataExtractionFailure, Option[ChargeOverride]] =
    for {
      billingPeriod <- ratePlanCharge.billingPeriod.toRight(
        DataExtractionFailure(s"Rate plan charge ${ratePlanCharge.number} has no billing period")
      )
      productRatePlanChargeBillingPeriod <- productRatePlanCharge.billingPeriod.toRight(
        DataExtractionFailure(s"Product rate plan charge ${ratePlanCharge.number} has no billing period")
      )

      productRatePlanChargePrice <- ZuoraPricing
        .pricing(productRatePlanCharge, ratePlanCharge.currency)
        .flatMap(_.price)
        .toRight(
          DataExtractionFailure(
            s"Product rate plan charge ${productRatePlanCharge.id} has no price for currency ${ratePlanCharge.currency}"
          )
        )

      price <- AmendmentData.adjustedForBillingPeriod(
        productRatePlanChargePrice,
        Some(billingPeriod),
        Some(productRatePlanChargeBillingPeriod)
      )

    } yield {
      Some(ChargeOverride(productRatePlanCharge.id, billingPeriod, enforcedPrice.getOrElse(price)))
    }
}
