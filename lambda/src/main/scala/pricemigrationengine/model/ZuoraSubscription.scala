package pricemigrationengine.model

import java.time.LocalDate

import pricemigrationengine.model.OptionReader // not sure why this import is needed as should be visible implicitly
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
    termEndDate: LocalDate
)

object ZuoraSubscription {
  implicit val rwSubscription: ReadWriter[ZuoraSubscription] = macroRW
}
