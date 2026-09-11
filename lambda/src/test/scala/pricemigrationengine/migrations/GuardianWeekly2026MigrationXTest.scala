package pricemigrationengine.migrations

import pricemigrationengine.Fixtures
import pricemigrationengine.model._
import pricemigrationengine.model.CohortTableFilter.{NotificationSendDateWrittenToSalesforce, ReadyForEstimation}

import java.time.{Instant, LocalDate}

// sub1: "Guardian Weekly - Domestic"  "GW Oct 18 - Quarterly - Domestic"  "GBP"  "Quarter"
// val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2026X/sub1/subscription.json")
// val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2026X/sub1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2026X/sub1/invoice-preview.json")

// sub2: "Guardian Weekly - Domestic"  "GW Oct 18 - Quarterly - Domestic"  "AUD"  "Quarter"
// sub3: "Guardian Weekly - Domestic"  "GW Oct 18 - Quarterly - Domestic"  "EUR"  "Quarter"
// sub4: "Guardian Weekly - Domestic"  "GW Oct 18 - Monthly - Domestic"    "GBP"  "Month"
// sub5: "Guardian Weekly - Domestic"  "GW Oct 18 - Annual - Domestic"     "GBP"  "Annual"
// sub6: "Guardian Weekly - Domestic"  "GW Oct 18 - Annual - Domestic"     "GBP"  "Annual"
//        variant of sub5 to test the 10% capping
//        price if set to 100 GBP
// sub7: "Guardian Weekly Zone B"                                          "CAD"  "Semi_Annual"

