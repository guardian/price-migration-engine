package pricemigrationengine.model

import java.time.LocalDate

import upickle.default._

case class ZuoraSubscription(
    subscriptionNumber: String,
    id: String,
    version: Int,
    customerAcceptanceDate: LocalDate,
    contractEffectiveDate: LocalDate,
    subscriptionStartDate: LocalDate,
    ratePlans: List[ZuoraRatePlan],
    accountNumber: String,
    accountId: String,
    status: String,
    termStartDate: LocalDate,
    termEndDate: LocalDate,
    autoRenew: Boolean
)

object ZuoraSubscription {
  implicit val rwSubscription: ReadWriter[ZuoraSubscription] = macroRW
}
