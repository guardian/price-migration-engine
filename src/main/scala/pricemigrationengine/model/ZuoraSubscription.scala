package pricemigrationengine.model

import java.time.LocalDate

import upickle.default._

case class ZuoraSubscription(
    subscriptionNumber: String,
    customerAcceptanceDate: LocalDate,
    contractEffectiveDate: LocalDate,
    ratePlans: List[ZuoraRatePlan] = Nil,
    accountNumber: String,
    accountId: String
)

object ZuoraSubscription {
  implicit val rwSubscription: ReadWriter[ZuoraSubscription] = macroRW
}
