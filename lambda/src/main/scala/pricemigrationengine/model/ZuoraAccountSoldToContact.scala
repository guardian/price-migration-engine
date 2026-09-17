package pricemigrationengine.model

import upickle.default._

case class ZuoraAccountSoldToContact(
    address1: Option[String],
    address2: Option[String],
    city: Option[String],
    zipCode: Option[String],
    state: Option[String],
    country: String
)

object ZuoraAccountSoldToContact {
  implicit val rwSubscription: ReadWriter[ZuoraAccountSoldToContact] = macroRW
}
