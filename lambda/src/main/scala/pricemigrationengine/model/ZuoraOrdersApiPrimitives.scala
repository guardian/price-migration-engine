package pricemigrationengine.model

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
      targetNewPrice: BigDecimal,
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

    // Following the "pennies incident" of 12th September 2025 (date at which it was reported),
    // were it was highlighted that the default rounding down after a percentage increase in the
    // computation of the charge override prices, we added the target price, the estimated new price,
    // to the parameters of this function. We must ensure that after ratio increase and truncation, the
    // sum of all charge prices is equal to the estimated new price.

    val ratePlanChargesWithUpdatedPrices = ratePlanCharges.map { rpc =>
      val newPrice = rpc.price.map(p => (p * priceRatio).setScale(2, RoundingMode.DOWN))
      rpc.copy(price = newPrice)
    }

    // At this point we have modified rate plan charges whose prices have been computed as a
    // ratio increase of the old rate plan charges, and then truncated at 0.01.

    // We now need to ensure that the total prices are the estimated new prices (this is not
    // automatically true due to the roundings)

    val totalPrice = ratePlanChargesWithUpdatedPrices.map(_.price.get).sum // [1]

    // [1] The `get` method here *will* cause a runtime exception if it turns out that
    // the rate plan charge didn't have a price attached to it. This is a very pathological situation
    // and will cause the engine to crashland in flames and alarm, but this is what we want.

    val difference = targetNewPrice - totalPrice

    // `difference` is now what we need to add to one of the legs.

    // Let's add it to the first one.

    val firstRatePlanChargeAdjusted: List[ZuoraRatePlanCharge] = ratePlanChargesWithUpdatedPrices.take(1).map { rpc =>
      val newPrice = rpc.price.map(p => p + difference)
      rpc.copy(price = newPrice)
    }

    val ratePlanChargesWithFinalNewPrices = firstRatePlanChargeAdjusted ++ ratePlanChargesWithUpdatedPrices.drop(1)

    val jsonData = ratePlanChargesWithFinalNewPrices.map { rpc =>
      Obj(
        "productRatePlanChargeId" -> Str(rpc.productRatePlanChargeId),
        "pricing" -> Obj(
          "recurringFlatFee" -> Obj(
            "listPrice" -> Num(rpc.price.get.doubleValue)
          )
        ),
        "billing" -> Obj(
          "billingPeriod" -> Str(billingPeriod)
        )
      )
    }

    val totalListPriceFromJsonArray: Double =
      jsonData.map(js => js("pricing")("recurringFlatFee")("listPrice").num).sum

    // We perform a check and throw an exception if the sum do not match within margin error
    if ((targetNewPrice.doubleValue - totalListPriceFromJsonArray) > 0.001) {
      throw new Exception(
        s"[] failed equality assertion with ratePlanCharges: ${ratePlanCharges}, priceRatio: ${priceRatio}, targetNewPrice: ${targetNewPrice}, jsonData: ${jsonData.toString()}"
      )
    }

    jsonData
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
