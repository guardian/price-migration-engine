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

}
