package pricemigrationengine.model

import java.time.LocalDate
import upickle.default.{ReadWriter, macroRW}

// Zuora documentation: https://knowledgecenter.zuora.com/Zuora_Billing/Manage_subscription_transactions/Orders/Order_actions_tutorials/D_Replace_a_product_in_a_subscription

sealed trait ZuoraAmendmentOrderPayloadOrderAction
object ZuoraAmendmentOrderPayloadOrderAction {
  implicit val rw: ReadWriter[ZuoraAmendmentOrderPayloadOrderAction] = macroRW
}

case class ZuoraAmendmentOrderPayloadOrderActionTriggerDate(name: String, triggerDate: LocalDate)
object ZuoraAmendmentOrderPayloadOrderActionTriggerDate {
  implicit val rw: ReadWriter[ZuoraAmendmentOrderPayloadOrderActionTriggerDate] = macroRW
}

case class ZuoraAmendmentOrderPayloadOrderActionRemoveProduct(ratePlanId: String)
object ZuoraAmendmentOrderPayloadOrderActionRemoveProduct {
  implicit val rw: ReadWriter[ZuoraAmendmentOrderPayloadOrderActionRemoveProduct] = macroRW
}

case class ZuoraAmendmentOrderPayloadOrderActionRemove(
    triggerDates: List[ZuoraAmendmentOrderPayloadOrderActionTriggerDate],
    removeProduct: ZuoraAmendmentOrderPayloadOrderActionRemoveProduct
) extends ZuoraAmendmentOrderPayloadOrderAction
object ZuoraAmendmentOrderPayloadOrderActionRemove {
  implicit val rw: ReadWriter[ZuoraAmendmentOrderPayloadOrderActionRemove] = macroRW
}

case class ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride(
    productRatePlanChargeId: String,
    pricing: Map[String, Map[String, BigDecimal]]
)
object ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride {
  implicit val rw: ReadWriter[ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride] = macroRW
}

case class ZuoraAmendmentOrderPayloadOrderActionAddProduct(
    productRatePlanId: String,
    chargeOverrides: List[ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride]
)
object ZuoraAmendmentOrderPayloadOrderActionAddProduct {
  implicit val rw: ReadWriter[ZuoraAmendmentOrderPayloadOrderActionAddProduct] = macroRW
}

case class ZuoraAmendmentOrderPayloadOrderActionAdd(
    triggerDates: List[ZuoraAmendmentOrderPayloadOrderActionTriggerDate],
    addProduct: ZuoraAmendmentOrderPayloadOrderActionAddProduct
) extends ZuoraAmendmentOrderPayloadOrderAction
object ZuoraAmendmentOrderPayloadOrderActionAdd {
  implicit val rw: ReadWriter[ZuoraAmendmentOrderPayloadOrderActionAdd] = macroRW
}

case class ZuoraAmendmentOrderPayloadSubscription(
    subscriptionNumber: String,
    orderActions: List[ZuoraAmendmentOrderPayloadOrderAction]
)
object ZuoraAmendmentOrderPayloadSubscription {
  implicit val rw: ReadWriter[ZuoraAmendmentOrderPayloadSubscription] = macroRW
}

case class ZuoraAmendmentOrderPayloadProcessingOptions(runBilling: Boolean, collectPayment: Boolean)
object ZuoraAmendmentOrderPayloadProcessingOptions {
  implicit val rw: ReadWriter[ZuoraAmendmentOrderPayloadProcessingOptions] = macroRW
}

case class ZuoraAmendmentOrderPayload(
    orderDate: LocalDate,
    existingAccountNumber: String,
    subscriptions: List[ZuoraAmendmentOrderPayloadSubscription],
    processingOptions: ZuoraAmendmentOrderPayloadProcessingOptions
)
object ZuoraAmendmentOrderPayload {
  implicit val rw: ReadWriter[ZuoraAmendmentOrderPayload] = macroRW
}
