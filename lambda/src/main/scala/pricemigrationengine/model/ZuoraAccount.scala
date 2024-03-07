package pricemigrationengine.model

import pricemigrationengine.model.OptionReader // not sure why this import is needed as should be visible implicitly
import upickle.default._

case class ZuoraAccount(
    soldToContact: SoldToContact
)

object ZuoraAccount {
  implicit val rwSubscription: ReadWriter[ZuoraAccount] = macroRW
}
