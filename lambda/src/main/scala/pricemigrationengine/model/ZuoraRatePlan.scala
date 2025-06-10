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

  def ratePlanChargeToMatchingRatePlan(
      subscription: ZuoraSubscription,
      ratePlanCharge: ZuoraRatePlanCharge
  ): Option[ZuoraRatePlan] =
    subscription.ratePlans.find(_.ratePlanCharges.exists(_.number == ratePlanCharge.number))

  def ratePlanToCurrency(ratePlan: ZuoraRatePlan): Option[String] = {
    ratePlan.ratePlanCharges.headOption.map(_.currency)
  }

  def ratePlanToBillingPeriod(ratePlan: ZuoraRatePlan): Option[BillingPeriod] = {
    for {
      ratePlanCharge <- ratePlan.ratePlanCharges.headOption
      billingPeriod <- ratePlanCharge.billingPeriod
    } yield BillingPeriod.fromString(billingPeriod)
  }

  // Sadly `lastChangeType` is not always defined on all rate plans. The situation is:
  //     - Not defined                    -> Active rate plan
  //     - Defined and value is "Add"     -> Active rate plan
  //     - Defined and value is "Removed" -> Non active rate plan

  def ratePlanIsActive(ratePlan: ZuoraRatePlan): Boolean = {
    !ratePlan.lastChangeType.contains("Removed")
  }
}
