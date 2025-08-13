package pricemigrationengine.model

import upickle.default.{ReadWriter, macroRW}

case class ZuoraRenewOrderResponse(
    success: Boolean
    // Be careful if you are considering extending this class because the answer's shape
    // varies depending on whether the operation was successful or not.
    // See comments on this PR: https://github.com/guardian/price-migration-engine/pull/1085
)
object ZuoraRenewOrderResponse {
  implicit val rwZuoraRenewOrderResponse: ReadWriter[ZuoraRenewOrderResponse] = macroRW
}
