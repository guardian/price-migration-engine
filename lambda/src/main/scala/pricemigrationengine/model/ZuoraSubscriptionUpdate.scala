package pricemigrationengine.model

import java.time.LocalDate
import upickle.default.{ReadWriter, macroRW}

case class ZuoraSubscriptionUpdate(
    add: Seq[AddZuoraRatePlan],
    remove: Seq[RemoveZuoraRatePlan],
    currentTerm: Option[Int],
    currentTermPeriodType: Option[String]
)

object ZuoraSubscriptionUpdate {
  implicit val rw: ReadWriter[ZuoraSubscriptionUpdate] = macroRW
}

case class AddZuoraRatePlan(
    productRatePlanId: String,
    contractEffectiveDate: LocalDate,
    chargeOverrides: Seq[ChargeOverride] = Nil
)

object AddZuoraRatePlan {
  implicit val rw: ReadWriter[AddZuoraRatePlan] = macroRW
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
}
