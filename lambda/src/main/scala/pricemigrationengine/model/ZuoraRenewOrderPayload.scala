package pricemigrationengine.model

import java.time.LocalDate
import upickle.default.{ReadWriter, macroRW}

case class ZuoraRenewOrderPayloadOrderActionTriggerDate(name: String, triggerDate: LocalDate)
object ZuoraRenewOrderPayloadOrderActionTriggerDate {
  implicit val rwZuoraRenewOrderPayloadOrderActionTriggerDate
      : ReadWriter[ZuoraRenewOrderPayloadOrderActionTriggerDate] = macroRW
}

case class ZuoraRenewOrderPayloadOrderAction(
    `type`: String,
    triggerDates: List[ZuoraRenewOrderPayloadOrderActionTriggerDate]
)
object ZuoraRenewOrderPayloadOrderAction {
  implicit val rwZuoraRenewOrderPayloadOrderAction: ReadWriter[ZuoraRenewOrderPayloadOrderAction] = macroRW
}

case class ZuoraRenewOrderPayloadSubscription(
    subscriptionNumber: String,
    orderActions: List[ZuoraRenewOrderPayloadOrderAction]
)
object ZuoraRenewOrderPayloadSubscription {
  implicit val rwZuoraRenewOrderPayloadSubscription: ReadWriter[ZuoraRenewOrderPayloadSubscription] = macroRW
}

case class ZuoraRenewOrderPayloadProcessingOptions(runBilling: Boolean, collectPayment: Boolean)
object ZuoraRenewOrderPayloadProcessingOptions {
  implicit val rwZuoraRenewOrderPayloadProcessingOptions: ReadWriter[ZuoraRenewOrderPayloadProcessingOptions] = macroRW
}

case class ZuoraRenewOrderPayload(
    orderDate: LocalDate,
    existingAccountNumber: String,
    subscriptions: List[ZuoraRenewOrderPayloadSubscription],
    processingOptions: ZuoraRenewOrderPayloadProcessingOptions
)
object ZuoraRenewOrderPayload {
  implicit val rwZuoraRenewOrderPayload: ReadWriter[ZuoraRenewOrderPayload] = macroRW

  def apply(
      subscriptionNumber: String,
      effectDate: LocalDate,
      accountNumber: String
  ): ZuoraRenewOrderPayload = {
    val triggerDates = List(
      ZuoraRenewOrderPayloadOrderActionTriggerDate(
        "ContractEffective",
        effectDate
      ),
      ZuoraRenewOrderPayloadOrderActionTriggerDate(
        "ServiceActivation",
        effectDate
      ),
      ZuoraRenewOrderPayloadOrderActionTriggerDate(
        "CustomerAcceptance",
        effectDate
      ),
    )

    val orderActions = List(
      ZuoraRenewOrderPayloadOrderAction(
        `type` = "RenewSubscription",
        triggerDates = triggerDates
      )
    )

    val subscriptions = List(
      ZuoraRenewOrderPayloadSubscription(
        subscriptionNumber = subscriptionNumber,
        orderActions = orderActions
      )
    )

    val processingOptions = ZuoraRenewOrderPayloadProcessingOptions(runBilling = false, collectPayment = false)

    ZuoraRenewOrderPayload(
      orderDate = effectDate,
      existingAccountNumber = accountNumber,
      subscriptions = subscriptions,
      processingOptions = processingOptions
    )
  }
}

case class ZuoraRenewOrderResponse(
    success: Boolean
    // Be careful if you are considering extending this class because the answer's shape
    // varies depending if the operation was successful or not.
    // See comments on this PR: https://github.com/guardian/price-migration-engine/pull/1085
)
object ZuoraRenewOrderResponse {
  implicit val rwZuoraRenewOrderResponse: ReadWriter[ZuoraRenewOrderResponse] = macroRW
}
