package pricemigrationengine.libs

import pricemigrationengine.model.ZuoraRatePlanCharge

import java.time.LocalDate
import upickle.default._
import ujson._

import scala.math.BigDecimal.RoundingMode

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
      "triggerDates" -> triggerDates(triggerDateString),
      "removeProduct" -> Obj(
        "ratePlanId" -> Str(subscriptionRatePlanId)
      )
    )
  }

  def chargeOverride(productRatePlanChargeId: String, listPrice: BigDecimal, billingPeriod: String): Value = {
    /*
        {
            "productRatePlanChargeId": "8a128ed885fc6ded018602296af13eba",
            "pricing": {
                "recurringFlatFee": {
                    "listPrice": 12
                }
            },
            "billing": {
                "billingPeriod": "Month"
            }
        }
     */
    Obj(
      "productRatePlanChargeId" -> Str(productRatePlanChargeId),
      "pricing" -> Obj(
        "recurringFlatFee" -> Obj(
          "listPrice" -> Num(listPrice.doubleValue)
        )
      ),
      "billing" -> Obj(
        "billingPeriod" -> Str(billingPeriod)
      )
    )
  }

  def ratePlanChargesToChargeOverrides(
      ratePlanCharges: List[ZuoraRatePlanCharge],
      priceRatio: BigDecimal,
      billingPeriod: String
  ): List[Value] = {
    // This functions is a more general case of the previous function (`chargeOverride`)
    // We originally introduced `chargeOverride` for price increases that have a single charge
    // in the rate plan that is being price increased, but cases such as Newspaper2025(P1) and
    // HomeDelivery2025 have rate plans containing charges for each day of the week (or a subset
    // of days of the week). In those cases we want to provide the set of ZuoraRatePlanCharge
    // and get a corresponding set of charge overrides objects.

    // An additional complexity is that in the simple case, we know the listing price
    // which is the price we are moving to. In the case of a migration involving increases
    // across different charges, we instead are going to provide the effective increase ratio.
    // For instance if the old price of the entire subscription was £50 and the new price (the price
    // carried by the cohort item, in other words the price we communicated to the user) is
    // £60, which corresponds to a 20% increase, and assuming that the individual charges before
    // price increase are £10, £20, £7 and £13 (note that the sum of values is 50), then the new
    // post price increase charges should be 10 + 20%, 20 + 20%, 7 +20% and 13 + 20%.
    // Consequently the signature of this functions include a parameter for the effective
    // price increase ratio (we express the percentage as a ratio, so for instance a 20% increase
    // will be a price ratio of 1.2).

    // Note that we use RoundingMode.DOWN instead of the more classical RoundingMode.HALF_UP, because we do not want
    // a rounding up to accidentally set the final price higher than the originally computed estimation price,
    // because then that would trigger the post amendment price check error

    ratePlanCharges.map { rpc =>
      Obj(
        "productRatePlanChargeId" -> Str(rpc.productRatePlanChargeId),
        "pricing" -> Obj(
          "recurringFlatFee" -> Obj(
            "listPrice" -> Num((rpc.price.get * priceRatio).setScale(2, RoundingMode.DOWN).doubleValue) // [1]
          )
        ),
        "billing" -> Obj(
          "billingPeriod" -> Str(billingPeriod)
        )
      )
    }

    // [1] The `get` method here *will* cause a runtime exception if it turns out that
    // the rate plan charge didn't have a price attached to it. This will cause the engine to
    // crashland in flames and alarm, but this is what we want.
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
                      },
                      "billing": {
                          "billingPeriod": "Month"
                      }
                  },
                  {
                      "productRatePlanChargeId": "8a128d7085fc6dec01860234cd075270",
                      "pricing": {
                          "recurringFlatFee": {
                              "listPrice": 0
                          }
                      },
                      "billing": {
                          "billingPeriod": "Month"
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
                        },
                        "billing": {
                            "billingPeriod": "Month"
                        }
                    },
                    {
                        "productRatePlanChargeId": "8a128d7085fc6dec01860234cd075270",
                        "pricing": {
                            "recurringFlatFee": {
                                "listPrice": 0
                            }
                        },
                        "billing": {
                            "billingPeriod": "Month"
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

  def processingOptions(): Value = {
    Obj(
      "runBilling" -> Bool(false),
      "collectPayment" -> Bool(false)
    )
  }

  def subscriptionUpdatePayload(
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
                            },
                            "billing": {
                                "billingPeriod": "Month"
                            }
                        },
                        {
                            "productRatePlanChargeId": "8a128d7085fc6dec01860234cd075270",
                            "pricing": {
                                "recurringFlatFee": {
                                    "listPrice": 0
                                }
                            },
                            "billing": {
                                "billingPeriod": "Month"
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
      "processingOptions" -> processingOptions()
    )
  }

  def subscriptionRenewalPayload(
      orderDate: String,
      existingAccountNumber: String,
      subscriptionNumber: String,
      triggerDate: String
  ): Value = {
    /*
        {
            "orderDate": "2025-08-12",
            "existingAccountNumber": "A-NUMBER",
            "subscriptions": [
                {
                    "subscriptionNumber": "A-NUMBER",
                    "orderActions": [
                        {
                            "type": "RenewSubscription",
                            "triggerDates": [
                                {
                                    "name": "ContractEffective",
                                    "triggerDate": "2025-08-12"
                                },
                                {
                                    "name": "ServiceActivation",
                                    "triggerDate": "2025-08-12"
                                },
                                {
                                    "name": "CustomerAcceptance",
                                    "triggerDate": "2025-08-12"
                                }
                            ]
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
      "subscriptions" -> Arr(
        Obj(
          "subscriptionNumber" -> Str(subscriptionNumber),
          "orderActions" -> Arr(
            Obj(
              "type" -> Str("RenewSubscription"),
              "triggerDates" -> triggerDates(triggerDate)
            )
          )
        )
      ),
      "processingOptions" -> processingOptions()
    )
  }
}
