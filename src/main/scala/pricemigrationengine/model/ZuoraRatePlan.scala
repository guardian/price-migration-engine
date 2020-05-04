package pricemigrationengine.model

import upickle.default.{ReadWriter, macroRW}

case class ZuoraRatePlan(
    productName: String,
    ratePlanName: String,
    ratePlanCharges: List[ZuoraRatePlanCharge] = Nil,
    lastChangeType: Option[String] = None
)

object ZuoraRatePlan {
  implicit val rw: ReadWriter[ZuoraRatePlan] = macroRW
}
