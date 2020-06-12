package pricemigrationengine.model

import java.time.LocalDate

import pricemigrationengine.model.Either._
import upickle.default._

case class ZuoraSubscriptionUpdate(
    add: Seq[AddZuoraRatePlan],
    remove: Seq[RemoveZuoraRatePlan]
)

object ZuoraSubscriptionUpdate {
  implicit val rw: ReadWriter[ZuoraSubscriptionUpdate] = macroRW

  /**
    * Takes all non-discount rate plans participating in the invoice list on the given date,
    * and replaces them with their current equivalent.
    * This has the effect of updating their prices to the current ones in the product catalogue.
    *
    * If the billing period of a rate plan charge is different to the billing period of
    * the corresponding product rate plan charge, we have to add a charge override to the update
    * to adjust the price of the charge to the length of its billing period.
    */
  def updateOfRatePlansToCurrent(
      pricingData: ZuoraPricingData,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      date: LocalDate
  ): Either[AmendmentDataFailure, ZuoraSubscriptionUpdate] = {

    val ratePlans = (for {
      invoiceItem <- ZuoraInvoiceItem.items(invoiceList, subscription, date)
      ratePlanCharge <- ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toSeq
      price <- ratePlanCharge.price.toSeq
      if price > 0
      ratePlan <- ZuoraRatePlan.ratePlan(subscription, ratePlanCharge).toSeq
    } yield ratePlan).distinct

    if (ratePlans.isEmpty)
      Left(AmendmentDataFailure("No rate plans to update"))
    else if (ratePlans.size > 1)
      Left(AmendmentDataFailure(s"Multiple rate plans to update: ${ratePlans.map(_.id)}"))
    else {
      ratePlans.map(AddZuoraRatePlan.fromRatePlan(pricingData, date)).sequence.map { add =>
        ZuoraSubscriptionUpdate(
          add,
          remove = ratePlans.map(ratePlan => RemoveZuoraRatePlan(ratePlan.id, date))
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

  def fromRatePlan(pricingData: ZuoraPricingData, contractEffectiveDate: LocalDate)(
      ratePlan: ZuoraRatePlan
  ): Either[AmendmentDataFailure, AddZuoraRatePlan] =
    for {
      chargeOverrides <- ChargeOverride.fromRatePlan(pricingData, ratePlan)
    } yield AddZuoraRatePlan(productRatePlanId = ratePlan.productRatePlanId, contractEffectiveDate, chargeOverrides)
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

  /**
    * <p>A <code>ChargeOverride</code> is defined iff the billing period of the given <code>ProductRatePlanCharge</code>
    * is different to the billing period of the given <code>RatePlanCharge</code>.</p>
    *
    * <p>If there isn't enough information in the given <code>ProductRatePlanCharge</code> and
    * <code>RatePlanCharge</code> to determine whether or not a <code>ChargeOverride</code> should be defined,
    * the result will be an AmendmentDataFailure.</p>
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
    } yield
      if (billingPeriod == productRatePlanChargeBillingPeriod) None
      else Some(ChargeOverride(productRatePlanCharge.id, billingPeriod, price))
}
