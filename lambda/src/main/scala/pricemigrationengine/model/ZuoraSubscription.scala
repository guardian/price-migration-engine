package pricemigrationengine.model

import java.time.LocalDate

import pricemigrationengine.model.OptionReader // not sure why this import is needed as should be visible implicitly
import upickle.default._

case class ZuoraSubscription(
    subscriptionNumber: String,
    customerAcceptanceDate: LocalDate,
    contractEffectiveDate: LocalDate,
    ratePlans: List[ZuoraRatePlan],
    accountNumber: String,
    accountId: String
)

object ZuoraSubscription {
  implicit val rwSubscription: ReadWriter[ZuoraSubscription] = macroRW
}

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

  def ratePlan(subscription: ZuoraSubscription, ratePlanChargeNumber: String): Option[ZuoraRatePlan] =
    subscription.ratePlans.find(_.ratePlanCharges.exists(_.number == ratePlanChargeNumber))
}

case class ZuoraRatePlanCharge(
    productRatePlanChargeId: ZuoraProductRatePlanChargeId,
    number: String,
    name: String,
    price: Double,
    billingPeriod: Option[String] = None,
    effectiveStartDate: LocalDate,
    chargedThroughDate: Option[LocalDate] = None,
    processedThroughDate: Option[LocalDate] = None,
    specificBillingPeriod: Option[Int] = None,
    endDateCondition: Option[String] = None,
    upToPeriodsType: Option[String] = None,
    upToPeriods: Option[Int] = None,
    billingDay: Option[String] = None,
    triggerEvent: Option[String] = None,
    triggerDate: Option[LocalDate] = None,
    discountPercentage: Option[Double] = None
)

object ZuoraRatePlanCharge {
  implicit val rw: ReadWriter[ZuoraRatePlanCharge] = macroRW
}
