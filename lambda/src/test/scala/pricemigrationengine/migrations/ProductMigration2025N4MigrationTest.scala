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
  }

}
