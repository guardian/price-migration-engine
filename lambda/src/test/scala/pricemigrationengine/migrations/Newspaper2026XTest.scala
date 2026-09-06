package pricemigrationengine.migrations

import pricemigrationengine.Fixtures
import pricemigrationengine.model._
import pricemigrationengine.model.CohortTableFilter.{NotificationSendDateWrittenToSalesforce, ReadyForEstimation}

import java.time.{Instant, LocalDate}

//Newspaper:
// sub1: "Newspaper Voucher"          "Everyday+"   "GBP"   "Month"
// val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub1/subscription.json")
// val account = Fixtures.accountFromJson("Migrations/Newspaper2026X/sub1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2026X/sub1/invoice-preview.json")

// sub2:  "Newspaper Digital Voucher"  "Everyday+"
// sub3:  "Newspaper Delivery"         "Everyday+"
// sub4:  "Newspaper Voucher"          "Sixday+"
// sub5:  "Newspaper Voucher"          "Weekend+"    "GBP"   "Month"
// sub6:  "Newspaper Voucher"          "Everyday"
// sub7:  "Newspaper Voucher"          "Sixday"
// sub8:  "Newspaper Voucher"          "Sixday+"     "GBP"   "Quarter"
// sub9:  "Newspaper Voucher"          "Everyday+"   "GBP"   "Annual"
// sub10: "Newspaper Voucher"          "Everyday+"   "GBP"   "Annual"
//        special edition of sub9 to test the 7.1% price cap
//        charges sum to 100 GBP

