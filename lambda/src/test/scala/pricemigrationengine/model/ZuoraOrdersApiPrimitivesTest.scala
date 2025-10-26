package pricemigrationengine.model

import java.time.LocalDate

class ZuoraOrdersAPIPrimitivesTest extends munit.FunSuite {

  test("ZuoraOrdersApiPrimitives.singletonDate") {
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

  test("ZuoraOrdersApiPrimitives.triggerDates") {
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

  test("ZuoraOrdersApiPrimitives.removeProduct") {
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

  test("ZuoraOrdersApiPrimitives.chargeOverride") {
    val chargeOverride = ZuoraOrdersApiPrimitives.chargeOverride("8a128ed885fc6ded018602296af13eba", 12, "Quarter")
    val jsonstrpp = ujson.write(chargeOverride, indent = 4)
    assertEquals(
      jsonstrpp,
      """{
        |    "productRatePlanChargeId": "8a128ed885fc6ded018602296af13eba",
        |    "pricing": {
        |        "recurringFlatFee": {
        |            "listPrice": 12
        |        }
        |    },
        |    "billing": {
        |        "billingPeriod": "Quarter"
        |    }
        |}""".stripMargin
    )
  }

  test("ZuoraOrdersApiPrimitives.addProduct") {
    val chargeOverrides = List(
      ZuoraOrdersApiPrimitives.chargeOverride("8a128ed885fc6ded018602296af13eba", 12, "Month"),
      ZuoraOrdersApiPrimitives.chargeOverride("8a128d7085fc6dec01860234cd075270", 0, "Month")
    )
    val addProduct = ZuoraOrdersApiPrimitives.addProduct(
      "2024-11-28",
      "8a128ed885fc6ded018602296ace3eb8",
      chargeOverrides
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
        |                },
        |                "billing": {
        |                    "billingPeriod": "Month"
        |                }
        |            },
        |            {
        |                "productRatePlanChargeId": "8a128d7085fc6dec01860234cd075270",
        |                "pricing": {
        |                    "recurringFlatFee": {
        |                        "listPrice": 0
        |                    }
        |                },
        |                "billing": {
        |                    "billingPeriod": "Month"
        |                }
        |            }
        |        ]
        |    }
        |}""".stripMargin
    )
  }

  test("ZuoraOrdersApiPrimitives.subscription") {
    val removeProduct = ZuoraOrdersApiPrimitives.removeProduct("2025-05-19", "8a12867e92c341870192c7c46bdb47d6")
    val addProduct = ZuoraOrdersApiPrimitives.addProduct(
      "2025-05-20",
      "8a128ed885fc6ded018602296ace3eb8",
      List(
        ZuoraOrdersApiPrimitives.chargeOverride("8a128ed885fc6ded018602296af13eba", 12, "Annual"),
        ZuoraOrdersApiPrimitives.chargeOverride("8a128d7085fc6dec01860234cd075270", 0, "Annual")
      )
    )
    val json = ZuoraOrdersApiPrimitives.subscription("a1809f5e84dd", List(removeProduct), List(addProduct))
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
        |                        },
        |                        "billing": {
        |                            "billingPeriod": "Annual"
        |                        }
        |                    },
        |                    {
        |                        "productRatePlanChargeId": "8a128d7085fc6dec01860234cd075270",
        |                        "pricing": {
        |                            "recurringFlatFee": {
        |                                "listPrice": 0
        |                            }
        |                        },
        |                        "billing": {
        |                            "billingPeriod": "Annual"
        |                        }
        |                    }
        |                ]
        |            }
        |        }
        |    ]
        |}""".stripMargin
    )
  }

  test("ZuoraOrdersApiPrimitives.replace_a_product_in_a_subscription") {
    val removeProduct = ZuoraOrdersApiPrimitives.removeProduct("2025-05-19", "8a12867e92c341870192c7c46bdb47d6")
    val addProduct = ZuoraOrdersApiPrimitives.addProduct(
      "2025-05-20",
      "8a128ed885fc6ded018602296ace3eb8",
      List(
        ZuoraOrdersApiPrimitives.chargeOverride("8a128ed885fc6ded018602296af13eba", 12, "Month"),
        ZuoraOrdersApiPrimitives.chargeOverride("8a128d7085fc6dec01860234cd075270", 0, "Month")
      )
    )
    val subscription = ZuoraOrdersApiPrimitives.subscription("a1809f5e84dd", List(removeProduct), List(addProduct))

    val json = ZuoraOrdersApiPrimitives.subscriptionUpdatePayload(
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
        |                                },
        |                                "billing": {
        |                                    "billingPeriod": "Month"
        |                                }
        |                            },
        |                            {
        |                                "productRatePlanChargeId": "8a128d7085fc6dec01860234cd075270",
        |                                "pricing": {
        |                                    "recurringFlatFee": {
        |                                        "listPrice": 0
        |                                    }
        |                                },
        |                                "billing": {
        |                                    "billingPeriod": "Month"
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

  test("ZuoraOrdersApiPrimitives.ratePlanChargesToChargeOverrides") {

    // The following rate plan was taken from the Newspaper2025P1 test suite.
    // We stole it from there to test ratePlanChargesToChargeOverrides
    val ratePlan = ZuoraRatePlan(
      id = "8a12910195aa4f620195b0e2bc0c139d",
      productName = "Newspaper Voucher",
      productRatePlanId = "2c92a0fc56fe26ba0157040c5ea17f6a",
      ratePlanName = "Sixday+",
      ratePlanCharges = List(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0ff56fe33f5015709cdedbd246b",
          name = "Tuesday",
          number = "C-05403277",
          currency = "GBP",
          price = Some(BigDecimal(8.96)),
          billingPeriod = Some("Month"),
          chargedThroughDate = Some(LocalDate.of(2025, 7, 8)),
          processedThroughDate = Some(LocalDate.of(2025, 6, 8)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 3, 29)),
          effectiveStartDate = Some(LocalDate.of(2024, 5, 8)),
          effectiveEndDate = Some(LocalDate.of(2025, 9, 8))
        ),
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0ff56fe33f3015709d10a436f52",
          name = "Digipack",
          number = "C-05403276",
          currency = "GBP",
          price = Some(BigDecimal(2.0)),
          billingPeriod = Some("Month"),
          chargedThroughDate = Some(LocalDate.of(2025, 7, 8)),
          processedThroughDate = Some(LocalDate.of(2025, 6, 8)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 3, 29)),
          effectiveStartDate = Some(LocalDate.of(2024, 5, 8)),
          effectiveEndDate = Some(LocalDate.of(2025, 9, 8))
        ),
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fe56fe33ff015704325d87494c",
          name = "Monday",
          number = "C-05403275",
          currency = "GBP",
          price = Some(BigDecimal(8.96)),
          billingPeriod = Some("Month"),
          chargedThroughDate = Some(LocalDate.of(2025, 7, 8)),
          processedThroughDate = Some(LocalDate.of(2025, 6, 8)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 3, 29)),
          effectiveStartDate = Some(LocalDate.of(2024, 5, 8)),
          effectiveEndDate = Some(LocalDate.of(2025, 9, 8))
        ),
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fd56fe26b6015709d078df4a80",
          name = "Saturday",
          number = "C-05403274",
          currency = "GBP",
          price = Some(BigDecimal(12.19)),
          billingPeriod = Some("Month"),
          chargedThroughDate = Some(LocalDate.of(2025, 7, 8)),
          processedThroughDate = Some(LocalDate.of(2025, 6, 8)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 3, 29)),
          effectiveStartDate = Some(LocalDate.of(2024, 5, 8)),
          effectiveEndDate = Some(LocalDate.of(2025, 9, 8))
        ),
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fd56fe26b6015709cfc1500a2e",
          name = "Friday",
          number = "C-05403273",
          currency = "GBP",
          price = Some(BigDecimal(8.96)),
          billingPeriod = Some("Month"),
          chargedThroughDate = Some(LocalDate.of(2025, 7, 8)),
          processedThroughDate = Some(LocalDate.of(2025, 6, 8)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 3, 29)),
          effectiveStartDate = Some(LocalDate.of(2024, 5, 8)),
          effectiveEndDate = Some(LocalDate.of(2025, 9, 8))
        ),
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fd56fe26b6015709ced61a032e",
          name = "Wednesday",
          number = "C-05403272",
          currency = "GBP",
          price = Some(BigDecimal(8.96)),
          billingPeriod = Some("Month"),
          chargedThroughDate = Some(LocalDate.of(2025, 7, 8)),
          processedThroughDate = Some(LocalDate.of(2025, 6, 8)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 3, 29)),
          effectiveStartDate = Some(LocalDate.of(2024, 5, 8)),
          effectiveEndDate = Some(LocalDate.of(2025, 9, 8))
        ),
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fc56fe26ba015709cf4bbd3d1c",
          name = "Thursday",
          number = "C-05403271",
          currency = "GBP",
          price = Some(BigDecimal(8.96)),
          billingPeriod = Some("Month"),
          chargedThroughDate = Some(LocalDate.of(2025, 7, 8)),
          processedThroughDate = Some(LocalDate.of(2025, 6, 8)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 3, 29)),
          effectiveStartDate = Some(LocalDate.of(2024, 5, 8)),
          effectiveEndDate = Some(LocalDate.of(2025, 9, 8))
        )
      ),
      lastChangeType = Some("Add")
    )

    val json = ZuoraOrdersApiPrimitives.ratePlanChargesToChargeOverrides(
      ratePlan.ratePlanCharges,
      BigDecimal(1.5), // price increase ratio
      BigDecimal(88.47), // target price (aka: estimated new price)
      "Month"
    )

    val jsonstrpp = ujson.write(json, indent = 4)

    assertEquals(
      jsonstrpp,
      """[
        |    {
        |        "productRatePlanChargeId": "2c92a0ff56fe33f5015709cdedbd246b",
        |        "pricing": {
        |            "recurringFlatFee": {
        |                "listPrice": 13.43
        |            }
        |        },
        |        "billing": {
        |            "billingPeriod": "Month"
        |        }
        |    },
        |    {
        |        "productRatePlanChargeId": "2c92a0ff56fe33f3015709d10a436f52",
        |        "pricing": {
        |            "recurringFlatFee": {
        |                "listPrice": 3
        |            }
        |        },
        |        "billing": {
        |            "billingPeriod": "Month"
        |        }
        |    },
        |    {
        |        "productRatePlanChargeId": "2c92a0fe56fe33ff015704325d87494c",
        |        "pricing": {
        |            "recurringFlatFee": {
        |                "listPrice": 13.44
        |            }
        |        },
        |        "billing": {
        |            "billingPeriod": "Month"
        |        }
        |    },
        |    {
        |        "productRatePlanChargeId": "2c92a0fd56fe26b6015709d078df4a80",
        |        "pricing": {
        |            "recurringFlatFee": {
        |                "listPrice": 18.28
        |            }
        |        },
        |        "billing": {
        |            "billingPeriod": "Month"
        |        }
        |    },
        |    {
        |        "productRatePlanChargeId": "2c92a0fd56fe26b6015709cfc1500a2e",
        |        "pricing": {
        |            "recurringFlatFee": {
        |                "listPrice": 13.44
        |            }
        |        },
        |        "billing": {
        |            "billingPeriod": "Month"
        |        }
        |    },
        |    {
        |        "productRatePlanChargeId": "2c92a0fd56fe26b6015709ced61a032e",
        |        "pricing": {
        |            "recurringFlatFee": {
        |                "listPrice": 13.44
        |            }
        |        },
        |        "billing": {
        |            "billingPeriod": "Month"
        |        }
        |    },
        |    {
        |        "productRatePlanChargeId": "2c92a0fc56fe26ba015709cf4bbd3d1c",
        |        "pricing": {
        |            "recurringFlatFee": {
        |                "listPrice": 13.44
        |            }
        |        },
        |        "billing": {
        |            "billingPeriod": "Month"
        |        }
        |    }
        |]""".stripMargin
    )

    // We can then perform a manual check that the sum of prices is indeed equal
    // to the estimated new price:
    // 88.47 = 13.43 + 3 + 13.44 + 18.28 + 13.44 + 13.44 + 13.44
  }

  test("ZuoraOrdersApiPrimitives.subscriptionRenewalPayload") {
    val payload = ZuoraOrdersApiPrimitives.subscriptionRenewalPayload(
      "2025-07-28",
      "A-NUMBER",
      "S-NUMBER",
      "2026-01-08"
    )
    val jsonstrpp = ujson.write(payload, indent = 4)
    assertEquals(
      jsonstrpp,
      """{
        |    "orderDate": "2025-07-28",
        |    "existingAccountNumber": "A-NUMBER",
        |    "subscriptions": [
        |        {
        |            "subscriptionNumber": "S-NUMBER",
        |            "orderActions": [
        |                {
        |                    "type": "RenewSubscription",
        |                    "triggerDates": [
        |                        {
        |                            "name": "ContractEffective",
        |                            "triggerDate": "2026-01-08"
        |                        },
        |                        {
        |                            "name": "ServiceActivation",
        |                            "triggerDate": "2026-01-08"
        |                        },
        |                        {
        |                            "name": "CustomerAcceptance",
        |                            "triggerDate": "2026-01-08"
        |                        }
        |                    ]
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
}
