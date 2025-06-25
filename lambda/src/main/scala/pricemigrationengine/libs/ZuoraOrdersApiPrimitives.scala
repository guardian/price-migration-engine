package pricemigrationengine.libs

import java.time.LocalDate
import upickle.default._
import ujson._

// This file contains the primitives to be able to construct the Orders API Payload
// described here:
// https://knowledgecenter.zuora.com/Zuora_Billing/Manage_subscription_transactions/Orders/Order_actions_tutorials/D_Replace_a_product_in_a_subscription

object ZuoraOrdersApiPrimitives {

  /*
    // We keep this example for future reference and add some built in documentation to the code

    def example(): Value = {
      val json: Value = Obj(
        "name" -> Str("Alice"),
        "age" -> Num(30),
        "isMember" -> Bool(true),
        "tags" -> Arr(Str("scala"), Str("json"))
      )
      val pretty = ujson.write(json, indent = 4)
      json
    }
   */

  def singletonDate(name: String, datestr: String): Value = {
    /*
        {
            "name": "ContractEffective",
            "triggerDate": "2024-11-28"
        }
     */
    Obj(
      "name" -> Str(name),
      "triggerDate" -> Str(datestr),
    )
  }
  def triggerDates(datestr: String): Value = {
    // This is a simplified version which assumes that all three dates
    // If we encounter a situation where they are different, we will update
    // the signature of the function
    /*
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
       ]
     */
    Arr(
      singletonDate("ContractEffective", datestr),
      singletonDate("ServiceActivation", datestr),
      singletonDate("CustomerAcceptance", datestr)
    )
  }
  def removeProduct(triggerDateString: String, subscriptionRatePlanId: String): Value = {
    /*
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
     */
    Obj(
      "type" -> Str("RemoveProduct"),
      "triggerDates" -> triggerDates(triggerDateString: String),
      "removeProduct" -> Obj(
        "ratePlanId" -> Str(subscriptionRatePlanId)
      )
    )
  }

  def chargeOverride(productRatePlanChargeId: String, listPrice: BigDecimal): Value = {
    /*
        {
            "productRatePlanChargeId": "8a128ed885fc6ded018602296af13eba",
            "pricing": {
                "recurringFlatFee": {
                    "listPrice": 12
                }
            }
        }
     */
    Obj(
      "productRatePlanChargeId" -> Str(productRatePlanChargeId),
      "pricing" -> Obj(
        "recurringFlatFee" -> Obj(
          "listPrice" -> Num(listPrice.doubleValue)
        )
      )
    )
  }

  def addProduct(triggerDateString: String, productRatePlanId: String, chargeOverrides: List[Value]): Value = {
    /*
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
     */
    Obj(
      "type" -> Str("AddProduct"),
      "triggerDates" -> triggerDates(triggerDateString),
      "addProduct" -> Obj(
        "productRatePlanId" -> Str(productRatePlanId),
        "chargeOverrides" -> chargeOverrides
      )
    )
  }

  def subscription(subscriptionNumber: String, removals: List[Value], additions: List[Value]): Value = {
    /*
      {
        "subscriptionNumber": "a1809f5e84dd",
        "orderActions": [
          {
            "type": "RemoveProduct",
            "triggerDates": [
              {
                "name": "ContractEffective",
                "triggerDate": "2025-05-19"
              },
              {
                "name": "ServiceActivation",
                "triggerDate": "2025-05-19"
              },
              {
                "name": "CustomerAcceptance",
                "triggerDate": "2025-05-19"
              }
            ],
            "removeProduct": {
                "ratePlanId": "8a12867e92c341870192c7c46bdb47d6"
            }
          },
          {
            "type": "AddProduct",
            "triggerDates": [
              {
                "name": "ContractEffective",
                "triggerDate": "2025-05-20"
              },
              {
                "name": "ServiceActivation",
                "triggerDate": "2025-05-20"
              },
              {
                "name": "CustomerAcceptance",
                "triggerDate": "2025-05-20"
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
        ]
      }
     */

    val actions = removals ++ additions
    Obj(
      "subscriptionNumber" -> Str(subscriptionNumber),
      "orderActions" -> ujson.Arr(actions: _*)
    )
  }

  def replace_a_product_in_a_subscription(
      orderDate: String,
      existingAccountNumber: String,
      subscription: Value
  ): Value = {
    /*
      {
        "orderDate": "2025-05-20",
        "existingAccountNumber": "4f3d6ed065437111b3",
        "subscriptions": [
          {
            "subscriptionNumber": "a1809f5e84dd",
            "orderActions": [
              {
                "type": "RemoveProduct",
                "triggerDates": [
                  {
                    "name": "ContractEffective",
                    "triggerDate": "2025-05-19"
                  },
                  {
                    "name": "ServiceActivation",
                    "triggerDate": "2025-05-19"
                  },
                  {
                    "name": "CustomerAcceptance",
                    "triggerDate": "2025-05-19"
                  }
                ],
                "removeProduct": {
                    "ratePlanId": "8a12867e92c341870192c7c46bdb47d6"
                }
              },
              {
                "type": "AddProduct",
                "triggerDates": [
                  {
                    "name": "ContractEffective",
                    "triggerDate": "2025-05-20"
                  },
                  {
                    "name": "ServiceActivation",
                    "triggerDate": "2025-05-20"
                  },
                  {
                    "name": "CustomerAcceptance",
                    "triggerDate": "2025-05-20"
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
            ]
          }
        ],
        "processingOptions": {
          "runBilling": false,
          "collectPayment": false
        }
      }
     */
    Obj(
      "orderDate" -> Str(orderDate),
      "existingAccountNumber" -> Str(existingAccountNumber),
      "subscriptions" -> Arr(subscription),
      "processingOptions" -> Obj(
        "runBilling" -> Bool(false),
        "collectPayment" -> Bool(false)
      )
    )
  }
}
