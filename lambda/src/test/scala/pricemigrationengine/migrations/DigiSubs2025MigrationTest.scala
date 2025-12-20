package pricemigrationengine.migrations

import pricemigrationengine.Fixtures
import pricemigrationengine.model.CohortTableFilter
import pricemigrationengine.model._

import java.time.{Instant, LocalDate}

class DigiSubs2025MigrationTest extends munit.FunSuite {

  // 01 : Digital Pack Monthly   : 15/06/2016 : Monthly
  // val subscription = Fixtures.subscriptionFromJson("Migrations/DigiSubs2025/01/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/DigiSubs2025/01/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/DigiSubs2025/01/invoice-preview.json")

  // 02 : Digital Pack Quarterly : 14/06/2016 : Quarterly
  // val subscription = Fixtures.subscriptionFromJson("Migrations/DigiSubs2025/02/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/DigiSubs2025/02/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/DigiSubs2025/02/invoice-preview.json")

  // 03 : Digital Pack Annual    : 12/09/2025 : Annually
  // val subscription = Fixtures.subscriptionFromJson("Migrations/DigiSubs2025/03/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/DigiSubs2025/03/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/DigiSubs2025/03/invoice-preview.json")

  // 04 : Digital Pack Monthly   : 19/11/2025 : Monthly
  // val subscription = Fixtures.subscriptionFromJson("Migrations/DigiSubs2025/04/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/DigiSubs2025/04/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/DigiSubs2025/04/invoice-preview.json")