class GuardianWeekly2026MigrationXTest extends munit.FunSuite {
  test("getNewPrice") {
    assertEquals(GuardianWeekly2026MigrationX.getNewPrice(Monthly, "GBP", Domestic), Some(BigDecimal(17.50)))
    assertEquals(GuardianWeekly2026MigrationX.getNewPrice(Monthly, "AUD", Domestic), Some(BigDecimal(48.00)))
    assertEquals(GuardianWeekly2026MigrationX.getNewPrice(Quarterly, "USD", RestOfWorld), Some(BigDecimal(114)))
    assertEquals(GuardianWeekly2026MigrationX.getNewPrice(Annual, "NZD", Domestic), Some(BigDecimal(720)))
  }
  test("getNewPrice") {
    // sub1: "Guardian Weekly - Domestic"  "GW Oct 18 - Quarterly - Domestic"  "GBP"  "Quarter"
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2026X/sub1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2026X/sub1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2026X/sub1/invoice-preview.json")
    // Quarter, GPB, Domestic, we are expecting 52
    assertEquals(GuardianWeekly2026MigrationX.getNewPrice(subscription, invoicePreview, account), Some(BigDecimal(52)))
  }
  test("getNewPrice") {
    // sub2: "Guardian Weekly - Domestic"  "GW Oct 18 - Quarterly - Domestic"  "AUD"  "Quarter"
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2026X/sub2/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2026X/sub2/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2026X/sub2/invoice-preview.json")
    // Quarter, AUD, Domestic, we are expecting 114
    assertEquals(GuardianWeekly2026MigrationX.getNewPrice(subscription, invoicePreview, account), Some(BigDecimal(144)))
  }
  test("getNewPrice") {
    // sub4: "Guardian Weekly - Domestic"  "GW Oct 18 - Monthly - Domestic"    "GBP"  "Month"
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2026X/sub4/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2026X/sub4/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2026X/sub4/invoice-preview.json")
    // Month, GBP, Domestic, we are expecting 17.50
    assertEquals(
      GuardianWeekly2026MigrationX.getNewPrice(subscription, invoicePreview, account),
      Some(BigDecimal(17.50))
    )
  }
  test("priceData") {
    // sub1: "Guardian Weekly - Domestic"  "GW Oct 18 - Quarterly - Domestic"  "GBP"  "Quarter"
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2026X/sub1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2026X/sub1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2026X/sub1/invoice-preview.json")
    assertEquals(
      GuardianWeekly2026MigrationX.priceData(CohortSpec("Test1", true), subscription, invoicePreview, account),
      Right(PriceData("GBP", BigDecimal(49.5), BigDecimal(52), BigDecimal(52), "Quarter"))
    )
  }
  test("priceData") {
    // sub2: "Guardian Weekly - Domestic"  "GW Oct 18 - Quarterly - Domestic"  "AUD"  "Quarter"
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2026X/sub2/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2026X/sub2/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2026X/sub2/invoice-preview.json")
    assertEquals(
      GuardianWeekly2026MigrationX.priceData(CohortSpec("Test1", true), subscription, invoicePreview, account),
      Right(PriceData("AUD", BigDecimal(132.0), BigDecimal(144), BigDecimal(144), "Quarter"))
    )
  }
  test("priceData") {
    // sub3: "Guardian Weekly - Domestic"  "GW Oct 18 - Quarterly - Domestic"  "EUR"  "Quarter"
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2026X/sub3/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2026X/sub3/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2026X/sub3/invoice-preview.json")
    assertEquals(
      GuardianWeekly2026MigrationX.priceData(CohortSpec("Test1", true), subscription, invoicePreview, account),
      Right(PriceData("EUR", BigDecimal(87.0), BigDecimal(91.5), BigDecimal(91.5), "Quarter"))
    )
  }
  test("priceData") {
    // sub4: "Guardian Weekly - Domestic"  "GW Oct 18 - Monthly - Domestic"    "GBP"  "Month"
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2026X/sub4/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2026X/sub4/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2026X/sub4/invoice-preview.json")
    assertEquals(
      GuardianWeekly2026MigrationX.priceData(CohortSpec("Test1", true), subscription, invoicePreview, account),
      Right(PriceData("GBP", BigDecimal(16.5), BigDecimal(17.50), BigDecimal(17.50), "Month"))
    )
  }
  test("priceData") {
    // sub5: "Guardian Weekly - Domestic"  "GW Oct 18 - Annual - Domestic"     "GBP"  "Annual"
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2026X/sub5/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2026X/sub5/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2026X/sub5/invoice-preview.json")
    assertEquals(
      GuardianWeekly2026MigrationX.priceData(CohortSpec("Test1", true), subscription, invoicePreview, account),
      Right(PriceData("GBP", BigDecimal(198.0), BigDecimal(208), BigDecimal(208), "Annual"))
    )
  }
  test("priceData") {
    // sub6: "Guardian Weekly - Domestic"  "GW Oct 18 - Annual - Domestic"     "GBP"  "Annual"
    //        variant of sub5 to test the 10% capping
    //        price if set to 100 GBP
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2026X/sub6/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2026X/sub6/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2026X/sub6/invoice-preview.json")
    assertEquals(
      GuardianWeekly2026MigrationX
        .priceData(CohortSpec("Print2026C1GWAnnualsUK", true), subscription, invoicePreview, account),
      Right(PriceData("GBP", BigDecimal(100.0), BigDecimal(208), BigDecimal(110), "Annual"))
    )
  }
  test("priceData") {
    // sub7: "Guardian Weekly Zone B" "CAD"  "Semi_Annual"
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2026X/sub7/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2026X/sub7/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2026X/sub7/invoice-preview.json")
    // We have a big jump because the sub was widely under priced
    // No capping
    assertEquals(
      GuardianWeekly2026MigrationX.priceData(CohortSpec("Test1", true), subscription, invoicePreview, account),
      Right(PriceData("CAD", BigDecimal(120.0), BigDecimal(237.0), BigDecimal(237.0), "Semi_Annual"))
    )
  }
  test("priceData") {
    // sub7: "Guardian Weekly Zone B" "CAD"  "Semi_Annual"
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2026X/sub7/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2026X/sub7/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2026X/sub7/invoice-preview.json")
    // We have a big jump because the sub was widely under priced
    // With 10% capping coming from the migration name
    assertEquals(
      GuardianWeekly2026MigrationX.priceData(
        CohortSpec("Print2026C5GWMonthliesAnnualsNoEmailsNonUK", true),
        subscription,
        invoicePreview,
        account
      ),
      Right(PriceData("CAD", BigDecimal(120.0), BigDecimal(237.0), BigDecimal(132.00), "Semi_Annual"))
    )
  }
  test("amendmentOrderPayload") {
    // sub1: "Guardian Weekly - Domestic"  "GW Oct 18 - Quarterly - Domestic"  "GBP"  "Quarter"
    val subscription = Fixtures.subscriptionFromJson("Migrations/GuardianWeekly2026X/sub1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GuardianWeekly2026X/sub1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GuardianWeekly2026X/sub1/invoice-preview.json")

    val amendmentEffectiveDate = LocalDate.of(2026, 10, 19) // 2026-10-19
    val oldPrice = BigDecimal(49.5)
    val estimatedNewPrice = BigDecimal(52)
    val commsPrice = BigDecimal(52)

    val cohortItem = CohortItem(
      subscriptionName = subscription.subscriptionNumber,
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
      amendmentEffectiveDate = Some(amendmentEffectiveDate),
      currency = Some("GBP"),
      oldPrice = Some(oldPrice),
      estimatedNewPrice = Some(estimatedNewPrice),
      billingPeriod = Some("Quarter")
    )

    // We now collect the arguments of GuardianWeekly2026X.amendmentOrderPayload

    val orderDate = LocalDate.of(2025, 6, 24) // LocalDate.now()
    val accountNumber = subscription.accountNumber
    val subscriptionNumber = subscription.subscriptionNumber
    val effectDate = amendmentEffectiveDate
    val priceCap = 1.1

    assertEquals(
      GuardianWeekly2026MigrationX.amendmentOrderPayload(
        cohortItem,
        orderDate,
        accountNumber,
        subscriptionNumber,
        effectDate,
        subscription,
        commsPrice,
        invoicePreview
      ),
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2025-06-24",
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
             |                            "triggerDate": "2026-10-19"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2026-10-19"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2026-10-19"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a129518979cc3280197a0c0567a6685"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2026-10-19"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2026-10-19"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2026-10-19"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0fe6619b4b301661aa494392ee2",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fe6619b4b601661aa8b74e623f",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 52
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
}
