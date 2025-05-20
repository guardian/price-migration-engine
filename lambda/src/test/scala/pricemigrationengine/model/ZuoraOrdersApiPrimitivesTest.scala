package pricemigrationengine.migrations

import pricemigrationengine.model._

import java.time.LocalDate
import pricemigrationengine.model.ZuoraOrdersApiPrimitives
import ujson.Value

class ZuoraOrdersAPIPrimitivesTest extends munit.FunSuite {

  test("singletonDate") {
    val singletonDate = ZuoraOrdersApiPrimitives.singletonDate("ContractEffective", "2024-11-28")
    val jsonstr = singletonDate.render()
    assertEquals(jsonstr, """{"name":"ContractEffective","triggerDate":"2024-11-28"}""")

    // We also demonstrate pretty printing. To that end, call ujson.write() with indent
    val jsonstrpp = ujson.write(singletonDate, indent = 4)
    assertEquals(
      jsonstrpp,
      """{
      |    "name": "ContractEffective",
      |    "triggerDate": "2024-11-28"
      |}""".stripMargin
    )
  }

  test("triggerDates") {
    val triggerDates = ZuoraOrdersApiPrimitives.triggerDates("2024-11-28")
    val jsonstrpp = ujson.write(triggerDates, indent = 4)
    assertEquals(
      jsonstrpp,
      """[
      |    {
      |        "name": "ContractEffective",
      |        "triggerDate": "2024-11-28"
      |    },
      |    {
      |        "name": "ServiceActivation",
      |        "triggerDate": "2024-11-28"
      |    },
      |    {
      |        "name": "CustomerAcceptance",
      |        "triggerDate": "2024-11-28"
      |    }
      |]""".stripMargin
    )
  }

  test("removeProduct") {
    val removeProduct = ZuoraOrdersApiPrimitives.removeProduct("2024-11-28", "8a12867e92c341870192c7c46bdb47d6")
    val jsonstrpp = ujson.write(removeProduct, indent = 4)
    assertEquals(
      jsonstrpp,
      """{
        |    "type": "RemoveProduct",
        |    "triggerDates": [
        |        {
        |            "name": "ContractEffective",
        |            "triggerDate": "2024-11-28"
        |        },
        |        {
        |            "name": "ServiceActivation",
        |            "triggerDate": "2024-11-28"
        |        },
        |        {
        |            "name": "CustomerAcceptance",
        |            "triggerDate": "2024-11-28"
        |        }
        |    ],
        |    "removeProduct": {
        |        "ratePlanId": "8a12867e92c341870192c7c46bdb47d6"
        |    }
        |}""".stripMargin
    )
  }

  test("chargeOverride") {
    val chargeOverride = ZuoraOrdersApiPrimitives.chargeOverride("8a128ed885fc6ded018602296af13eba", 12)
    val jsonstrpp = ujson.write(chargeOverride, indent = 4)
    assertEquals(
      jsonstrpp,
      """{
        |    "productRatePlanChargeId": "8a128ed885fc6ded018602296af13eba",
        |    "pricing": {
        |        "recurringFlatFee": {
        |            "listPrice": 12
        |        }
        |    }
        |}""".stripMargin
    )
  }

  test("addProduct") {
    val chargeOverrides = List(
      ZuoraOrdersApiPrimitives.chargeOverride("8a128ed885fc6ded018602296af13eba", 12),
      ZuoraOrdersApiPrimitives.chargeOverride("8a128d7085fc6dec01860234cd075270", 0)
    )
    val addProduct = ZuoraOrdersApiPrimitives.addProduct(
      "2024-11-28",
      "8a128ed885fc6ded018602296ace3eb8",
      chargeOverrides: List[Value]
    )
    val jsonstrpp = ujson.write(addProduct, indent = 4)
    assertEquals(
      jsonstrpp,
      """{
        |    "type": "AddProduct",
        |    "triggerDates": [
        |        {
        |            "name": "ContractEffective",
        |            "triggerDate": "2024-11-28"
        |        },
        |        {
        |            "name": "ServiceActivation",
        |            "triggerDate": "2024-11-28"
        |        },
        |        {
        |            "name": "CustomerAcceptance",
        |            "triggerDate": "2024-11-28"
        |        }
        |    ],
        |    "addProduct": {
        |        "productRatePlanId": "8a128ed885fc6ded018602296ace3eb8",
        |        "chargeOverrides": [
        |            {
        |                "productRatePlanChargeId": "8a128ed885fc6ded018602296af13eba",
        |                "pricing": {
        |                    "recurringFlatFee": {
        |                        "listPrice": 12
        |                    }
        |                }
        |            },
        |            {
        |                "productRatePlanChargeId": "8a128d7085fc6dec01860234cd075270",
        |                "pricing": {
        |                    "recurringFlatFee": {
        |                        "listPrice": 0
        |                    }
        |                }
        |            }
        |        ]
        |    }
        |}""".stripMargin
    )
  }

