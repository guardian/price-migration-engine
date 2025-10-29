package pricemigrationengine.migrations

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.Fixtures

import java.time.LocalDate

class ProductMigration2025N4MigrationTest extends munit.FunSuite {
  test("amendment payload for 01") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/01/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/ProductMigration2025N4/01/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/ProductMigration2025N4/01/invoice-preview.json")

    assertEquals(
      ProductMigration2025N4Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(20.99), BigDecimal(20.99), "Month")) // [1]
    )

    // [1] Product migration the old and new prices are the same

    val payload = ProductMigration2025N4Migration.amendmentOrderPayload(
      LocalDate.of(2025, 10, 28),
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2025, 11, 17),
      subscription,
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-10-28",
             |    "existingAccountNumber": "accountNumber",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "subscriptionNumber",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a128b2899f14131019a0e62f3c51d63"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0ff6205708e01622484bb2c4613",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff6205708e01622484bb68461d",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 11.52
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff6205708e01622484bb404615",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.47
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
      )
    )

    // 11.52 + 9.47
    // => 20.99 ✅
  }

  test("amendment payload for 02") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/02/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/ProductMigration2025N4/02/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/ProductMigration2025N4/02/invoice-preview.json")

    assertEquals(
      ProductMigration2025N4Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(34.99), BigDecimal(34.99), "Month")) // [1]
    )

    // [1] Product migration the old and new prices are the same

    val payload = ProductMigration2025N4Migration.amendmentOrderPayload(
      LocalDate.of(2025, 10, 28),
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2025, 11, 17),
      subscription,
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-10-28",
             |    "existingAccountNumber": "accountNumber",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "subscriptionNumber",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a129bd5993a10c301994a6e60e450ad"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0ff560d311b0156136b9f5c3968",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff560d311b0156136ba0523996",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 12.4
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff560d311b0156136ba11539ae",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 12.4
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff560d311b0156136b9fac3976",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.19
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
      )
    )

    // 12.4 + 12.4 + 10.19
    // => 34.99 ✅
  }

  test("amendment payload for 03") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/03/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/ProductMigration2025N4/03/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/ProductMigration2025N4/03/invoice-preview.json")

    assertEquals(
      ProductMigration2025N4Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(83.99), BigDecimal(83.99), "Month")) // [1]
    )

    // [1] Product migration the old and new prices are the same

    val payload = ProductMigration2025N4Migration.amendmentOrderPayload(
      LocalDate.of(2025, 10, 28),
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2025, 11, 17),
      subscription,
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-10-28",
             |    "existingAccountNumber": "accountNumber",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "subscriptionNumber",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a129bd5993a10c301994a463092564b"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0fd560d132301560e43cf041a3c",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fc560d13390156136324931d21",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.65
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd560d138801561364cad96af7",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.65
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fe560d3104015613640f555223",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.65
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fe560d31040156136626dd5d1b",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 12.67
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff560d31190156134be59060f4",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.65
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff560d311b015613623e050a63",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.65
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff560d311c0156136573e366f3",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 12.67
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd560d132901561367b2f17763",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.4
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
      )
    )

    // 9.65 + 9.65 + 9.65 + 12.67 + 9.65 + 9.65 + 12.67 + 10.4
    // => 83.9 ✅
  }

  test("amendment payload for 04") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/04/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/ProductMigration2025N4/04/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/ProductMigration2025N4/04/invoice-preview.json")

    assertEquals(
      ProductMigration2025N4Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(73.99), BigDecimal(73.99), "Month")) // [1]
    )

    // [1] Product migration the old and new prices are the same

    val payload = ProductMigration2025N4Migration.amendmentOrderPayload(
      LocalDate.of(2025, 10, 28),
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2025, 11, 17),
      subscription,
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-10-28",
             |    "existingAccountNumber": "accountNumber",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "subscriptionNumber",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a1285019949177b0199492fcc491f77"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0ff560d311b0156136b697438a9",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff560d311b0156136b698f38ac",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.01
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff560d311b0156136b6a0838bc",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.01
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff560d311b0156136b6a4138c5",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.01
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff560d311b0156136b6ac738d5",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.01
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff560d311b0156136b6b0438dd",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.01
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff560d311b0156136b6b4b38e6",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 13.14
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff560d311b0156136b69d038b4",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.8
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
      )
    )

    // 10.01 + 10.01 + 10.01 + 10.01 + 10.01 + 13.14 + 10.8
    // => 73.99 ✅
  }

  test("amendment payload for 05") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/05/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/ProductMigration2025N4/05/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/ProductMigration2025N4/05/invoice-preview.json")

    assertEquals(
      ProductMigration2025N4Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(34.99), BigDecimal(34.99), "Month")) // [1]
    )

    // [1] Product migration the old and new prices are the same

    val payload = ProductMigration2025N4Migration.amendmentOrderPayload(
      LocalDate.of(2025, 10, 28),
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2025, 11, 17),
      subscription,
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-10-28",
             |    "existingAccountNumber": "accountNumber",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "subscriptionNumber",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a128536993307dd01994ab88d7766aa"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "8a1280be96d33dbf0196d487b55c1283",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "8a12817596d33daf0196d48a3eec13ce",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.19
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "8a1280be96d33dbf0196d487b5ae1285",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 12.4
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "8a1280be96d33dbf0196d487b5f8128d",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 12.4
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
      )
    )

    // 10.19 + 12.4 + 12.4
    // => 34.99 ✅
  }

  test("amendment payload for 06") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/06/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/ProductMigration2025N4/06/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/ProductMigration2025N4/06/invoice-preview.json")

    assertEquals(
      ProductMigration2025N4Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(73.99), BigDecimal(73.99), "Month")) // [1]
    )

    // [1] Product migration the old and new prices are the same

    val payload = ProductMigration2025N4Migration.amendmentOrderPayload(
      LocalDate.of(2025, 10, 28),
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2025, 11, 17),
      subscription,
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-10-28",
             |    "existingAccountNumber": "accountNumber",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "subscriptionNumber",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a1288c5995c4219019992c593e22d0d"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "8a12994696d3587b0196d484491e3beb",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "8a12979796d358720196d4878ee0421f",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.8
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "8a12994696d3587b0196d4844b5f3c15",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 13.14
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "8a12994696d3587b0196d48449893bed",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.01
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "8a12994696d3587b0196d48449f33bf5",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.01
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "8a12994696d3587b0196d4844a473bfd",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.01
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "8a12994696d3587b0196d4844aa13c05",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.01
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "8a12994696d3587b0196d4844aff3c0d",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.01
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
      )
    )

    // 10.8 + 13.14 + 10.01 + 10.01 + 10.01 + 10.01 + 10.01
    // => 73.99 ✅
  }

  test("amendment payload for 07") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/07/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/ProductMigration2025N4/07/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/ProductMigration2025N4/07/invoice-preview.json")

    assertEquals(
      ProductMigration2025N4Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(83.99), BigDecimal(83.99), "Month")) // [1]
    )

    // [1] Product migration the old and new prices are the same

    val payload = ProductMigration2025N4Migration.amendmentOrderPayload(
      LocalDate.of(2025, 10, 28),
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2025, 11, 17),
      subscription,
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-10-28",
             |    "existingAccountNumber": "accountNumber",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "subscriptionNumber",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a1298ad9744f22201975e38c49c51a5"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "8a1280be96d33dbf0196d48a632616f4",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "8a12904196d3586d0196d48bff216382",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.4
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "8a1280be96d33dbf0196d48a634b16f6",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 12.67
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "8a1280be96d33dbf0196d48a638d16fe",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.65
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "8a1280be96d33dbf0196d48a63cf1706",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.65
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "8a1280be96d33dbf0196d48a6414170f",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.65
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "8a1280be96d33dbf0196d48a645c1718",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.65
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "8a1280be96d33dbf0196d48a64a21720",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.65
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "8a1280be96d33dbf0196d48a64f21728",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 12.67
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
      )
    )

    // 10.4 + 12.67 + 9.65 + 9.65 + 9.65 + 9.65 + 9.65 + 12.67
    // => 83.99 ✅
  }

  test("amendment payload for 08") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/08/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/ProductMigration2025N4/08/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/ProductMigration2025N4/08/invoice-preview.json")

    assertEquals(
      ProductMigration2025N4Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(27.99), BigDecimal(27.99), "Month")) // [1]
    )

    // [1] Product migration the old and new prices are the same

    val payload = ProductMigration2025N4Migration.amendmentOrderPayload(
      LocalDate.of(2025, 10, 28),
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2025, 11, 17),
      subscription,
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-10-28",
             |    "existingAccountNumber": "accountNumber",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "subscriptionNumber",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a129bd5993a10c3019949d7826c74cd"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0fd56fe26b60157040cdd323f76",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe26b601570432f4e33d17",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.92
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f5015709b8fc4d5617",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.92
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fe56fe33ff015709bb986636d8",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.15
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
      )
    )

    // 9.92 + 9.92 + 8.15
    // => 27.99 ✅
  }

  test("amendment payload for 09") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/09/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/ProductMigration2025N4/09/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/ProductMigration2025N4/09/invoice-preview.json")

    assertEquals(
      ProductMigration2025N4Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(15.99), BigDecimal(15.99), "Month")) // [1]
    )

    // [1] Product migration the old and new prices are the same

    val payload = ProductMigration2025N4Migration.amendmentOrderPayload(
      LocalDate.of(2025, 10, 28),
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2025, 11, 17),
      subscription,
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-10-28",
             |    "existingAccountNumber": "accountNumber",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "subscriptionNumber",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a1287e398c18dad019900736da57b15"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0fd6205707201621fa1350710e3",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd6205707201621fa1354710ed",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.78
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd6205707201621fa1351710e5",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 7.21
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
      )
    )

    // 8.78 + 7.21
    // => 15.99 ✅
  }

  test("amendment payload for 10") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/10/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/ProductMigration2025N4/10/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/ProductMigration2025N4/10/invoice-preview.json")

    assertEquals(
      ProductMigration2025N4Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(61.99), BigDecimal(61.99), "Month")) // [1]
    )

    // [1] Product migration the old and new prices are the same

    val payload = ProductMigration2025N4Migration.amendmentOrderPayload(
      LocalDate.of(2025, 10, 28),
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2025, 11, 17),
      subscription,
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-10-28",
             |    "existingAccountNumber": "accountNumber",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "subscriptionNumber",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a129bd5993a10c301994afeaa324d05"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0fc56fe26ba0157040c5ea17f6a",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fc56fe26ba015709cf4bbd3d1c",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.39
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe26b6015709ced61a032e",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.39
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe26b6015709cfc1500a2e",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.39
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe26b6015709d078df4a80",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 11.01
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fe56fe33ff015704325d87494c",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.39
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f5015709cdedbd246b",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.39
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f3015709d10a436f52",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.03
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
      )
    )

    // 8.39 + 8.39 + 8.39 + 11.01 + 8.39 + 8.39 + 9.03
    // => 61.99 ✅
  }

  test("amendment payload for 11") {
    // 11 Newspaper Voucher / Everyday / Quarterly
    // 16 Newspaper Voucher / Everyday / Annual

    val subscription = Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/11/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/ProductMigration2025N4/11/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/ProductMigration2025N4/11/invoice-preview.json")

    assertEquals(
      ProductMigration2025N4Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(209.97), BigDecimal(209.97), "Quarter")) // [1]
    )

    // [1] Product migration the old and new prices are the same

    val payload = ProductMigration2025N4Migration.amendmentOrderPayload(
      LocalDate.of(2025, 10, 28),
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2025, 11, 17),
      subscription,
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-10-28",
             |    "existingAccountNumber": "accountNumber",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "subscriptionNumber",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a129636994b99c901994ba18c3d1508"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0ff56fe33f50157040bbdcf3ae4",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe26b60157042fcd462666",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 31.68
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Quarter"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe26b6015709ca144a646a",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 24.12
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Quarter"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe270b015709c90c291c49",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 24.12
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Quarter"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe270b015709cc16f92645",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 24.12
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Quarter"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f0015709cac4561bf3",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 24.12
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Quarter"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f5015709c80af30495",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 31.68
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Quarter"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f5015709cce7ad1aea",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 24.12
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Quarter"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fc56fe26ba01570418eddd26e1",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 26.01
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Quarter"
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
      )
    )

    // 31.68 + 24.12 + 24.12 + 24.12 + 24.12 + 31.68 + 24.12 + 26.01
    // => 209.97 ✅
  }

  test("amendment payload for 12") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/12/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/ProductMigration2025N4/12/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/ProductMigration2025N4/12/invoice-preview.json")

    assertEquals(
      ProductMigration2025N4Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(15.99), BigDecimal(15.99), "Month")) // [1]
    )

    // [1] Product migration the old and new prices are the same

    val payload = ProductMigration2025N4Migration.amendmentOrderPayload(
      LocalDate.of(2025, 10, 28),
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2025, 11, 17),
      subscription,
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-10-28",
             |    "existingAccountNumber": "accountNumber",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "subscriptionNumber",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a12992098448d770198624c80461b84"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a00870ec598001710740ce702ff0",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740cf1e2ffc",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.78
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740cea02ff4",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 7.21
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
      )
    )

    // 8.78 + 7.21
    // => 15.98 ✅
  }

  test("amendment payload for 13") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/13/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/ProductMigration2025N4/13/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/ProductMigration2025N4/13/invoice-preview.json")

    assertEquals(
      ProductMigration2025N4Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(27.99), BigDecimal(27.99), "Month")) // [1]
    )

    // [1] Product migration the old and new prices are the same

    val payload = ProductMigration2025N4Migration.amendmentOrderPayload(
      LocalDate.of(2025, 10, 28),
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2025, 11, 17),
      subscription,
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-10-28",
             |    "existingAccountNumber": "accountNumber",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "subscriptionNumber",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a129bd5993a10c301994ac801ae2bf6"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a00870ec598001710740c6672ee7",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740c6872ee9",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.92
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740c7132efe",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.92
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740c6ce2ef1",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.15
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
      )
    )

    // 9.92 + 9.92 + 8.15
    // => 27.99 ✅
  }

  test("amendment payload for 14") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/14/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/ProductMigration2025N4/14/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/ProductMigration2025N4/14/invoice-preview.json")

    assertEquals(
      ProductMigration2025N4Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(61.99), BigDecimal(61.99), "Month")) // [1]
    )

    // [1] Product migration the old and new prices are the same

    val payload = ProductMigration2025N4Migration.amendmentOrderPayload(
      LocalDate.of(2025, 10, 28),
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2025, 11, 17),
      subscription,
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-10-28",
             |    "existingAccountNumber": "accountNumber",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "subscriptionNumber",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a129bd5993a10c301994a74cb417deb"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a00870ec598001710740c4582ead",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740c48e2eaf",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.39
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740c4dc2eb7",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.39
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740c5192ebf",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.39
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740c55a2ec7",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 11.01
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740c5962ecf",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.39
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740c60f2edf",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.39
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740c5cf2ed7",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.03
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
      )
    )

    // 8.39 + 8.39 + 8.39 + 11.01 + 8.39 + 8.39 + 9.03
    // => 61.99 ✅
  }

  test("amendment payload for 15") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/15/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/ProductMigration2025N4/15/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/ProductMigration2025N4/15/invoice-preview.json")

    assertEquals(
      ProductMigration2025N4Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(69.99), BigDecimal(69.99), "Month")) // [1]
    )

    // [1] Product migration the old and new prices are the same

    val payload = ProductMigration2025N4Migration.amendmentOrderPayload(
      LocalDate.of(2025, 10, 28),
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2025, 11, 17),
      subscription,
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-10-28",
             |    "existingAccountNumber": "accountNumber",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "subscriptionNumber",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a128536995c57d501995c77691e18ac"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a00870ec598001710740d3d03035",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740d4b8304f",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.56
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740d54f3069",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.04
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740d5fd3073",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.04
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740d691307c",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.04
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740d7493084",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.04
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740d7e2308d",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.56
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740d8873096",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.04
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a00870ec598001710740d4143037",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.67
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
      )
    )

    // 10.56 + 8.04 + 8.04 + 8.04 + 8.04 + 10.56 + 8.04 + 8.67
    // => 69.99 ✅
  }

  test("amendment payload for 16") {
    // 11 Newspaper Voucher / Everyday / Quarterly
    // 16 Newspaper Voucher / Everyday / Annual

    val subscription = Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/16/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/ProductMigration2025N4/16/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/ProductMigration2025N4/16/invoice-preview.json")

    assertEquals(
      ProductMigration2025N4Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(839.88), BigDecimal(839.88), "Annual")) // [1]
    )

    // [1] Product migration the old and new prices are the same

    val payload = ProductMigration2025N4Migration.amendmentOrderPayload(
      LocalDate.of(2025, 10, 28),
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2025, 11, 17),
      subscription,
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-10-28",
             |    "existingAccountNumber": "accountNumber",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "subscriptionNumber",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a12992c998d8ae201998df35bbb65f5"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-17"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0ff56fe33f50157040bbdcf3ae4",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe26b60157042fcd462666",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 126.72
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Annual"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe26b6015709ca144a646a",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 96.48
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Annual"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe270b015709c90c291c49",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 96.48
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Annual"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe270b015709cc16f92645",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 96.48
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Annual"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f0015709cac4561bf3",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 96.48
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Annual"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f5015709c80af30495",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 126.72
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Annual"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f5015709cce7ad1aea",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 96.48
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Annual"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fc56fe26ba01570418eddd26e1",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 104.04
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Annual"
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
      )
    )

    // 126.72 + 96.48 + 96.48 + 96.48 + 96.48 + 126.72 + 96.48 + 104.04
    // => 839.88 ✅
  }

  test("basic scala") {
    val list1 = List("1", "a", "c")
    val list2 = List("1", "a", "d")
    assertEquals(list1.diff(list2), List("c"))
  }

  test("postAmendmentIntegrityCheck (02-after-correct)") {
    val subscriptionBefore =
      Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/Tegridy-Farms/01-before/subscription.json")
    val subscriptionAfter = Fixtures.subscriptionFromJson(
      "Migrations/ProductMigration2025N4/Tegridy-Farms/02-after-correct/subscription.json"
    )

    // 02-after-correct was built to pass the integrity check

    assertEquals(
      ProductMigration2025N4Migration.postAmendmentStructureIntegrityCheck(
        subscriptionBefore,
        subscriptionAfter
      ),
      Right(())
    )
  }

  test("postAmendmentIntegrityCheck (03-after-incorrect-number-of-charges)") {
    val subscriptionBefore =
      Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/Tegridy-Farms/01-before/subscription.json")
    val subscriptionAfter = Fixtures.subscriptionFromJson(
      "Migrations/ProductMigration2025N4/Tegridy-Farms/03-after-incorrect-number-of-charges/subscription.json"
    )

    // 03-after-incorrect-number-of-charges was built to fail the integrity check
    // on the number of charges

    val result = ProductMigration2025N4Migration
      .postAmendmentStructureIntegrityCheck(
        subscriptionBefore,
        subscriptionAfter
      )

    assertEquals(
      result match {
        case Left(message) => message.contains("68eb28cc")
        case _             => false
      },
      true
    )
  }

  test("postAmendmentIntegrityCheck (04-after-incorrect-extra-name)") {
    val subscriptionBefore =
      Fixtures.subscriptionFromJson("Migrations/ProductMigration2025N4/Tegridy-Farms/01-before/subscription.json")
    val subscriptionAfter = Fixtures.subscriptionFromJson(
      "Migrations/ProductMigration2025N4/Tegridy-Farms/04-after-incorrect-extra-name/subscription.json"
    )

    // 04-after-incorrect-extra-name was built to fail the integrity check
    // on the name of the extra check

    val result = ProductMigration2025N4Migration
      .postAmendmentStructureIntegrityCheck(
        subscriptionBefore,
        subscriptionAfter
      )

    assertEquals(
      result match {
        case Left(message) => message.contains("d2bccdbd")
        case _             => false
      },
      true
    )
  }
}
