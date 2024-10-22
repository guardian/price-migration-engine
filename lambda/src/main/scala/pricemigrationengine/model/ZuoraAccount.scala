package pricemigrationengine.model

import pricemigrationengine.model.OptionReader // not sure why this import is needed as should be visible implicitly
import upickle.default._

case class ZuoraAccountBasicInfo(accountNumber: String)
object ZuoraAccountBasicInfo {
  implicit val rwZuoraAccountBasicInfo: ReadWriter[ZuoraAccountBasicInfo] = macroRW
}

case class ZuoraAccount(
    basicInfo: ZuoraAccountBasicInfo,
    soldToContact: SoldToContact
)

object ZuoraAccount {
  implicit val rwZuoraAccount: ReadWriter[ZuoraAccount] = macroRW
}
