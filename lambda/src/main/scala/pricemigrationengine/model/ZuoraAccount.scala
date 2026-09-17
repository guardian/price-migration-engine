package pricemigrationengine.model

import upickle.default._

case class ZuoraAccountBasicInfo(accountNumber: String)
object ZuoraAccountBasicInfo {
  implicit val rwZuoraAccountBasicInfo: ReadWriter[ZuoraAccountBasicInfo] = macroRW
}

case class ZuoraAccount(
    basicInfo: ZuoraAccountBasicInfo,
    soldToContact: ZuoraAccountSoldToContact
)

object ZuoraAccount {
  implicit val rwZuoraAccount: ReadWriter[ZuoraAccount] = macroRW
}
