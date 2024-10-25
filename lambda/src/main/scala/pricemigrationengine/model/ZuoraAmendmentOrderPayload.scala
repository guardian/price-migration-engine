package pricemigrationengine.model

import java.time.LocalDate
import upickle.default._

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

  /*

  We are using a custom JSON serialisation for this class because the JSON serialisation provided by upickle
  would produce:

  {
    "$type": "ZuoraAmendmentOrderPayloadOrderActionRemove",
    "triggerDates": [
      {
        "name": "ContractEffective",
        "triggerDate": "2024-11-26"
      },
      {
        "name": "ServiceActivation",
        "triggerDate": "2024-11-26"
      },
      {
        "name": "CustomerAcceptance",
        "triggerDate": "2024-11-26"
      }
    ],
    "removeProduct": {
      "ratePlanId": "8a128e208bdd4251018c0d5050970bd9"
    }
  }

  whereas we want:

  {
    "type": "RemoveProduct",
    "triggerDates": [
      {
        "name": "ContractEffective",
        "triggerDate": "2024-11-26"
      },
      {
        "name": "ServiceActivation",
        "triggerDate": "2024-11-26"
      },
      {
        "name": "CustomerAcceptance",
        "triggerDate": "2024-11-26"
      }
    ],
    "removeProduct": {
      "ratePlanId": "8a128e208bdd4251018c0d5050970bd9"
    }
  }

   */

  implicit val rw: ReadWriter[ZuoraAmendmentOrderPayloadOrderActionRemove] = {
    readwriter[ujson.Value].bimap[ZuoraAmendmentOrderPayloadOrderActionRemove](
      action =>
        ujson.Obj(
          "type" -> ujson.Str("RemoveProduct"),
          "triggerDates" -> writeJs(action.triggerDates),
          "removeProduct" -> writeJs(action.removeProduct)
        ),
      json =>
        ZuoraAmendmentOrderPayloadOrderActionRemove(
          triggerDates = read[List[ZuoraAmendmentOrderPayloadOrderActionTriggerDate]](json("triggerDates")),
          removeProduct = read[ZuoraAmendmentOrderPayloadOrderActionRemoveProduct](json("removeProduct"))
        )
    )
  }
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
  /*

  We are using a custom JSON serialisation for this class because the JSON serialisation provided by upickle
would produce:

  {
    "$type": "ZuoraAmendmentOrderPayloadOrderActionAdd",
    "triggerDates": [
      {
        "name": "ContractEffective",
        "triggerDate": "2024-11-26"
      },
      {
        "name": "ServiceActivation",
        "triggerDate": "2024-11-26"
      },
      {
        "name": "CustomerAcceptance",
        "triggerDate": "2024-11-26"
      }
    ],
    "addProduct": {
      "productRatePlanId": "8a128ed885fc6ded018602296ace3eb8",
      "chargeOverrides": [
        {
          "productRatePlanChargeId": "8a128ed885fc6ded018602296af13eba",
          "pricing": {
            "recurringFlatFee": {
              "listPrice": 15
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

  whereas we want:

  {
    "type": "AddProduct",
    "triggerDates": [
      {
        "name": "ContractEffective",
        "triggerDate": "2024-11-26"
      },
      {
        "name": "ServiceActivation",
        "triggerDate": "2024-11-26"
      },
      {
        "name": "CustomerAcceptance",
        "triggerDate": "2024-11-26"
      }
    ],
    "addProduct": {
      "productRatePlanId": "8a128ed885fc6ded018602296ace3eb8",
      "chargeOverrides": [
        {
          "productRatePlanChargeId": "8a128ed885fc6ded018602296af13eba",
          "pricing": {
            "recurringFlatFee": {
              "listPrice": 15
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

   */
  implicit val rw: ReadWriter[ZuoraAmendmentOrderPayloadOrderActionAdd] = {
    readwriter[ujson.Value].bimap[ZuoraAmendmentOrderPayloadOrderActionAdd](
      action =>
        ujson.Obj(
          "type" -> ujson.Str("AddProduct"),
          "triggerDates" -> writeJs(action.triggerDates),
          "addProduct" -> writeJs(action.addProduct)
        ),
      json =>
        ZuoraAmendmentOrderPayloadOrderActionAdd(
          triggerDates = read[List[ZuoraAmendmentOrderPayloadOrderActionTriggerDate]](json("triggerDates")),
          addProduct = read[ZuoraAmendmentOrderPayloadOrderActionAddProduct](json("addProduct"))
        )
    )
  }
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

case class ZuoraAmendmentOrderResponse(
    success: Boolean
    // Be careful if you are considering extending this class because the answer's shape
    // varies depending on whether the operation was successful or not.
)
object ZuoraAmendmentOrderResponse {
  implicit val rw: ReadWriter[ZuoraAmendmentOrderResponse] = macroRW
}