  test("price data for 01") {
    // 01 : Digital Pack Monthly   : 15/06/2016 : Monthly
    val subscription = Fixtures.subscriptionFromJson("Migrations/DigiSubs2025/01/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/DigiSubs2025/01/invoice-preview.json")

    assertEquals(
      DigiSubs2025Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(14.99), BigDecimal(18.0), "Month"))
    )
  }

  test("price data for 02") {
    // 02 : Digital Pack Quarterly : 14/06/2016 : Quarterly
    val subscription = Fixtures.subscriptionFromJson("Migrations/DigiSubs2025/02/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/DigiSubs2025/02/invoice-preview.json")

    assertEquals(
      DigiSubs2025Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(44.94), BigDecimal(54.0), "Quarter"))
    )
  }

  test("price data for 03") {
    // 03 : Digital Pack Annual    : 12/09/2025 : Annually
    val subscription = Fixtures.subscriptionFromJson("Migrations/DigiSubs2025/03/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/DigiSubs2025/03/invoice-preview.json")

    assertEquals(
      DigiSubs2025Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(149.0), BigDecimal(180.0), "Annual"))
    )
  }

  test("price data for 04") {
    // 04 : Digital Pack Monthly   : 19/11/2025 : Monthly
    val subscription = Fixtures.subscriptionFromJson("Migrations/DigiSubs2025/04/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/DigiSubs2025/04/invoice-preview.json")

    assertEquals(
      DigiSubs2025Migration.priceData(subscription, invoicePreview),
      Right(PriceData("USD", BigDecimal(28.0), BigDecimal(28.0), "Month")) // [1]
    )

    // [1]
    // The subscription is so recent that its price is already the target price of
    // the migration. This is going to result in a NoPriceIncrease processing state.
  }

  test("EstimationResult for 01") {
    // 01 : Digital Pack Monthly   : 15/06/2016 : Monthly
    val subscription = Fixtures.subscriptionFromJson("Migrations/DigiSubs2025/01/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/DigiSubs2025/01/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/DigiSubs2025/01/invoice-preview.json")

    val amendmentEffectiveDateLowerBound = LocalDate.of(2026, 2, 1)
    val cohortSpec = CohortSpec("DigiSubs2025", "unspecified", LocalDate.of(2025, 11, 25))

    assertEquals(
      EstimationResult.apply(account, subscription, invoicePreview, amendmentEffectiveDateLowerBound, cohortSpec),
      Right(
        EstimationData(
          subscriptionName = subscription.subscriptionNumber,
          amendmentEffectiveDate =
            LocalDate.of(2026, 2, 15), // The first billing period after amendmentEffectiveDateLowerBound
          currency = "GBP",
          oldPrice = BigDecimal(14.99),
          estimatedNewPrice = BigDecimal(18.0),
          commsPrice = BigDecimal(18.0),
          billingPeriod = "Month"
        )
      )
    )
  }

  test("EstimationResult for 02") {
    // 02 : Digital Pack Quarterly : 14/06/2016 : Quarterly
    val subscription = Fixtures.subscriptionFromJson("Migrations/DigiSubs2025/02/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/DigiSubs2025/02/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/DigiSubs2025/02/invoice-preview.json")

    val amendmentEffectiveDateLowerBound = LocalDate.of(2026, 2, 1)
    val cohortSpec = CohortSpec("DigiSubs2025", "unspecified", LocalDate.of(2025, 11, 25))

    assertEquals(
      EstimationResult.apply(account, subscription, invoicePreview, amendmentEffectiveDateLowerBound, cohortSpec),
      Right(
        EstimationData(
          subscriptionName = subscription.subscriptionNumber,
          amendmentEffectiveDate =
            LocalDate.of(2026, 3, 14), // The first Quarterly billing period after amendmentEffectiveDateLowerBound
          currency = "GBP",
          oldPrice = BigDecimal(44.94),
          estimatedNewPrice = BigDecimal(54.0),
          commsPrice = BigDecimal(54.0),
          billingPeriod = "Quarter"
        )
      )
    )
  }

  test("EstimationResult for 03") {
    // 03 : Digital Pack Annual    : 12/09/2025 : Annually
    val subscription = Fixtures.subscriptionFromJson("Migrations/DigiSubs2025/03/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/DigiSubs2025/03/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/DigiSubs2025/03/invoice-preview.json")

    val amendmentEffectiveDateLowerBound = LocalDate.of(2026, 2, 1)
    val cohortSpec = CohortSpec("DigiSubs2025", "unspecified", LocalDate.of(2025, 11, 25))

    assertEquals(
      EstimationResult.apply(account, subscription, invoicePreview, amendmentEffectiveDateLowerBound, cohortSpec),
      Right(
        EstimationData(
          subscriptionName = subscription.subscriptionNumber,
          amendmentEffectiveDate =
            LocalDate.of(2026, 9, 28), // The first Annual billing period after amendmentEffectiveDateLowerBound
          currency = "GBP",
          oldPrice = BigDecimal(149.0),
          estimatedNewPrice = BigDecimal(180.0),
          commsPrice = BigDecimal(180.0),
          billingPeriod = "Annual"
        )
      )
    )
  }

  test("EstimationResult for 04") {
    // 04 : Digital Pack Monthly   : 19/11/2025 : Monthly
    val subscription = Fixtures.subscriptionFromJson("Migrations/DigiSubs2025/04/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/DigiSubs2025/04/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/DigiSubs2025/04/invoice-preview.json")

    val amendmentEffectiveDateLowerBound = LocalDate.of(2026, 2, 1)
    val cohortSpec = CohortSpec("DigiSubs2025", "unspecified", LocalDate.of(2025, 11, 25))

    assertEquals(
      EstimationResult.apply(account, subscription, invoicePreview, amendmentEffectiveDateLowerBound, cohortSpec),
      Right(
        EstimationData(
          subscriptionName = subscription.subscriptionNumber,
          amendmentEffectiveDate =
            LocalDate.of(2026, 2, 5), // The first Monthly billing period after amendmentEffectiveDateLowerBound
          currency = "USD",
          oldPrice = BigDecimal(28.0), // [1]
          estimatedNewPrice = BigDecimal(28.0), // [1]
          commsPrice = BigDecimal(28.0),
          billingPeriod = "Month"
        )
      )
    )

    // [1]
    // The subscription is so recent that its price is already the target price of
    // the migration. This is going to result in a NoPriceIncrease processing state.
  }

  test("amendment payload for 01") {
    // 01 : Digital Pack Monthly   : 15/06/2016 : Monthly
    val subscription = Fixtures.subscriptionFromJson("Migrations/DigiSubs2025/01/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/DigiSubs2025/01/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/DigiSubs2025/01/invoice-preview.json")

    // Estimation Data from above
    /*
      EstimationData(
        subscriptionName = subscription.subscriptionNumber,
        amendmentEffectiveDate =
          LocalDate.of(2026, 2, 15), // The first billing period after amendmentEffectiveDateLowerBound
        currency = "GBP",
        oldPrice = BigDecimal(14.99),
        estimatedNewPrice = BigDecimal(18.0),
        commsPrice = BigDecimal(18.0),
        billingPeriod = "Month"
      )
     */

    val cohortItem: CohortItem = CohortItem(
      subscriptionName = "subscriptionNumber",
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
      currency = Some("GBP"),

      // Pre migration price
      oldPrice = Some(BigDecimal(18.0)),

      // Price derived from the Estimation step, without capping
      estimatedNewPrice = Some(BigDecimal(18.0)),

      // Price (with possible capping) used in the communication to the user and sent to Salesforce
      commsPrice = Some(BigDecimal(18.0)),

      //
      billingPeriod = Some("Month")
    )

    val payload = DigiSubs2025Migration.amendmentOrderPayload(
      cohortItem,
      LocalDate.of(2025, 12, 20), // order date
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2026, 2, 15), // effect date, from the estimation data
      subscription, // Zuora subscription
      BigDecimal(18.0), // comms price
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-12-20",
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
             |                            "triggerDate": "2026-02-15"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2026-02-15"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2026-02-15"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a129276976df790019771a286c00071"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2026-02-15"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2026-02-15"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2026-02-15"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0fb4edd70c8014edeaa4eae220a",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fb4edd70c9014edeaa50342192",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 18
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
    // 03 : Digital Pack Annual    : 12/09/2025 : Annually
    val subscription = Fixtures.subscriptionFromJson("Migrations/DigiSubs2025/03/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/DigiSubs2025/03/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/DigiSubs2025/03/invoice-preview.json")

    // Estimation Data from above
    /*
        EstimationData(
          subscriptionName = subscription.subscriptionNumber,
          amendmentEffectiveDate =
            LocalDate.of(2026, 9, 28), // The first Annual billing period after amendmentEffectiveDateLowerBound
          currency = "GBP",
          oldPrice = BigDecimal(149.0),
          estimatedNewPrice = BigDecimal(180.0),
          commsPrice = BigDecimal(180.0),
          billingPeriod = "Annual"
        )
     */

    val cohortItem: CohortItem = CohortItem(
      subscriptionName = "subscriptionNumber",
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
      currency = Some("GBP"),

      // Pre migration price
      oldPrice = Some(BigDecimal(149.0)),

      // Price derived from the Estimation step, without capping
      estimatedNewPrice = Some(BigDecimal(180.0)),

      // Price (with possible capping) used in the communication to the user and sent to Salesforce
      commsPrice = Some(BigDecimal(180.0)),

      //
      billingPeriod = Some("Annual")
    )

    val payload = DigiSubs2025Migration.amendmentOrderPayload(
      cohortItem,
      LocalDate.of(2025, 12, 20), // order date
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2026, 9, 28), // effect date, from the estimation data
      subscription, // Zuora subscription
      BigDecimal(180.0), // comms price
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-12-20",
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
             |                            "triggerDate": "2026-09-28"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2026-09-28"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2026-09-28"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a1288a7993307bb01993e6bc4a61fac"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2026-09-28"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2026-09-28"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2026-09-28"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0fb4edd70c8014edeaa4e972204",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fb4edd70c9014edeaa5001218c",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 180
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
  }
}
