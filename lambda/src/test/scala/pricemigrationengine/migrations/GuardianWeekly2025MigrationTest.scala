package pricemigrationengine.migrations

import pricemigrationengine.Fixtures
import pricemigrationengine.model._
import pricemigrationengine.model.CohortTableFilter.{NotificationSendDateWrittenToSalesforce, ReadyForEstimation}

import java.time.{Instant, LocalDate}

// Subscription fixture: GBP-monthly1
// val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/subscription.json")
// val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/invoice-preview.json")

// Subscription fixture: EUR-annual1
// val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/EUR-annual1/subscription.json")
// val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2025/EUR-annual1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/EUR-annual1/invoice-preview.json")

// Subscription fixture: A-S00531704
// This one was introduced to investigate "Subscription A-S00531704 failed: DataExtractionFailure(Could not compute amendmentOrderPayload for subscription A-S00531704)"
// val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/A-S00531704/subscription.json")
// val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2025/A-S00531704/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/A-S00531704/invoice-preview.json")

class GuardianWeekly2025ExtraAttributesTest extends munit.FunSuite {

  test("decoding (0)") {
    val s = "{}"
    val attribute: GuardianWeekly2025ExtraAttributes = upickle.default.read[GuardianWeekly2025ExtraAttributes](s)
    assertEquals(attribute, GuardianWeekly2025ExtraAttributes(None, None))
  }

  test("decoding (1)") {
    val s = """{ "earliestMigrationDate": "2025-10-06" }"""
    val attribute: GuardianWeekly2025ExtraAttributes = upickle.default.read[GuardianWeekly2025ExtraAttributes](s)
    assertEquals(attribute, GuardianWeekly2025ExtraAttributes(Some(LocalDate.of(2025, 10, 6)), None))
  }

  test("decoding (2)") {
    val s = """{ "removeDiscount": true }"""
    val attribute: GuardianWeekly2025ExtraAttributes = upickle.default.read[GuardianWeekly2025ExtraAttributes](s)
    assertEquals(attribute, GuardianWeekly2025ExtraAttributes(None, Some(true)))
  }

  test("decoding (3)") {
    val s = """{ "earliestMigrationDate": "2025-10-06", "removeDiscount": false }"""
    val attribute: GuardianWeekly2025ExtraAttributes = upickle.default.read[GuardianWeekly2025ExtraAttributes](s)
    assertEquals(attribute, GuardianWeekly2025ExtraAttributes(Some(LocalDate.of(2025, 10, 6)), Some(false)))
  }

