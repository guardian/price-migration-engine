package pricemigrationengine.model

import java.time.LocalDate
import upickle.default._

// Zuora documentation: https://knowledgecenter.zuora.com/Zuora_Billing/Manage_subscription_transactions/Orders/Order_actions_tutorials/D_Replace_a_product_in_a_subscription

sealed trait ZuoraAmendmentOrderPayloadOrderAction
object ZuoraAmendmentOrderPayloadOrderAction {
  implicit val w: Writer[ZuoraAmendmentOrderPayloadOrderAction] = Writer.merge(
    macroW[ZuoraAmendmentOrderPayloadOrderActionAdd],
    macroW[ZuoraAmendmentOrderPayloadOrderActionRemove]
  )
}
case class ZuoraAmendmentOrderPayloadOrderActionTriggerDate(name: String, triggerDate: LocalDate)
object ZuoraAmendmentOrderPayloadOrderActionTriggerDate {
  implicit val w: Writer[ZuoraAmendmentOrderPayloadOrderActionTriggerDate] = macroW
}

case class ZuoraAmendmentOrderPayloadOrderActionRemoveProduct(ratePlanId: String)
object ZuoraAmendmentOrderPayloadOrderActionRemoveProduct {
  implicit val w: Writer[ZuoraAmendmentOrderPayloadOrderActionRemoveProduct] = macroW
}

case class ZuoraAmendmentOrderPayloadOrderActionRemove(
    `type`: String,
    triggerDates: List[ZuoraAmendmentOrderPayloadOrderActionTriggerDate],
    removeProduct: ZuoraAmendmentOrderPayloadOrderActionRemoveProduct
) extends ZuoraAmendmentOrderPayloadOrderAction
object ZuoraAmendmentOrderPayloadOrderActionRemove {
  implicit val w: Writer[ZuoraAmendmentOrderPayloadOrderActionRemove] = macroW
}

case class ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride(
    productRatePlanChargeId: String,
    pricing: Map[String, Map[String, BigDecimal]]
)
object ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride {
  implicit val w: Writer[ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride] = macroW
}

case class ZuoraAmendmentOrderPayloadOrderActionAddProduct(
    productRatePlanId: String,
    chargeOverrides: List[ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride]
)
object ZuoraAmendmentOrderPayloadOrderActionAddProduct {
  implicit val w: Writer[ZuoraAmendmentOrderPayloadOrderActionAddProduct] = macroW
}

case class ZuoraAmendmentOrderPayloadOrderActionAdd(
    `type`: String,
    triggerDates: List[ZuoraAmendmentOrderPayloadOrderActionTriggerDate],
    addProduct: ZuoraAmendmentOrderPayloadOrderActionAddProduct
) extends ZuoraAmendmentOrderPayloadOrderAction
object ZuoraAmendmentOrderPayloadOrderActionAdd {
  implicit val w: Writer[ZuoraAmendmentOrderPayloadOrderActionAdd] = macroW
}

case class ZuoraAmendmentOrderPayloadSubscription(
    subscriptionNumber: String,
    orderActions: List[ZuoraAmendmentOrderPayloadOrderAction]
)
object ZuoraAmendmentOrderPayloadSubscription {
  implicit val w: Writer[ZuoraAmendmentOrderPayloadSubscription] = macroW
}

case class ZuoraAmendmentOrderPayloadProcessingOptions(runBilling: Boolean, collectPayment: Boolean)
object ZuoraAmendmentOrderPayloadProcessingOptions {
  implicit val w: Writer[ZuoraAmendmentOrderPayloadProcessingOptions] = macroW
}

case class ZuoraAmendmentOrderPayload(
    orderDate: LocalDate,
    existingAccountNumber: String,
    subscriptions: List[ZuoraAmendmentOrderPayloadSubscription],
    processingOptions: ZuoraAmendmentOrderPayloadProcessingOptions
)
object ZuoraAmendmentOrderPayload {
  implicit val w: Writer[ZuoraAmendmentOrderPayload] = macroW
}

case class ZuoraAmendmentOrderResponse(
    success: Boolean
    // Be careful if you are considering extending this class because the answer's shape
    // varies depending on whether the operation was successful or not.
)
object ZuoraAmendmentOrderResponse {
  implicit val rw: ReadWriter[ZuoraAmendmentOrderResponse] = macroRW
}
