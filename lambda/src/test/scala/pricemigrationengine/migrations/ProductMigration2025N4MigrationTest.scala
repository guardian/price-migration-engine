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
  }

}