  test("getEarliestMigrationDateFromExtendedAttributes") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "earliestMigrationDate": "2025-10-06" }"""),
    )
    val date = GuardianWeekly2025Migration.getEarliestMigrationDateFromMigrationExtraAttributes(cohortItem)
    assertEquals(date, Some(LocalDate.of(2025, 10, 6)))
  }

  test("GuardianWeekly2025Migration.computeStartDateLowerBound4 (0): no extra attributes") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = None,
    )
    val date = GuardianWeekly2025Migration.computeStartDateLowerBound4(LocalDate.of(2025, 8, 9), cohortItem)
    assertEquals(date, LocalDate.of(2025, 8, 9))
  }

  test("GuardianWeekly2025Migration.computeStartDateLowerBound4 (1): bound3 is lower than extra attributes date") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "earliestMigrationDate": "2025-10-06" }"""),
    )
    val date = GuardianWeekly2025Migration.computeStartDateLowerBound4(LocalDate.of(2025, 8, 9), cohortItem)
    assertEquals(date, LocalDate.of(2025, 10, 6))
  }

  test("GuardianWeekly2025Migration.computeStartDateLowerBound4 (2): bound3 is higher than extra attributes date") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "earliestMigrationDate": "2025-10-06" }"""),
    )
    val date = GuardianWeekly2025Migration.computeStartDateLowerBound4(LocalDate.of(2025, 11, 9), cohortItem)
    assertEquals(date, LocalDate.of(2025, 11, 9))
  }
}

class GuardianWeekly2025MigrationTest extends munit.FunSuite {

  // For this migration we are using the (new) SubscriptionIntrospection2025, which
  // performs some heavy lifting, consequently we can focus on the migration function
  // themselves.

  test("GuardianWeekly2025Migration.priceLookUp") {
    assertEquals(
      GuardianWeekly2025Migration.priceLookUp(Domestic, Annual, "CAD"),
      Some(BigDecimal(432))
    )

    assertEquals(
      GuardianWeekly2025Migration.priceLookUp(RestOfWorld, Quarterly, "GBP"),
      Some(BigDecimal(83.9))
    )
  }

  test("GuardianWeekly2025Migration.subscriptionToLastPriceMigrationDate (GBP-monthly1)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/subscription.json")

    // Here we only have one active rate plan
    // "GW Oct 18 - Monthly - Domestic",
    // "originalOrderDate": "2024-04-13"

    assertEquals(
      GuardianWeekly2025Migration.subscriptionToLastPriceMigrationDate(subscription),
      Some(LocalDate.of(2024, 4, 13))
    )
  }

  test("GuardianWeekly2025Migration.subscriptionToLastPriceMigrationDate (EUR-annual1)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/EUR-annual1/subscription.json")

    // Here we only have one active rate plan (note that there also is an active discount)
    // "ratePlanName": "GW Oct 18 - Annual - Domestic",
    // "originalOrderDate": "2024-11-05",

    assertEquals(
      GuardianWeekly2025Migration.subscriptionToLastPriceMigrationDate(subscription),
      Some(LocalDate.of(2024, 11, 5))
    )
  }

  test("GuardianWeekly2025Migration.priceData (GBP-monthly1)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/invoice-preview.json")

    // Currency from subscription: "currency": "GBP"
    // Old price from currency: active rate plan, one single rate plan charge: "price": 15.0
    // price lookup (new price): (Monthly, "GBP") -> BigDecimal(16.5)
    // Billing frequency from currency: active rate plan, rate plan charge: "billingPeriod": "Month"

    assertEquals(
      GuardianWeekly2025Migration.priceData(subscription, invoicePreview, account),
      Right(PriceData("GBP", BigDecimal(15.0), BigDecimal(16.5), "Month"))
    )
  }

  test("GuardianWeekly2025Migration.priceData (EUR-annual1)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/EUR-annual1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2025/EUR-annual1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/EUR-annual1/invoice-preview.json")

    // Currency from subscription: "currency": "EUR"
    // Old price from currency: active rate plan, one single rate plan charge: "price": 318
    // price lookup (new price): (Annual, "EUR") -> BigDecimal(348)
    // Billing frequency from currency: active rate plan, rate plan charge: "billingPeriod": "Annual"

    assertEquals(
      GuardianWeekly2025Migration.priceData(subscription, invoicePreview, account),
      Right(PriceData("EUR", BigDecimal(318), BigDecimal(348), "Annual"))
    )
  }

  test("GuardianWeekly2025Migration.priceData (GBP-monthly1) (standard)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/invoice-preview.json")

    val cohortItem = CohortItem(
      "SUBSCRIPTION-NUMBER", // Doesn't really matter because we are not reading that in GuardianWeekly2025Migration.amendmentOrderPayload
      NotificationSendDateWrittenToSalesforce, // Doesn't really matter because we are not reading that in GuardianWeekly2025Migration.amendmentOrderPayload
      migrationExtraAttributes = None
    )

    // Currency from subscription: "currency": "GBP"
    // Old price from currency, active rate plan, one single rate plan charge: "price": 15.0
    // price lookup (new price): (Monthly, "GBP") -> BigDecimal(16.5)
    // Billing frequency from currency: active rate plan, rate plan charge: "billingPeriod": "Month"

    val orderDate = LocalDate.of(2025, 11, 7) // "2025-11-07"
    val accountNumber = "ed570ff842d582466"
    val subscriptionNumber = subscription.subscriptionNumber // "SUBSCRIPTION-NUMBER" (sanitised fixture)
    val effectDate = LocalDate.of(2025, 11, 8) // "2025-11-08": Will be used in tiggerDates
    val oldPrice = BigDecimal(15.0)
    val estimatedNewPrice = BigDecimal(16.5)
    val priceCap = BigDecimal(1.2)

    // So here, I think that the natural way to test is to compare Values.
    // We have one coming from GuardianWeekly2025Migration.amendmentOrderPayload
    // The other one is going to be parsed from the raw JSON string, which gives us
    // visibility on the actual payload.

    assertEquals(
      GuardianWeekly2025Migration.amendmentOrderPayload(
        cohortItem,
        orderDate,
        accountNumber,
        subscriptionNumber,
        effectDate,
        subscription,
        oldPrice,
        estimatedNewPrice,
        priceCap,
        invoicePreview
      ),
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-11-07",
             |    "existingAccountNumber": "ed570ff842d582466",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "SUBSCRIPTION-NUMBER",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-08"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-08"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-08"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a1299b39348e9bb019376218c532ba1"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-08"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-08"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-08"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0fd79ac64b00179ae3f9d474960",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd79ac64b00179ae3f9d704962",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 16.5
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

  test("GuardianWeekly2025Migration.priceData (GBP-monthly1) (active price cap)") {

    // This test is similar to the previous one but with an inflated estimated new price to
    // see the effect of the price cap

    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/invoice-preview.json")

    val cohortItem = CohortItem(
      "SUBSCRIPTION-NUMBER", // Doesn't really matter because we are not reading that in GuardianWeekly2025Migration.amendmentOrderPayload
      NotificationSendDateWrittenToSalesforce, // Doesn't really matter because we are not reading that in GuardianWeekly2025Migration.amendmentOrderPayload
      migrationExtraAttributes = None
    )

    // Currency from subscription: "currency": "GBP"
    // Old price from currency, active rate plan, one single rate plan charge: "price": 15.0
    // price lookup (new price): (Monthly, "GBP") -> BigDecimal(16.5)
    // Billing frequency from currency: active rate plan, rate plan charge: "billingPeriod": "Month"

    val orderDate = LocalDate.of(2025, 11, 7) // "2025-11-07"
    val accountNumber = "ed570ff842d582466"
    val subscriptionNumber = subscription.subscriptionNumber // "SUBSCRIPTION-NUMBER" (sanitised fixture)
    val effectDate = LocalDate.of(2025, 11, 8) // "2025-11-08": Will be used in tiggerDates
    val oldPrice = BigDecimal(200.0)
    val estimatedNewPrice = BigDecimal(300.0)
    val priceCap = BigDecimal(1.2)

    // So here, I think that the natural way to test is to compare Values.
    // We have one coming from GuardianWeekly2025Migration.amendmentOrderPayload
    // The other one is going to be parsed from the raw JSON string, which gives us
    // visibility on the actual payload.

    assertEquals(
      GuardianWeekly2025Migration.amendmentOrderPayload(
        cohortItem,
        orderDate,
        accountNumber,
        subscriptionNumber,
        effectDate,
        subscription,
        oldPrice,
        estimatedNewPrice,
        priceCap,
        invoicePreview
      ),
      Right(
        ujson.read(
          s"""{
            |    "orderDate": "2025-11-07",
            |    "existingAccountNumber": "ed570ff842d582466",
            |    "subscriptions": [
            |        {
            |            "subscriptionNumber": "SUBSCRIPTION-NUMBER",
            |            "orderActions": [
            |                {
            |                    "type": "RemoveProduct",
            |                    "triggerDates": [
            |                        {
            |                            "name": "ContractEffective",
            |                            "triggerDate": "2025-11-08"
            |                        },
            |                        {
            |                            "name": "ServiceActivation",
            |                            "triggerDate": "2025-11-08"
            |                        },
            |                        {
            |                            "name": "CustomerAcceptance",
            |                            "triggerDate": "2025-11-08"
            |                        }
            |                    ],
            |                    "removeProduct": {
            |                        "ratePlanId": "8a1299b39348e9bb019376218c532ba1"
            |                    }
            |                },
            |                {
            |                    "type": "AddProduct",
            |                    "triggerDates": [
            |                        {
            |                            "name": "ContractEffective",
            |                            "triggerDate": "2025-11-08"
            |                        },
            |                        {
            |                            "name": "ServiceActivation",
            |                            "triggerDate": "2025-11-08"
            |                        },
            |                        {
            |                            "name": "CustomerAcceptance",
            |                            "triggerDate": "2025-11-08"
            |                        }
            |                    ],
            |                    "addProduct": {
            |                        "productRatePlanId": "2c92a0fd79ac64b00179ae3f9d474960",
            |                        "chargeOverrides": [
            |                            {
            |                                "productRatePlanChargeId": "2c92a0fd79ac64b00179ae3f9d704962",
            |                                "pricing": {
            |                                    "recurringFlatFee": {
            |                                        "listPrice": 240
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

  test("GuardianWeekly2025Migration.priceData (EUR-annual1) (standard)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/EUR-annual1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2025/EUR-annual1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/EUR-annual1/invoice-preview.json")

    // Currency from subscription: "currency": "EUR"
    // Old price from currency, active rate plan, one single rate plan charge: "price": 318.0
    // price lookup (new price): (Annual, "EUR") -> BigDecimal(348.0)
    // Billing frequency from currency: active rate plan, rate plan charge: "billingPeriod": "Annual"

    val cohortItem = CohortItem(
      "SUBSCRIPTION-NUMBER", // Doesn't really matter because we are not reading that in GuardianWeekly2025Migration.amendmentOrderPayload
      NotificationSendDateWrittenToSalesforce, // Doesn't really matter because we are not reading that in GuardianWeekly2025Migration.amendmentOrderPayload
      migrationExtraAttributes = None
    )

    val orderDate = LocalDate.of(2025, 6, 24) // LocalDate.now()
    val accountNumber = "0b89c1c6b41e"
    val subscriptionNumber = subscription.subscriptionNumber // "SUBSCRIPTION-NUMBER" (sanitised fixture)
    val effectDate = LocalDate.of(2025, 11, 5) // "2025-11-08": Will be used in tiggerDates
    val oldPrice = BigDecimal(318.0)
    val estimatedNewPrice = BigDecimal(348.0)
    val priceCap = BigDecimal(1.2)

    // So here, I think that the natural way to test is to compare Values.
    // We have one coming from GuardianWeekly2025Migration.amendmentOrderPayload
    // The other one is going to be parsed from the raw JSON string, which gives us
    // visibility on the actual payload.

    assertEquals(
      GuardianWeekly2025Migration.amendmentOrderPayload(
        cohortItem,
        orderDate,
        accountNumber,
        subscriptionNumber,
        effectDate,
        subscription,
        oldPrice,
        estimatedNewPrice,
        priceCap,
        invoicePreview
      ),
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-06-24",
             |    "existingAccountNumber": "0b89c1c6b41e",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "SUBSCRIPTION-NUMBER",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-05"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-05"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-05"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a12820a92f75e4b0192fb2364496183"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-11-05"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-11-05"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-11-05"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0fe6619b4b901661aa8e66c1692",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fe6619b4b901661aa8e6811695",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 348.0
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

  test("A-S00531704 Investigation (1)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/A-S00531704/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2025/A-S00531704/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/A-S00531704/invoice-preview.json")

    // Here we test correct extraction of the rateplan
    // The subscription has an active Discount, a removed rate plan and the currently active "Guardian Weekly - Domestic"

    val rateplan = SI2025RateplanFromSubAndInvoices.determineRatePlan(subscription, invoicePreview)

    assertEquals(
      rateplan,
      Some(
        ZuoraRatePlan(
          id = "8a129ede979cc3330197a13698217602",
          productName = "Guardian Weekly - Domestic",
          productRatePlanId = "2c92a0fe6619b4b301661aa494392ee2",
          ratePlanName = "GW Oct 18 - Quarterly - Domestic",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "2c92a0fe6619b4b601661aa8b74e623f",
              name = "GW Oct 18 - Quarterly - Domestic",
              number = "C-05701607",
              currency = "GBP",
              price = Some(45.0),
              billingPeriod = Some("Quarter"),
              chargedThroughDate = Some(LocalDate.of(2025, 8, 11)),
              processedThroughDate = Some(LocalDate.of(2025, 5, 11)),
              specificBillingPeriod = None,
              endDateCondition = Some("Subscription_End"),
              upToPeriodsType = None,
              upToPeriods = None,
              billingDay = Some("ChargeTriggerDay"),
              triggerEvent = Some("CustomerAcceptance"),
              triggerDate = None,
              discountPercentage = None,
              originalOrderDate = Some(LocalDate.of(2024, 6, 23)),
              effectiveStartDate = Some(LocalDate.of(2024, 8, 11)),
              effectiveEndDate = Some(LocalDate.of(2026, 7, 30))
            )
          ),
          lastChangeType = Some("Add")
        )
      )
    )
  }

  test("A-S00531704 Investigation (2)") {

    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/A-S00531704/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2025/A-S00531704/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/A-S00531704/invoice-preview.json")

    // Here we test the step by step extraction of the `for` construct in amendmentOrderPayload

    val ratePlan = ZuoraRatePlan(
      id = "8a129ede979cc3330197a13698217602",
      productName = "Guardian Weekly - Domestic",
      productRatePlanId = "2c92a0fe6619b4b301661aa494392ee2",
      ratePlanName = "GW Oct 18 - Quarterly - Domestic",
      ratePlanCharges = List(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fe6619b4b601661aa8b74e623f",
          name = "GW Oct 18 - Quarterly - Domestic",
          number = "C-05701607",
          currency = "GBP",
          price = Some(45.0),
          billingPeriod = Some("Quarter"),
          chargedThroughDate = Some(LocalDate.of(2025, 8, 11)),
          processedThroughDate = Some(LocalDate.of(2025, 5, 11)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 6, 23)),
          effectiveStartDate = Some(LocalDate.of(2024, 8, 11)),
          effectiveEndDate = Some(LocalDate.of(2026, 7, 30))
        )
      ),
      lastChangeType = Some("Add")
    )

    val subscriptionRatePlanId = ratePlan.id

    assertEquals(
      subscriptionRatePlanId,
      "8a129ede979cc3330197a13698217602"
    )

    val effectDate = LocalDate.of(2025, 8, 11) // read from the CohortItem

    val removeProduct = ZuoraOrdersApiPrimitives.removeProduct(effectDate.toString, subscriptionRatePlanId)

    assertEquals(
      removeProduct,
      ujson.read(
        s"""{
           |    "type": "RemoveProduct",
           |    "triggerDates": [
           |        {
           |            "name": "ContractEffective",
           |            "triggerDate": "2025-08-11"
           |        },
           |        {
           |            "name": "ServiceActivation",
           |            "triggerDate": "2025-08-11"
           |        },
           |        {
           |            "name": "CustomerAcceptance",
           |            "triggerDate": "2025-08-11"
           |        }
           |    ],
           |    "removeProduct": {
           |        "ratePlanId": "8a129ede979cc3330197a13698217602"
           |    }
           |}""".stripMargin
      )
    )

    val triggerDateString = effectDate.toString

    val productRatePlanId = ratePlan.productRatePlanId

    val oldPrice = BigDecimal(45)
    val estimatedNewPrice = BigDecimal(49.5)
    val priceCap = 1.2

    val chargeOverrides = List(
      ZuoraOrdersApiPrimitives.chargeOverride(
        ratePlan.ratePlanCharges.headOption.get.productRatePlanChargeId,
        PriceCap.cappedPrice(oldPrice, estimatedNewPrice, priceCap),
        "Quarter"
      )
    )

    val addProduct = ZuoraOrdersApiPrimitives.addProduct(triggerDateString, productRatePlanId, chargeOverrides)

    assertEquals(
      addProduct,
      ujson.read(
        s"""{
           |    "type": "AddProduct",
           |    "triggerDates": [
           |        {
           |            "name": "ContractEffective",
           |            "triggerDate": "2025-08-11"
           |        },
           |        {
           |            "name": "ServiceActivation",
           |            "triggerDate": "2025-08-11"
           |        },
           |        {
           |            "name": "CustomerAcceptance",
           |            "triggerDate": "2025-08-11"
           |        }
           |    ],
           |    "addProduct": {
           |        "productRatePlanId": "2c92a0fe6619b4b301661aa494392ee2",
           |        "chargeOverrides": [
           |            {
           |                "productRatePlanChargeId": "2c92a0fe6619b4b601661aa8b74e623f",
           |                "pricing": {
           |                    "recurringFlatFee": {
           |                        "listPrice": 49.5
           |                    }
           |                },
           |                "billing": {
           |                    "billingPeriod": "Quarter"
           |                }
           |            }
           |        ]
           |    }
           |}""".stripMargin
      )
    )

    val subscriptionNumber = subscription.subscriptionNumber

    val order_subscription =
      ZuoraOrdersApiPrimitives.subscription(subscriptionNumber, List(removeProduct), List(addProduct))

    assertEquals(
      order_subscription,
      ujson.read(
        s"""{
           |    "subscriptionNumber": "SUBSCRIPTION-NUMBER",
           |    "orderActions": [
           |        {
           |            "type": "RemoveProduct",
           |            "triggerDates": [
           |                {
           |                    "name": "ContractEffective",
           |                    "triggerDate": "2025-08-11"
           |                },
           |                {
           |                    "name": "ServiceActivation",
           |                    "triggerDate": "2025-08-11"
           |                },
           |                {
           |                    "name": "CustomerAcceptance",
           |                    "triggerDate": "2025-08-11"
           |                }
           |            ],
           |            "removeProduct": {
           |                "ratePlanId": "8a129ede979cc3330197a13698217602"
           |            }
           |        },
           |        {
           |            "type": "AddProduct",
           |            "triggerDates": [
           |                {
           |                    "name": "ContractEffective",
           |                    "triggerDate": "2025-08-11"
           |                },
           |                {
           |                    "name": "ServiceActivation",
           |                    "triggerDate": "2025-08-11"
           |                },
           |                {
           |                    "name": "CustomerAcceptance",
           |                    "triggerDate": "2025-08-11"
           |                }
           |            ],
           |            "addProduct": {
           |                "productRatePlanId": "2c92a0fe6619b4b301661aa494392ee2",
           |                "chargeOverrides": [
           |                    {
           |                        "productRatePlanChargeId": "2c92a0fe6619b4b601661aa8b74e623f",
           |                        "pricing": {
           |                            "recurringFlatFee": {
           |                                "listPrice": 49.5
           |                            }
           |                        },
           |                        "billing": {
           |                            "billingPeriod": "Quarter"
           |                        }
           |                    }
           |                ]
           |            }
           |        }
           |    ]
           |}""".stripMargin
      )
    )

  }

  test("A-S00531704 Investigation (3)") {

    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/A-S00531704/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2025/A-S00531704/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/A-S00531704/invoice-preview.json")

    // Here is the rate plan from step (1).

    val ratePlan = ZuoraRatePlan(
      id = "8a129ede979cc3330197a13698217602",
      productName = "Guardian Weekly - Domestic",
      productRatePlanId = "2c92a0fe6619b4b301661aa494392ee2",
      ratePlanName = "GW Oct 18 - Quarterly - Domestic",
      ratePlanCharges = List(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fe6619b4b601661aa8b74e623f",
          name = "GW Oct 18 - Quarterly - Domestic",
          number = "C-05701607",
          currency = "GBP",
          price = Some(45.0),
          billingPeriod = Some("Quarter"),
          chargedThroughDate = Some(LocalDate.of(2025, 8, 11)),
          processedThroughDate = Some(LocalDate.of(2025, 5, 11)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 6, 23)),
          effectiveStartDate = Some(LocalDate.of(2024, 8, 11)),
          effectiveEndDate = Some(LocalDate.of(2026, 7, 30))
        )
      ),
      lastChangeType = Some("Add")
    )

    // We also identity the cohort item, which I read from the Dynamo table

    val startDate = LocalDate.of(2025, 8, 11)
    val oldPrice = BigDecimal(45)
    val estimatedNewPrice = BigDecimal(49.5)

    val cohortItem = CohortItem(
      subscriptionName = subscription.subscriptionNumber,
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
      amendmentEffectiveDate = Some(startDate),
      currency = Some("GBP"),
      oldPrice = Some(oldPrice),
      estimatedNewPrice = Some(estimatedNewPrice),
      billingPeriod = Some("Quarter")
    )

    // We now collect the arguments of GuardianWeekly2025Migration.amendmentOrderPayload

    val orderDate = LocalDate.of(2025, 6, 24) // LocalDate.now()
    val accountNumber = subscription.accountNumber
    val subscriptionNumber = subscription.subscriptionNumber
    val effectDate = startDate
    val priceCap = 1.2

    assertEquals(
      GuardianWeekly2025Migration.amendmentOrderPayload(
        cohortItem,
        orderDate,
        accountNumber,
        subscriptionNumber,
        effectDate,
        subscription,
        oldPrice,
        estimatedNewPrice,
        priceCap,
        invoicePreview
      ),
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-06-24",
             |    "existingAccountNumber": "ACCOUNT-NUMBER",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "SUBSCRIPTION-NUMBER",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-08-11"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-08-11"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-08-11"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a129ede979cc3330197a13698217602"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-08-11"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-08-11"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-08-11"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0fe6619b4b301661aa494392ee2",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fe6619b4b601661aa8b74e623f",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 49.5
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
  }

  test("GuardianWeekly2025Migration.getDiscount") {
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/6801-Discount/subscription.json")
    assertEquals(
      SI2025Extractions.getDiscountByRatePlanName(subscription, "Percentage"),
      Some(
        ZuoraRatePlan(
          id = "8a129ce595aa3a180195c130cca57d19",
          productName = "Discounts",
          productRatePlanId = "2c92a0ff5345f9220153559d915d5c26",
          ratePlanName = "Percentage",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "2c92a0fd5345efa10153559e97bb76c6",
              name = "Percentage",
              number = "C-01271544",
              currency = "AUD",
              price = None,
              billingPeriod = Some("Annual"),
              chargedThroughDate = Some(LocalDate.of(2026, 3, 23)),
              processedThroughDate = Some(LocalDate.of(2025, 3, 23)),
              specificBillingPeriod = None,
              endDateCondition = Some("Subscription_End"),
              upToPeriodsType = None,
              upToPeriods = None,
              billingDay = Some("DefaultFromCustomer"),
              triggerEvent = Some("CustomerAcceptance"),
              triggerDate = None,
              discountPercentage = Some(10),
              originalOrderDate = Some(LocalDate.of(2018, 3, 20)),
              effectiveStartDate = Some(LocalDate.of(2018, 3, 23)),
              effectiveEndDate = Some(LocalDate.of(2026, 3, 23))
            )
          ),
          lastChangeType = Some("Add")
        )
      )
    )
  }

  test("6801-Discount (testing the discount payload)") {

    val subscription =
      Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/6801-Discount/subscription.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/6801-Discount/invoice-preview.json")

    val startDate = LocalDate.of(2025, 8, 11)
    val oldPrice = BigDecimal(45)
    val estimatedNewPrice = BigDecimal(49.5)

    val cohortItem = CohortItem(
      subscriptionName = subscription.subscriptionNumber,
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
      amendmentEffectiveDate = Some(startDate),
      currency = Some("GBP"),
      oldPrice = Some(oldPrice),
      estimatedNewPrice = Some(estimatedNewPrice),
      billingPeriod = Some("Quarter"),
      migrationExtraAttributes = Some("""{ "removeDiscount": true }""")
    )

    // We now collect the arguments of GuardianWeekly2025Migration.amendmentOrderPayload

    val orderDate = LocalDate.of(2025, 6, 24) // LocalDate.now()
    val accountNumber = subscription.accountNumber
    val subscriptionNumber = subscription.subscriptionNumber
    val effectDate = startDate
    val priceCap = 1.2

    // In the test below
    // "8a129ce595aa3a180195c130cc947d0a" is the id of "GW Oct 18 - Annual - ROW", and
    // "8a129ce595aa3a180195c130cca57d19" is the id of the Percentage Discount.
    // We have two RemoveProduct, and one AddProduct

    assertEquals(
      GuardianWeekly2025Migration.amendmentOrderPayload(
        cohortItem,
        orderDate,
        accountNumber,
        subscriptionNumber,
        effectDate,
        subscription,
        oldPrice,
        estimatedNewPrice,
        priceCap,
        invoicePreview
      ),
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-06-24",
             |    "existingAccountNumber": "ACCOUNT-NUMBER",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "SUBSCRIPTION-NUMBER",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-08-11"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-08-11"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-08-11"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a129ce595aa3a180195c130cc947d0a"
             |                    }
             |                },
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-08-11"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-08-11"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-08-11"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a129ce595aa3a180195c130cca57d19"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-08-11"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-08-11"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-08-11"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0fe6619b4b601661ab300222651",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fe6619b4b601661ab3002f2653",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 49.5
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

  test("Testing A-S01332827 (strange payload)") {

    val subscription =
      Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/A-S01332827/subscription.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/A-S01332827/invoice-preview.json")

    val startDate = LocalDate.of(2025, 8, 5)
    val oldPrice = BigDecimal(67.5)
    val estimatedNewPrice = BigDecimal(87.0)

    val cohortItem = CohortItem(
      subscriptionName = subscription.subscriptionNumber,
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
      amendmentEffectiveDate = Some(startDate),
      currency = Some("EUR"),
      oldPrice = Some(oldPrice),
      estimatedNewPrice = Some(estimatedNewPrice),
      billingPeriod = Some("Quarter"),
      migrationExtraAttributes = None
    )

    // We now collect the arguments of GuardianWeekly2025Migration.amendmentOrderPayload

    val orderDate = LocalDate.of(2025, 7, 4) // LocalDate.now()
    val accountNumber = subscription.accountNumber
    val subscriptionNumber = subscription.subscriptionNumber
    val effectDate = startDate
    val priceCap = 1.2

    // In the test below
    // "8a129ce595aa3a180195c130cc947d0a" is the id of "GW Oct 18 - Annual - ROW", and
    // "8a129ce595aa3a180195c130cca57d19" is the id of the Percentage Discount.
    // We have two RemoveProduct, and one AddProduct

    assertEquals(
      GuardianWeekly2025Migration.amendmentOrderPayload(
        cohortItem,
        orderDate,
        accountNumber,
        subscriptionNumber,
        effectDate,
        subscription,
        oldPrice,
        estimatedNewPrice,
        priceCap,
        invoicePreview
      ),
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-07-04",
             |    "existingAccountNumber": "ACCOUNT-NUMBER",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "SUBSCRIPTION-NUMBER",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-08-05"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-08-05"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-08-05"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a1298ad97aef8350197d59057e638ab"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-08-05"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-08-05"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-08-05"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0fe6619b4b301661aa494392ee2",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fe6619b4b601661aa8b74e623f",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 81
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
  }

  test("73291-GW-ROW-EUR") {

    val subscription =
      Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/73291-GW-ROW-EUR/subscription.json")

    val invoicePreview =
      Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/73291-GW-ROW-EUR/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan(subscription, invoicePreview).get
    val ratePlanName = ratePlan.ratePlanName
    val currency = SI2025Extractions.determineCurrency(ratePlan).get

    // Testing that we have the correct ratePlanName and currency pair,
    // as well as the expected productRatePlanId and the expected productRatePlanChargeId
    assertEquals(ratePlanName, "GW Oct 18 - Quarterly - ROW")
    assertEquals(currency, "EUR")
    assertEquals(ratePlan.productRatePlanId, "2c92a0086619bf8901661ab02752722f")
    assertEquals(ratePlan.ratePlanCharges.headOption.get.productRatePlanChargeId, "2c92a0ff6619bf8b01661ab2d0396eb2")

    // Then we check that we are computing the right target productRatePlanId
    assertEquals(
      GuardianWeekly2025Migration.determineTargetProductRatePlanId(ratePlan),
      "2c92a0fe6619b4b301661aa494392ee2"
    )

    // ... and the right productRatePlanChargeId
    assertEquals(
      GuardianWeekly2025Migration.determineTargetRatePlanChargeId(ratePlan),
      "2c92a0fe6619b4b601661aa8b74e623f"
    )
  }
}
