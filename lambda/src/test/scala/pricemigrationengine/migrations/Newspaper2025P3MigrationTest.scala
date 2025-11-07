package pricemigrationengine.migrations

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.Fixtures

import java.time.LocalDate

class Newspaper2025P3MigrationTest extends munit.FunSuite {

  // val s = """{ "brandTitle": "the Guardian" }"""
  // val s = """{ "brandTitle": "the Guardian and the Observer" }"""
  // val s = """{ "brandTitle": "the Guardian", "removeDiscount": true }"""

  test("decoding") {
    val s = """{ "brandTitle": "Label 01" }"""
    val attribute: Newspaper2025P3ExtraAttributes = upickle.default.read[Newspaper2025P3ExtraAttributes](s)
    assertEquals(attribute, Newspaper2025P3ExtraAttributes("Label 01", None))
  }

  test("getLabelFromMigrationExtraAttributes (1)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01" }"""),
    )
    val label = Newspaper2025P3Migration.getLabelFromMigrationExtraAttributes(cohortItem)
    assertEquals(label, Some("Label 01"))
  }

  test("getLabelFromMigrationExtraAttributes (2)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01", "removeDiscount": true }"""),
    )
    val label = Newspaper2025P3Migration.getLabelFromMigrationExtraAttributes(cohortItem)
    assertEquals(label, Some("Label 01"))
  }

  test("decideShouldRemoveDiscount (1)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01" }"""),
    )
    val flag = Newspaper2025P3Migration.decideShouldRemoveDiscount(cohortItem)
    assertEquals(flag, false)
  }

  test("decideShouldRemoveDiscount (2)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01", "removeDiscount": true }"""),
    )
    val flag = Newspaper2025P3Migration.decideShouldRemoveDiscount(cohortItem)
    assertEquals(flag, true)
  }

  test("decideShouldRemoveDiscount (3)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01", "removeDiscount": false }"""),
    )
    val flag = Newspaper2025P3Migration.decideShouldRemoveDiscount(cohortItem)
    assertEquals(flag, false)
  }

  test("getEarliestMigrationDateFromMigrationExtraAttributes (1)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01" }"""),
    )
    val date = Newspaper2025P3Migration.getEarliestMigrationDateFromMigrationExtraAttributes(cohortItem)
    assertEquals(date, None)
  }

  test("getEarliestMigrationDateFromMigrationExtraAttributes (2)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01", "earliestMigrationDate": "2025-10-06" }"""),
    )
    val date = Newspaper2025P3Migration.getEarliestMigrationDateFromMigrationExtraAttributes(cohortItem)
    assertEquals(date, Some(LocalDate.of(2025, 10, 6)))
  }

  test("getEarliestMigrationDateFromMigrationExtraAttributes (3)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes =
        Some("""{ "brandTitle": "Label 01", "removeDiscount": false, "earliestMigrationDate": "2025-10-06" }"""),
    )
    val date = Newspaper2025P3Migration.getEarliestMigrationDateFromMigrationExtraAttributes(cohortItem)
    assertEquals(date, Some(LocalDate.of(2025, 10, 6)))
  }

  test("priceLookUp") {
    assertEquals(
      Newspaper2025P3Migration.priceLookUp(Newspaper2025P3Everyday, Monthly),
      Some(BigDecimal(69.99))
    )

    assertEquals(
      Newspaper2025P3Migration.priceLookUp(Newspaper2025P3Sixday, SemiAnnual),
      Some(BigDecimal(371.94))
    )

    assertEquals(
      Newspaper2025P3Migration.priceLookUp(Newspaper2025P3Weekend, Quarterly),
      Some(BigDecimal(83.97))
    )

    assertEquals(
      Newspaper2025P3Migration.priceLookUp(Newspaper2025P3Saturday, Monthly),
      Some(BigDecimal(15.99))
    )
  }

  // --------------------------------------------------------------------
  // Fixtures:

  // 277291-everyday-annual
  // val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/invoice-preview.json")

  // 277750-everyday-month
  // val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/277750-everyday-month/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/277750-everyday-month/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/277750-everyday-month/invoice-preview.json")

  // 412032-sixday-annual
  // val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/412032-sixday-annual/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/412032-sixday-annual/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/412032-sixday-annual/invoice-preview.json")

  // A-S02075439-saturday-month
  // val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/A-S02075439-saturday-month/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/A-S02075439-saturday-month/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/A-S02075439-saturday-month/invoice-preview.json")

  // --------------------------------------------------------------------
  // Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate

  test("Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate (277291-everyday-annual)") {
    // 277291-everyday-annual
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/invoice-preview.json")

    val today = LocalDate.of(2023, 1, 1)

    assertEquals(
      Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate(subscription, today),
      Some(LocalDate.of(2024, 11, 27))
    )
  }

  test("Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate (412032-sixday-annual)") {
    // 412032-sixday-annual
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/412032-sixday-annual/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/412032-sixday-annual/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/412032-sixday-annual/invoice-preview.json")

    val today = LocalDate.of(2023, 1, 1)

    assertEquals(
      Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate(subscription, today),
      Some(LocalDate.of(2024, 8, 12))
    )
  }

  // --------------------------------------------------------------------
  // Newspaper2025P3Migration.decideDeliveryPattern

  test("Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate (277291-everyday-annual)") {
    // 277291-everyday-annual
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/invoice-preview.json")

    val today = LocalDate.of(2023, 1, 1)

    val ratePlan =
      SI2025RateplanFromSub
        .uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(subscription: ZuoraSubscription, today)
        .get

    assertEquals(
      Newspaper2025P3Migration.decideDeliveryPattern(ratePlan),
      Some(Newspaper2025P3Everyday)
    )
  }

  test("Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate (277750-everyday-month)") {
    // 277750-everyday-month
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/277750-everyday-month/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/277750-everyday-month/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/277750-everyday-month/invoice-preview.json")

    val today = LocalDate.of(2023, 1, 1)

    val ratePlan =
      SI2025RateplanFromSub
        .uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(subscription: ZuoraSubscription, today)
        .get

    assertEquals(
      Newspaper2025P3Migration.decideDeliveryPattern(ratePlan),
      Some(Newspaper2025P3Everyday)
    )
  }

  test("Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate (412032-sixday-annual)") {
    // 412032-sixday-annual
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/412032-sixday-annual/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/412032-sixday-annual/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/412032-sixday-annual/invoice-preview.json")

    val today = LocalDate.of(2023, 1, 1)

    val ratePlan =
      SI2025RateplanFromSub
        .uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(subscription: ZuoraSubscription, today)
        .get

    assertEquals(
      Newspaper2025P3Migration.decideDeliveryPattern(ratePlan),
      Some(Newspaper2025P3Sixday)
    )
  }

  test("Newspaper2025P3Migration.subscriptionToLastPriceMigrationDate (A-S02075439-saturday-month)") {
    // A-S02075439-saturday-month
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/A-S02075439-saturday-month/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/A-S02075439-saturday-month/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/A-S02075439-saturday-month/invoice-preview.json")

    val today = LocalDate.of(2023, 1, 1)

    val ratePlan =
      SI2025RateplanFromSub
        .uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(subscription: ZuoraSubscription, today)
        .get

    assertEquals(
      Newspaper2025P3Migration.decideDeliveryPattern(ratePlan),
      Some(Newspaper2025P3Saturday)
    )
  }

  // --------------------------------------------------------------------
  // Newspaper2025P3Migration.priceData

  test("Newspaper2025P3Migration.priceData") {
    // 277291-everyday-annual
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/account.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/invoice-preview.json")

    // Here we are testing priceData, but to be confident that the price data
    // is correct, we are going to test all the intermediary values of the
    // for yield construct.

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan(subscription, invoicePreview).get

    assertEquals(
      ratePlan,
      ZuoraRatePlan(
        id = "8a128da59348e4fd01936c6ed8466963",
        productName = "Newspaper Voucher",
        productRatePlanId = "2c92a0fd56fe270b0157040dd79b35da",
        ratePlanName = "Everyday",
        ratePlanCharges = List(
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0ff56fe33f5015709c39719783e",
            name = "Sunday",
            number = "C-06169227",
            currency = "GBP",
            price = Some(BigDecimal(137.4)),
            billingPeriod = Some("Annual"),
            chargedThroughDate = Some(LocalDate.of(2026, 1, 6)),
            processedThroughDate = Some(LocalDate.of(2025, 1, 6)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 11, 27)),
            effectiveStartDate = Some(LocalDate.of(2025, 1, 6)),
            effectiveEndDate = Some(LocalDate.of(2026, 1, 6))
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0ff56fe33f3015709c110a71630",
            name = "Wednesday",
            number = "C-06169226",
            currency = "GBP",
            price = Some(BigDecimal(101.04)),
            billingPeriod = Some("Annual"),
            chargedThroughDate = Some(LocalDate.of(2026, 1, 6)),
            processedThroughDate = Some(LocalDate.of(2025, 1, 6)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 11, 27)),
            effectiveStartDate = Some(LocalDate.of(2025, 1, 6)),
            effectiveEndDate = Some(LocalDate.of(2026, 1, 6))
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0ff56fe33f0015709c215527db4",
            name = "Friday",
            number = "C-06169225",
            currency = "GBP",
            price = Some(BigDecimal(101.04)),
            billingPeriod = Some("Annual"),
            chargedThroughDate = Some(LocalDate.of(2026, 1, 6)),
            processedThroughDate = Some(LocalDate.of(2025, 1, 6)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 11, 27)),
            effectiveStartDate = Some(LocalDate.of(2025, 1, 6)),
            effectiveEndDate = Some(LocalDate.of(2026, 1, 6))
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0ff56fe33f0015709c182cb7c82",
            name = "Thursday",
            number = "C-06169224",
            currency = "GBP",
            price = Some(BigDecimal(101.04)),
            billingPeriod = Some("Annual"),
            chargedThroughDate = Some(LocalDate.of(2026, 1, 6)),
            processedThroughDate = Some(LocalDate.of(2025, 1, 6)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 11, 27)),
            effectiveStartDate = Some(LocalDate.of(2025, 1, 6)),
            effectiveEndDate = Some(LocalDate.of(2026, 1, 6))
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0fd56fe270b015709c320ee0595",
            name = "Saturday",
            number = "C-06169223",
            currency = "GBP",
            price = Some(BigDecimal(137.28)),
            billingPeriod = Some("Annual"),
            chargedThroughDate = Some(LocalDate.of(2026, 1, 6)),
            processedThroughDate = Some(LocalDate.of(2025, 1, 6)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 11, 27)),
            effectiveStartDate = Some(LocalDate.of(2025, 1, 6)),
            effectiveEndDate = Some(LocalDate.of(2026, 1, 6))
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0fd56fe26b6015709c0613b44a6",
            name = "Tuesday",
            number = "C-06169222",
            currency = "GBP",
            price = Some(BigDecimal(101.04)),
            billingPeriod = Some("Annual"),
            chargedThroughDate = Some(LocalDate.of(2026, 1, 6)),
            processedThroughDate = Some(LocalDate.of(2025, 1, 6)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 11, 27)),
            effectiveStartDate = Some(LocalDate.of(2025, 1, 6)),
            effectiveEndDate = Some(LocalDate.of(2026, 1, 6))
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0fd56fe26b601570431a5bc5a34",
            name = "Monday",
            number = "C-06169221",
            currency = "GBP",
            price = Some(BigDecimal(101.04)),
            billingPeriod = Some("Annual"),
            chargedThroughDate = Some(LocalDate.of(2026, 1, 6)),
            processedThroughDate = Some(LocalDate.of(2025, 1, 6)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 11, 27)),
            effectiveStartDate = Some(LocalDate.of(2025, 1, 6)),
            effectiveEndDate = Some(LocalDate.of(2026, 1, 6))
          )
        ),
        lastChangeType = Some("Add")
      )
    )

    val deliveryPattern = Newspaper2025P3Migration.decideDeliveryPattern(ratePlan).get

    assertEquals(
      deliveryPattern,
      Newspaper2025P3Everyday
    )

    val priceData = Newspaper2025P3Migration.priceData(
      subscription,
      invoicePreview,
      account
    )

    assertEquals(
      priceData,
      Right(PriceData("GBP", BigDecimal(779.88), BigDecimal(839.88), "Annual"))
    )
  }

  // --------------------------------------------------------------------
  // Newspaper2025P3Migration.amendmentOrderPayload

  test("Newspaper2025P3Migration.amendmentOrderPayload (276579)") {

    // Newspaper2025P3Migration.amendmentOrderPayload without discount removal

    // 277291-everyday-annual
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/account.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("Migrations/Newspaper2025P3/277291-everyday-annual/invoice-preview.json")

    val startDate = LocalDate.of(2025, 8, 23)
    val oldPrice = BigDecimal(779.88) // using the same number from the Newspaper2025P3Migration.priceData check
    val estimatedNewPrice = BigDecimal(839.88)
    val commsPrice = BigDecimal(839.88)

    val cohortItem = CohortItem(
      subscriptionName = subscription.subscriptionNumber,
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
      amendmentEffectiveDate = Some(startDate),
      currency = Some("GBP"),
      oldPrice = Some(oldPrice),
      estimatedNewPrice = Some(estimatedNewPrice),
      commsPrice = Some(commsPrice),
      billingPeriod = Some("Annual"),
      migrationExtraAttributes = None
    )

    // We now collect the arguments of Newspaper2025P3Migration.amendmentOrderPayload

    val orderDate = LocalDate.of(2025, 7, 20) // LocalDate.now()
    val accountNumber = subscription.accountNumber
    val subscriptionNumber = subscription.subscriptionNumber
    val effectDate = startDate
    val priceCap = 1.2

    assertEquals(
      Newspaper2025P3Migration.amendmentOrderPayload(
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
             |    "orderDate": "2025-07-20",
             |    "existingAccountNumber": "SUBSCRIPTION-NUMBER",
             |    "subscriptions": [
             |        {
             |            "subscriptionNumber": "SUBSCRIPTION-NUMBER",
             |            "orderActions": [
             |                {
             |                    "type": "RemoveProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-08-23"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-08-23"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-08-23"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a128da59348e4fd01936c6ed8466963"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2025-08-23"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2025-08-23"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2025-08-23"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "2c92a0fd56fe270b0157040dd79b35da",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f5015709c39719783e",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 147.99
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Annual"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f3015709c110a71630",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 108.81
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Annual"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f0015709c215527db4",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 108.81
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Annual"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f0015709c182cb7c82",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 108.81
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Annual"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe270b015709c320ee0595",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 147.84
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Annual"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe26b6015709c0613b44a6",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 108.81
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Annual"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe26b601570431a5bc5a34",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 108.81
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