class Newspaper2026XTest extends munit.FunSuite {
  test("getNewPrice") {
    assertEquals(Newspaper2026X.getNewPrice(Monthly, Voucher, EverydayBasicAndPlus), Some(BigDecimal(72.99)))
    assertEquals(Newspaper2026X.getNewPrice(Quarterly, HomeDelivery, WeekendBasicAndPlus), Some(BigDecimal(110.97)))
  }
  test("decideFulfillment") {
    // sub1: "Newspaper Voucher"          "Everyday+"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub1/subscription.json")
    // Product name is "Newspaper Voucher", so we expect `Voucher`
    assertEquals(
      Newspaper2026X.decideFulfillment(subscription, LocalDate.of(2026, 8, 3)),
      Some(Voucher)
    )
  }
  test("decideFulfillment") {
    // sub3: "Newspaper Delivery"         "Everyday+"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub3/subscription.json")
    // Product name is "Newspaper Delivery", so we expect `Voucher`
    assertEquals(
      Newspaper2026X.decideFulfillment(subscription, LocalDate.of(2026, 8, 3)),
      Some(HomeDelivery)
    )
  }
  test("decidePackage") {
    // sub1: "Newspaper Voucher"          "Everyday+"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub1/subscription.json")
    assertEquals(
      Newspaper2026X.decidePackage(subscription, LocalDate.of(2026, 8, 3)),
      Some(EverydayBasicAndPlus)
    )
  }
  test("decidePackage") {
    // sub4: "Newspaper Voucher"          "Sixday+"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub4/subscription.json")
    assertEquals(
      Newspaper2026X.decidePackage(subscription, LocalDate.of(2026, 8, 3)),
      Some(SixdayBasicAndPlus)
    )
  }
  test("decidePackage") {
    // sub6: "Newspaper Voucher"          "Everyday"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub6/subscription.json")
    assertEquals(
      Newspaper2026X.decidePackage(subscription, LocalDate.of(2026, 8, 3)),
      Some(EverydayBasicAndPlus)
    )
  }
  test("decidePackage") {
    // sub7: "Newspaper Voucher"          "Sixday"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub7/subscription.json")
    assertEquals(
      Newspaper2026X.decidePackage(subscription, LocalDate.of(2026, 8, 3)),
      Some(SixdayBasicAndPlus)
    )
  }
  // -----------
  test("decideBrandTitle") {
    // sub1: "Newspaper Voucher"          "Everyday+"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub1/subscription.json")
    assertEquals(
      Newspaper2026X.decideBrandTitle(subscription, LocalDate.of(2026, 8, 3)),
      Some("the Guardian and the Observer")
    )
  }
  test("decideBrandTitle") {
    // sub4: "Newspaper Voucher"          "Sixday+"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub4/subscription.json")
    assertEquals(
      Newspaper2026X.decideBrandTitle(subscription, LocalDate.of(2026, 8, 3)),
      Some("the Guardian")
    )
  }
  test("decideBrandTitle") {
    // sub6: "Newspaper Voucher"          "Everyday"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub6/subscription.json")
    assertEquals(
      Newspaper2026X.decideBrandTitle(subscription, LocalDate.of(2026, 8, 3)),
      Some("the Guardian and the Observer")
    )
  }
  test("decideBrandTitle") {
    // sub7: "Newspaper Voucher"          "Sixday"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub7/subscription.json")
    assertEquals(
      Newspaper2026X.decideBrandTitle(subscription, LocalDate.of(2026, 8, 3)),
      Some("the Guardian")
    )
  }
  test("priceData") {
    // sub1: "Newspaper Voucher"          "Everyday+"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2026X/sub1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2026X/sub1/invoice-preview.json")
    assertEquals(
      Newspaper2026X.priceData(
        CohortSpec("Test1", true),
        subscription,
        invoicePreview,
        account: ZuoraAccount,
        LocalDate.of(2026, 9, 14)
      ),
      Right(PriceData("GBP", BigDecimal(69.99), BigDecimal(72.99), BigDecimal(72.99), "Month"))
    )
  }
  test("priceData") {
    // sub7: "Newspaper Voucher"          "Sixday"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub7/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2026X/sub7/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2026X/sub7/invoice-preview.json")
    assertEquals(
      Newspaper2026X.priceData(
        CohortSpec("Test1", true),
        subscription,
        invoicePreview,
        account: ZuoraAccount,
        LocalDate.of(2026, 9, 14)
      ),
      Right(PriceData("GBP", BigDecimal(61.99), BigDecimal(64.99), BigDecimal(64.99), "Month"))
    )
  }
  test("priceData") {
    // sub8: "Newspaper Voucher"          "Sixday+"     "GBP"   "Quarter"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub8/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2026X/sub8/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2026X/sub8/invoice-preview.json")
    assertEquals(
      Newspaper2026X.priceData(
        CohortSpec("Test1", true),
        subscription,
        invoicePreview,
        account: ZuoraAccount,
        LocalDate.of(2026, 9, 14)
      ),
      Right(PriceData("GBP", BigDecimal(185.97), BigDecimal(194.97), BigDecimal(194.97), "Quarter"))
    )
  }
  test("priceData") {
    // sub9: "Newspaper Voucher"          "Everyday+"   "GBP"   "Annual"
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub9/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2026X/sub9/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2026X/sub9/invoice-preview.json")
    assertEquals(
      Newspaper2026X.priceData(
        CohortSpec("Test1", true),
        subscription,
        invoicePreview,
        account: ZuoraAccount,
        LocalDate.of(2026, 9, 14)
      ),
      Right(PriceData("GBP", BigDecimal(839.88), BigDecimal(875.88), BigDecimal(875.88), "Annual"))
    )
  }
  test("priceData") {
    // sub10: "Newspaper Voucher"          "Everyday+"   "GBP"   "Annual"
    //        special edition of sub9 to test the 7.1% price cap
    //        charges sum to 100 GBP
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub10/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2026X/sub10/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2026X/sub10/invoice-preview.json")
    assertEquals(
      Newspaper2026X.priceData(
        CohortSpec("Print2026C2NPMonthliesUK", true),
        subscription,
        invoicePreview,
        account: ZuoraAccount,
        LocalDate.of(2026, 9, 14)
      ),
      Right(PriceData("GBP", BigDecimal(100), BigDecimal(875.88), BigDecimal(107.10), "Annual"))
    )
  }
  test("Newspaper2026X.amendmentOrderPayload") {

    // sub1: "Newspaper Voucher"          "Everyday+"   "GBP"   "Month"

    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub1/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2026X/sub1/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2026X/sub1/invoice-preview.json")

    val amendmentEffectiveDate = LocalDate.of(2026, 11, 1)
    val oldPrice = BigDecimal(69.99)
    val estimatedNewPrice = BigDecimal(72.99)
    val commsPrice = BigDecimal(72.99)

    val cohortItem = CohortItem(
      subscriptionName = subscription.subscriptionNumber,
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
      amendmentEffectiveDate = Some(amendmentEffectiveDate),
      currency = Some("GBP"),
      oldPrice = Some(oldPrice),
      estimatedNewPrice = Some(estimatedNewPrice),
      commsPrice = Some(commsPrice),
      billingPeriod = Some("Month"),
      migrationExtraAttributes = None
    )

    // We now collect the arguments of Newspaper2026X.amendmentOrderPayload

    val orderDate = LocalDate.of(2026, 9, 1) // LocalDate.now()
    val accountNumber = subscription.accountNumber
    val subscriptionNumber = subscription.subscriptionNumber
    val effectDate = amendmentEffectiveDate
    val priceCap = 1.071 // 7.1 %

    assertEquals(
      Newspaper2026X.amendmentOrderPayload(
        cohortItem,
        orderDate,
        accountNumber,
        subscriptionNumber,
        effectDate,
        subscription,
        oldPrice,
        commsPrice,
        invoicePreview
      ),
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2026-09-01",
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
             |                            "triggerDate": "2026-11-01"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2026-11-01"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2026-11-01"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a128536993307dd01994bf7e8d2044b"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2026-11-01"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2026-11-01"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2026-11-01"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0ff56fe33f50157040bbdcf3ae4",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fc56fe26ba01570418eddd26e1",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 2.39
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f5015709cce7ad1aea",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.05
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f5015709c80af30495",
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
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f0015709cac4561bf3",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.05
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe270b015709cc16f92645",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.05
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe270b015709c90c291c49",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.05
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe26b6015709ca144a646a",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 9.05
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe26b60157042fcd462666",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 12.68
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
  test("Newspaper2026X.amendmentOrderPayload") {

    // sub5: "Newspaper Voucher"          "Weekend+"    "GBP"   "Month"

    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2026X/sub5/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2026X/sub5/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2026X/sub5/invoice-preview.json")

    val amendmentEffectiveDate = LocalDate.of(2026, 11, 17)
    val oldPrice = BigDecimal(27.99)
    val estimatedNewPrice = BigDecimal(29.99)
    val commsPrice = BigDecimal(29.99)

    val cohortItem = CohortItem(
      subscriptionName = subscription.subscriptionNumber,
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
      amendmentEffectiveDate = Some(amendmentEffectiveDate),
      currency = Some("GBP"),
      oldPrice = Some(oldPrice),
      estimatedNewPrice = Some(estimatedNewPrice),
      commsPrice = Some(commsPrice),
      billingPeriod = Some("Month"),
      migrationExtraAttributes = None
    )

    // We now collect the arguments of Newspaper2026X.amendmentOrderPayload

    val orderDate = LocalDate.of(2026, 9, 1) // LocalDate.now()
    val accountNumber = subscription.accountNumber
    val subscriptionNumber = subscription.subscriptionNumber
    val effectDate = amendmentEffectiveDate
    val priceCap = 1.071 // 7.1 %

    assertEquals(
      Newspaper2026X.amendmentOrderPayload(
        cohortItem,
        orderDate,
        accountNumber,
        subscriptionNumber,
        effectDate,
        subscription,
        oldPrice,
        commsPrice,
        invoicePreview
      ),
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2026-09-01",
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
             |                            "triggerDate": "2026-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2026-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2026-11-17"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a12817b9a43b21f019a496d92e16c52"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2026-11-17"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2026-11-17"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2026-11-17"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0fd56fe26b60157040cdd323f76",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fe56fe33ff015709bb986636d8",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 8.75
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
             |                                        "listPrice": 10.62
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe26b601570432f4e33d17",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 10.62
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
