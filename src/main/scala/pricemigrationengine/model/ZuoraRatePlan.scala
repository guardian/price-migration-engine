package pricemigrationengine.model

case class ZuoraRatePlan(
    productName: String,
    ratePlanName: String,
    ratePlanCharges: List[ZuoraRatePlanCharge] = Nil,
    lastChangeType: Option[String] = None
)
