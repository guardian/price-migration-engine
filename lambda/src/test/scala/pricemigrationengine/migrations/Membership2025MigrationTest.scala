package pricemigrationengine.migrations

import pricemigrationengine.Fixtures
import pricemigrationengine.model.CohortTableFilter
import pricemigrationengine.model._

import java.time.{Instant, LocalDate}

class Membership2025MigrationTest extends munit.FunSuite {

  test("price data for sub1") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2025/sub1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Membership2025/sub1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2025/sub1/invoice-preview.json")

    assertEquals(
      Membership2025Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(7), BigDecimal(10), "Month"))
    )
  }

  test("price data for sub2") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2025/sub2/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Membership2025/sub2/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2025/sub2/invoice-preview.json")

    // sub2 is a variation of sub1 with a non standard old price to test the price capping

    assertEquals(
      Membership2025Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(2.5), BigDecimal(10), "Month"))
    )
  }

  test("price data for sub3") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2025/sub3/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Membership2025/sub3/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2025/sub3/invoice-preview.json")

    // Non standard old price and annual

    assertEquals(
      Membership2025Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(75), BigDecimal(100), "Annual"))
    )
  }

  test("price data for sub4") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2025/sub4/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Membership2025/sub4/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2025/sub4/invoice-preview.json")

    // Non standard old price (Non Founder Supporter)

    assertEquals(
      Membership2025Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(5), BigDecimal(10), "Month"))
    )
  }

  test("amendment payload for sub1") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2025/sub1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Membership2025/sub1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2025/sub1/invoice-preview.json")

    // PriceData from the above test: PriceData("GBP", BigDecimal(7), BigDecimal(10), "Month")

    val cohortItem: CohortItem = CohortItem(
      subscriptionName = "subscriptionNumber",
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
      currency = Some("GBP"),

      // Pre migration price
      oldPrice = Some(BigDecimal(7)),

      // Price derived from the Estimation step, without capping
      estimatedNewPrice = Some(BigDecimal(10)),

      // Price (with possible capping) used in the communication to the user and sent to Salesforce
      commsPrice = Some(BigDecimal(10)),

      //
      billingPeriod = Some("Month")
    )

    val payload = Membership2025Migration.amendmentOrderPayload(
      cohortItem,
      LocalDate.of(2025, 10, 28),
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2025, 11, 17),
      subscription,
      BigDecimal(10),
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
             |                        "ratePlanId": "8a128182980d17de0198165347296f5a"
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
             |                        "productRatePlanId": "8a1287c586832d250186a2040b1548fe",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "8a12800986832d1d0186a20bf5136471",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10
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