  test("replace_a_product_in_a_subscription") {
    val removeProduct = ZuoraOrdersApiPrimitives.removeProduct("2025-05-19", "8a12867e92c341870192c7c46bdb47d6")
    val addProduct = ZuoraOrdersApiPrimitives.addProduct(
      "2025-05-20",
      "8a128ed885fc6ded018602296ace3eb8",
      List(
        ZuoraOrdersApiPrimitives.chargeOverride("8a128ed885fc6ded018602296af13eba", 12),
        ZuoraOrdersApiPrimitives.chargeOverride("8a128d7085fc6dec01860234cd075270", 0)
      )
    )
    val subscription = ZuoraOrdersApiPrimitives.subscription("a1809f5e84dd", removeProduct: Value, addProduct: Value)

    val json = ZuoraOrdersApiPrimitives.replace_a_product_in_a_subscription(
      "2025-05-20",
      "4f3d6ed065437111b3",
      subscription
    )
    val jsonstrpp = ujson.write(json, indent = 4)

    assertEquals(
      jsonstrpp,
      """{
        |    "orderDate": "2025-05-20",
        |    "existingAccountNumber": "4f3d6ed065437111b3",
        |    "subscriptions": [
        |        {
        |            "subscriptionNumber": "a1809f5e84dd",
        |            "orderActions": [
        |                {
        |                    "type": "RemoveProduct",
        |                    "triggerDates": [
        |                        {
        |                            "name": "ContractEffective",
        |                            "triggerDate": "2025-05-19"
        |                        },
        |                        {
        |                            "name": "ServiceActivation",
        |                            "triggerDate": "2025-05-19"
        |                        },
        |                        {
        |                            "name": "CustomerAcceptance",
        |                            "triggerDate": "2025-05-19"
        |                        }
        |                    ],
        |                    "removeProduct": {
        |                        "ratePlanId": "8a12867e92c341870192c7c46bdb47d6"
        |                    }
        |                },
        |                {
        |                    "type": "AddProduct",
        |                    "triggerDates": [
        |                        {
        |                            "name": "ContractEffective",
        |                            "triggerDate": "2025-05-20"
        |                        },
        |                        {
        |                            "name": "ServiceActivation",
        |                            "triggerDate": "2025-05-20"
        |                        },
        |                        {
        |                            "name": "CustomerAcceptance",
        |                            "triggerDate": "2025-05-20"
        |                        }
        |                    ],
        |                    "addProduct": {
        |                        "productRatePlanId": "8a128ed885fc6ded018602296ace3eb8",
        |                        "chargeOverrides": [
        |                            {
        |                                "productRatePlanChargeId": "8a128ed885fc6ded018602296af13eba",
        |                                "pricing": {
        |                                    "recurringFlatFee": {
        |                                        "listPrice": 12
        |                                    }
        |                                }
        |                            },
        |                            {
        |                                "productRatePlanChargeId": "8a128d7085fc6dec01860234cd075270",
        |                                "pricing": {
        |                                    "recurringFlatFee": {
        |                                        "listPrice": 0
        |                                    }
        |                                }
        |                            }
        |                        ]
        |                    }
        |                }
        |            ]
        |        }
        |    ],
        |    "processingOptions": {
        |        "runBilling": false,
        |        "collectPayment": false
        |    }
        |}""".stripMargin
    )
  }

  test("subscription") {
    val removeProduct = ZuoraOrdersApiPrimitives.removeProduct("2025-05-19", "8a12867e92c341870192c7c46bdb47d6")
    val addProduct = ZuoraOrdersApiPrimitives.addProduct(
      "2025-05-20",
      "8a128ed885fc6ded018602296ace3eb8",
      List(
        ZuoraOrdersApiPrimitives.chargeOverride("8a128ed885fc6ded018602296af13eba", 12),
        ZuoraOrdersApiPrimitives.chargeOverride("8a128d7085fc6dec01860234cd075270", 0)
      )
    )
    val json = ZuoraOrdersApiPrimitives.subscription("a1809f5e84dd", removeProduct: Value, addProduct: Value)
    val jsonstrpp = ujson.write(json, indent = 4)
    assertEquals(
      jsonstrpp,
      """{
        |    "subscriptionNumber": "a1809f5e84dd",
        |    "orderActions": [
        |        {
        |            "type": "RemoveProduct",
        |            "triggerDates": [
        |                {
        |                    "name": "ContractEffective",
        |                    "triggerDate": "2025-05-19"
        |                },
        |                {
        |                    "name": "ServiceActivation",
        |                    "triggerDate": "2025-05-19"
        |                },
        |                {
        |                    "name": "CustomerAcceptance",
        |                    "triggerDate": "2025-05-19"
        |                }
        |            ],
        |            "removeProduct": {
        |                "ratePlanId": "8a12867e92c341870192c7c46bdb47d6"
        |            }
        |        },
        |        {
        |            "type": "AddProduct",
        |            "triggerDates": [
        |                {
        |                    "name": "ContractEffective",
        |                    "triggerDate": "2025-05-20"
        |                },
        |                {
        |                    "name": "ServiceActivation",
        |                    "triggerDate": "2025-05-20"
        |                },
        |                {
        |                    "name": "CustomerAcceptance",
        |                    "triggerDate": "2025-05-20"
        |                }
        |            ],
        |            "addProduct": {
        |                "productRatePlanId": "8a128ed885fc6ded018602296ace3eb8",
        |                "chargeOverrides": [
        |                    {
        |                        "productRatePlanChargeId": "8a128ed885fc6ded018602296af13eba",
        |                        "pricing": {
        |                            "recurringFlatFee": {
        |                                "listPrice": 12
        |                            }
        |                        }
        |                    },
        |                    {
        |                        "productRatePlanChargeId": "8a128d7085fc6dec01860234cd075270",
        |                        "pricing": {
        |                            "recurringFlatFee": {
        |                                "listPrice": 0
        |                            }
        |                        }
        |                    }
        |                ]
        |            }
        |        }
        |    ]
        |}""".stripMargin
    )
  }

}
