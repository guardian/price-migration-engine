package pricemigrationengine.migrations

import pricemigrationengine.Fixtures
import pricemigrationengine.model._
import pricemigrationengine.libs._
import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation

import java.time.{Instant, LocalDate}

// Subscription fixture: GBP-monthly1
// val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/subscription.json")
// val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/invoice-preview.json")

// Subscription fixture: EUR-annual1
// val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/EUR-annual1/subscription.json")
// val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2025/EUR-annual1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/EUR-annual1/invoice-preview.json")

class GuardianWeekly2025ExtraAttributesTest extends munit.FunSuite {

  test("decoding") {
    val s = """{ "earliestMigrationDate": "2025-10-06" }"""
    val attribute: GuardianWeekly2025ExtraAttributes = upickle.default.read[GuardianWeekly2025ExtraAttributes](s)
    assertEquals(attribute, GuardianWeekly2025ExtraAttributes(LocalDate.of(2025, 10, 6)))
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

  test("GuardianWeekly2025Migration.priceData (GBP-monthly1)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2025/GBP-monthly1/invoice-preview.json")

    // Currency from subscription: "currency": "GBP"
    // Old price from currency: active rate plan, one single rate plan charge: "price": 15.0
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
