package pricemigrationengine.model

import pricemigrationengine.model.OptionReader // not sure why this import is needed as should be visible implicitly
import upickle.default._

case class ZuoraRatePlan(
    id: String,
    productName: String,
    productRatePlanId: String,
    ratePlanName: String,
    ratePlanCharges: List[ZuoraRatePlanCharge],
    lastChangeType: Option[String] = None
)

object ZuoraRatePlan {
  implicit val rw: ReadWriter[ZuoraRatePlan] = macroRW

  def ratePlanToCurrency(ratePlan: ZuoraRatePlan): Option[String] = {
    ratePlan.ratePlanCharges.headOption.map(_.currency)
  }

  def ratePlanToChargesBillingPeriods(ratePlan: ZuoraRatePlan): List[BillingPeriod] = {
    ratePlan.ratePlanCharges.flatMap(rpc => rpc.billingPeriod.map(bp => BillingPeriod.fromString(bp)))
  }

  def ratePlanToOptionalUniquelyDeterminedBillingPeriod(ratePlan: ZuoraRatePlan): Option[BillingPeriod] = {
    // Date: October 2025
    // Author: Pascal

    // "billing period" is not an attribute of a rate plan, but that of its rate plan charges. In fact
    // we just discovered that there has been subs in the past carrying rate plans with
    // multiple billing period among their charges.

    // With that said it's convenient to think of the rate plan has having a billing period
    // during price migrations and product re-structurations, but only if it's shared among all charges.

    // It in unlikely that active rate plans with multiple billing periods will occur in the future
    // but if they do and didn't happen by accident, then a bit of refactoring across the engine
    // will be necessary. For the moment we will simply error if that happens.

    val billingPeriods: List[BillingPeriod] = ratePlanToChargesBillingPeriods(ratePlan).distinct
    billingPeriods.size match {
      case 0 => None
      case 1 => billingPeriods.headOption
      case _ => throw new Exception(s"[d6c35e2f] rate plan with multiple billing periods, rate plan: ${ratePlan}")
    }
  }

  def ratePlanIsActive(ratePlan: ZuoraRatePlan): Boolean = {
    // `lastChangeType` is not always defined on all rate plans. The situation is:
    //     - Not defined                    -> Active rate plan
    //     - Defined and value is "Add"     -> Active rate plan
    //     - Defined and value is "Remove"  -> Non active rate plan
    !ratePlan.lastChangeType.contains("Remove")
  }
}
