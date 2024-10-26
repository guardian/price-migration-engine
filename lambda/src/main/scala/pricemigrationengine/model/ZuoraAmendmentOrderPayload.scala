package pricemigrationengine.model

import java.time.LocalDate
import upickle.default._

/*
  Author: Pascal
  Comment id: 4eb4b0a9

  This file was written in October 2024 when we introduced Zuora.applyAmendmentOrder with the aim
  of eventually decommissioning Zuora.updateSubscription.

  The documentation for this Order is available here:
  Zuora documentation: https://knowledgecenter.zuora.com/Zuora_Billing/Manage_subscription_transactions/Orders/Order_actions_tutorials/D_Replace_a_product_in_a_subscription

  The migration to the new Orders API is currently a work in progress and the choice of the
  model hierarchy in this file reflects the fact that we are migrating SupporterPlus2024
  from the old API to the new Orders API; in other words migrating the amendment a
  SupporterPlus2024 subscription from using Zuora.updateSubscription to using Zuora.applyAmendmentOrder
 */

/*
  Author: Pascal
  Comment id: cada56ad

  The model hierarchy presented in this file captures the making of a ZuoraAmendmentOrderPayload according
  to the schema presented here: https://knowledgecenter.zuora.com/Zuora_Billing/Manage_subscription_transactions/Orders/Order_actions_tutorials/D_Replace_a_product_in_a_subscription

  One interesting note about the design is using a sealed trait to represent the different types of an Order action,
  in the context of replacing a product in a subscription. Indeed, the two actions in that order are:

  {
    "type": "RemoveProduct",
    "triggerDates": [
        {
            "name": "ContractEffective",
            "triggerDate": "2024-11-28"
        },
        {
            "name": "ServiceActivation",
            "triggerDate": "2024-11-28"
        },
        {
            "name": "CustomerAcceptance",
            "triggerDate": "2024-11-28"
        }
    ],
    "removeProduct": {
        "ratePlanId": "8a12867e92c341870192c7c46bdb47d6"
    }
}

and

{
    "type": "AddProduct",
    "triggerDates": [
        {
            "name": "ContractEffective",
            "triggerDate": "2024-11-28"
        },
        {
            "name": "ServiceActivation",
            "triggerDate": "2024-11-28"
        },
        {
            "name": "CustomerAcceptance",
            "triggerDate": "2024-11-28"
        }
    ],
    "addProduct": {
        "productRatePlanId": "8a128ed885fc6ded018602296ace3eb8",
        "chargeOverrides": [
            {
                "productRatePlanChargeId": "8a128ed885fc6ded018602296af13eba",
                "pricing": {
                    "recurringFlatFee": {
                        "listPrice": 12
                    }
                }
            },
            {
                "productRatePlanChargeId": "8a128d7085fc6dec01860234cd075270",
                "pricing": {
                    "recurringFlatFee": {
                        "listPrice": 0
                    }
                }
            }
        ]
    }
}

They are respectively modeled as

ZuoraAmendmentOrderPayloadOrderActionRemove
and
ZuoraAmendmentOrderPayloadOrderActionAdd

Because of the way upickle works, we end up with a JSON serialization that has a "$type" field which is a bit annoying.
We can see it in the test data in SupporterPlus2024MigrationTest.

I have tried to submit the Orders payload to Zuora with the "$type" field left inside the JSON but
Zuora errors during the processing. (Technically, it should actually work, but I think that Zuora may be doing payload introspection
beyond the mandatory fields and is error'ing on the "$type" field)

I have tried to remove the "$type" field by providing a custom writer for the serialization of two classes, but
could not get that to work.

In the end the solution I have chosen was to remove the "$type" field from the JSON before sending it to Zuora,
which explains the function type_flush in the ZuoraLive implementation of applyAmendmentOrder

This is, for all intent and purpose a hack and in a future change we may try and get those
custom writers to work to avoid string manipulation in the implementation of applyAmendmentOrder.

 */

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
