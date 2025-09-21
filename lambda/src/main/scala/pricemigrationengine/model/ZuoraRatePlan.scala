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

  def ratePlanToBillingPeriod(ratePlan: ZuoraRatePlan): Option[BillingPeriod] = {
    for {
      ratePlanCharge <- ratePlan.ratePlanCharges.headOption
      billingPeriod <- ratePlanCharge.billingPeriod
    } yield BillingPeriod.fromString(billingPeriod)
  }

  def ratePlanIsActive(ratePlan: ZuoraRatePlan): Boolean = {
    // `lastChangeType` is not always defined on all rate plans. The situation is:
    //     - Not defined                    -> Active rate plan
    //     - Defined and value is "Add"     -> Active rate plan
    //     - Defined and value is "Remove"  -> Non active rate plan
    !ratePlan.lastChangeType.contains("Remove")
  }
}
