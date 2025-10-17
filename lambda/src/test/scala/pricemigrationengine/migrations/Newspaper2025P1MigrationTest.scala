package pricemigrationengine.migrations

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.Fixtures

import java.time.LocalDate

// Subscription fixture: 276579
// 276579; Newspaper - Voucher Book; Sixday+; Month
// val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/276579/subscription.json")
// val account = Fixtures.accountFromJson("Migrations/Newspaper2025P1/276579/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/276579/invoice-preview.json")

// Subscription fixture: 280828
// 280828; Newspaper - Voucher Book; Everyday+; Quarter
// val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/280828/subscription.json")
// val account = Fixtures.accountFromJson("Migrations/Newspaper2025P1/280828/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/280828/invoice-preview.json")

// Subscription fixture: 296144 (comes as "productName": "Newspaper Digital Voucher") # anomaly
// 296144; Newspaper - Subscription Card; Sixday+; Month
// val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/296144/subscription.json")
// val account = Fixtures.accountFromJson("Migrations/Newspaper2025P1/296144/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/296144/invoice-preview.json")

// Subscription fixture: A-S00553498
// A-S00553498; Newspaper - Home Delivery; Everyday+; Month
// val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/A-S00553498/subscription.json")
// val account = Fixtures.accountFromJson("Migrations/Newspaper2025P1/A-S00553498/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/A-S00553498/invoice-preview.json")

class Newspaper2025P1MigrationTest extends munit.FunSuite {

  // val s = """{ "brandTitle": "the Guardian" }"""
  // val s = """{ "brandTitle": "the Guardian and the Observer" }"""
  // val s = """{ "brandTitle": "the Guardian", "removeDiscount": true }"""

  test("decoding") {
    val s = """{ "brandTitle": "Label 01" }"""
    val attribute: Newspaper2025P1ExtraAttributes = upickle.default.read[Newspaper2025P1ExtraAttributes](s)
    assertEquals(attribute, Newspaper2025P1ExtraAttributes("Label 01", None))
  }

  test("getLabelFromMigrationExtraAttributes (1)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01" }"""),
    )
    val label = Newspaper2025P1Migration.getLabelFromMigrationExtraAttributes(cohortItem)
    assertEquals(label, Some("Label 01"))
  }

  test("getLabelFromMigrationExtraAttributes (2)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01", "removeDiscount": true }"""),
    )
    val label = Newspaper2025P1Migration.getLabelFromMigrationExtraAttributes(cohortItem)
    assertEquals(label, Some("Label 01"))
  }

  test("decideShouldRemoveDiscount (2)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01" }"""),
    )
    val label = Newspaper2025P1Migration.decideShouldRemoveDiscount(cohortItem)
    assertEquals(label, false)
  }

  test("decideShouldRemoveDiscount (2)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01", "removeDiscount": true }"""),
    )
    val label = Newspaper2025P1Migration.decideShouldRemoveDiscount(cohortItem)
    assertEquals(label, true)
  }

  test("decideShouldRemoveDiscount (3)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01", "removeDiscount": false }"""),
    )
    val label = Newspaper2025P1Migration.decideShouldRemoveDiscount(cohortItem)
    assertEquals(label, false)
  }

  test("priceLookUp") {
    assertEquals(
      Newspaper2025P1Migration.priceLookUp(Newspaper2025P1Voucher, Newspaper2025P1EverydayPlus, Monthly),
      Some(BigDecimal(69.99))
    )

    assertEquals(
      Newspaper2025P1Migration.priceLookUp(Newspaper2025P1Voucher, Newspaper2025P1SixdayPlus, SemiAnnual),
      Some(BigDecimal(371.94))
    )

    assertEquals(
      Newspaper2025P1Migration.priceLookUp(Newspaper2025P1Subcard, Newspaper2025P1EverydayPlus, Quarterly),
      Some(BigDecimal(209.97))
    )

    assertEquals(
      Newspaper2025P1Migration.priceLookUp(Newspaper2025P1HomeDelivery, Newspaper2025P1SixdayPlus, Monthly),
      Some(BigDecimal(73.99))
    )

    // And we test an undefined combination
    assertEquals(
      Newspaper2025P1Migration.priceLookUp(Newspaper2025P1HomeDelivery, Newspaper2025P1SixdayPlus, SemiAnnual),
      None
    )
  }

  // decideProductType

  test("decideProductType (276579)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/276579/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/276579/invoice-preview.json")
    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get
    assertEquals(
      Newspaper2025P1Migration.decideProductType(ratePlan),
      Some(Newspaper2025P1Voucher)
    )
  }

  test("decideProductType (280828)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/280828/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/280828/invoice-preview.json")
    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get
    assertEquals(
      Newspaper2025P1Migration.decideProductType(ratePlan),
      Some(Newspaper2025P1Voucher)
    )
  }

  test("decideProductType (296144)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/296144/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/296144/invoice-preview.json")
    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get
    assertEquals(
      Newspaper2025P1Migration.decideProductType(ratePlan),
      Some(Newspaper2025P1Voucher)
    )
  }

  test("decideProductType (A-S00553498)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/A-S00553498/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/A-S00553498/invoice-preview.json")
    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get
    assertEquals(
      Newspaper2025P1Migration.decideProductType(ratePlan),
      Some(Newspaper2025P1HomeDelivery)
    )
  }

  // decidePlusType

  test("decidePlusType (276579)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/276579/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/276579/invoice-preview.json")
    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get
    assertEquals(
      Newspaper2025P1Migration.decidePlusType(ratePlan),
      Some(Newspaper2025P1SixdayPlus)
    )
  }

  test("decidePlusType (280828)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/280828/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/280828/invoice-preview.json")
    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get
    assertEquals(
      Newspaper2025P1Migration.decidePlusType(ratePlan),
      Some(Newspaper2025P1EverydayPlus)
    )
  }

  test("decidePlusType (296144)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/296144/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/296144/invoice-preview.json")
    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get
    assertEquals(
      Newspaper2025P1Migration.decidePlusType(ratePlan),
      Some(Newspaper2025P1SixdayPlus)
    )
  }

  test("decidePlusType (A-S00553498)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/A-S00553498/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/A-S00553498/invoice-preview.json")
    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get
    assertEquals(
      Newspaper2025P1Migration.decidePlusType(ratePlan),
      Some(Newspaper2025P1EverydayPlus)
    )
  }

  // subscriptionToLastPriceMigrationDate

  test("subscriptionToLastPriceMigrationDate (A-S00553498)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/A-S00553498/subscription.json")
    assertEquals(
      Newspaper2025P1Migration.subscriptionToLastPriceMigrationDate(subscription),
      Some(LocalDate.of(2024, 4, 6))
    )
  }

  // priceData

  test("priceData (276579)") {
    // Subscription fixture: 276579
    // 276579; Newspaper - Voucher Book; Sixday+; Month

    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/276579/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2025P1/276579/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/276579/invoice-preview.json")

    // Here we are testing priceData, but to be confident that the price data
    // is correct, we are going to test all the intermediary values of the
    // for yield construct.

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get

    assertEquals(
      ratePlan,
      ZuoraRatePlan(
        id = "8a12910195aa4f620195b0e2bc0c139d",
        productName = "Newspaper Voucher",
        productRatePlanId = "2c92a0fc56fe26ba0157040c5ea17f6a",
        ratePlanName = "Sixday+",
        ratePlanCharges = List(
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0ff56fe33f5015709cdedbd246b",
            name = "Tuesday",
            number = "C-05403277",
            currency = "GBP",
            price = Some(BigDecimal(8.96)),
            billingPeriod = Some("Month"),
            chargedThroughDate = Some(LocalDate.of(2025, 7, 8)),
            processedThroughDate = Some(LocalDate.of(2025, 6, 8)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 3, 29)),
            effectiveStartDate = Some(LocalDate.of(2024, 5, 8)),
            effectiveEndDate = Some(LocalDate.of(2025, 9, 8))
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0ff56fe33f3015709d10a436f52",
            name = "Digipack",
            number = "C-05403276",
            currency = "GBP",
            price = Some(BigDecimal(2.0)),
            billingPeriod = Some("Month"),
            chargedThroughDate = Some(LocalDate.of(2025, 7, 8)),
            processedThroughDate = Some(LocalDate.of(2025, 6, 8)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 3, 29)),
            effectiveStartDate = Some(LocalDate.of(2024, 5, 8)),
            effectiveEndDate = Some(LocalDate.of(2025, 9, 8))
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0fe56fe33ff015704325d87494c",
            name = "Monday",
            number = "C-05403275",
            currency = "GBP",
            price = Some(BigDecimal(8.96)),
            billingPeriod = Some("Month"),
            chargedThroughDate = Some(LocalDate.of(2025, 7, 8)),
            processedThroughDate = Some(LocalDate.of(2025, 6, 8)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 3, 29)),
            effectiveStartDate = Some(LocalDate.of(2024, 5, 8)),
            effectiveEndDate = Some(LocalDate.of(2025, 9, 8))
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0fd56fe26b6015709d078df4a80",
            name = "Saturday",
            number = "C-05403274",
            currency = "GBP",
            price = Some(BigDecimal(12.19)),
            billingPeriod = Some("Month"),
            chargedThroughDate = Some(LocalDate.of(2025, 7, 8)),
            processedThroughDate = Some(LocalDate.of(2025, 6, 8)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 3, 29)),
            effectiveStartDate = Some(LocalDate.of(2024, 5, 8)),
            effectiveEndDate = Some(LocalDate.of(2025, 9, 8))
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0fd56fe26b6015709cfc1500a2e",
            name = "Friday",
            number = "C-05403273",
            currency = "GBP",
            price = Some(BigDecimal(8.96)),
            billingPeriod = Some("Month"),
            chargedThroughDate = Some(LocalDate.of(2025, 7, 8)),
            processedThroughDate = Some(LocalDate.of(2025, 6, 8)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 3, 29)),
            effectiveStartDate = Some(LocalDate.of(2024, 5, 8)),
            effectiveEndDate = Some(LocalDate.of(2025, 9, 8))
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0fd56fe26b6015709ced61a032e",
            name = "Wednesday",
            number = "C-05403272",
            currency = "GBP",
            price = Some(BigDecimal(8.96)),
            billingPeriod = Some("Month"),
            chargedThroughDate = Some(LocalDate.of(2025, 7, 8)),
            processedThroughDate = Some(LocalDate.of(2025, 6, 8)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 3, 29)),
            effectiveStartDate = Some(LocalDate.of(2024, 5, 8)),
            effectiveEndDate = Some(LocalDate.of(2025, 9, 8))
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0fc56fe26ba015709cf4bbd3d1c",
            name = "Thursday",
            number = "C-05403271",
            currency = "GBP",
            price = Some(BigDecimal(8.96)),
            billingPeriod = Some("Month"),
            chargedThroughDate = Some(LocalDate.of(2025, 7, 8)),
            processedThroughDate = Some(LocalDate.of(2025, 6, 8)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 3, 29)),
            effectiveStartDate = Some(LocalDate.of(2024, 5, 8)),
            effectiveEndDate = Some(LocalDate.of(2025, 9, 8))
          )
        ),
        lastChangeType = Some("Add")
      )
    )

    val productType = Newspaper2025P1Migration.decideProductType(ratePlan).get

    assertEquals(
      productType,
      Newspaper2025P1Voucher
    )

    val plusType = Newspaper2025P1Migration.decidePlusType(ratePlan).get

    assertEquals(
      plusType,
      Newspaper2025P1SixdayPlus
    )

    val priceData = Newspaper2025P1Migration.priceData(
      subscription,
      invoicePreview,
      account
    )

    assertEquals(
      priceData,
      Right(PriceData("GBP", BigDecimal(58.99), BigDecimal(61.99), "Month"))
    )
  }

  test("priceData (280828)") {
    // Subscription fixture: 276579
    // 280828; Newspaper - Voucher Book; Everyday+; Quarter

    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/280828/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2025P1/280828/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/280828/invoice-preview.json")

    // Here we check the rate plan product name and rate plan name
    // and then we perform the main test Newspaper2025P1Migration.priceData

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get

    assertEquals(
      ratePlan.productName,
      "Newspaper Voucher"
    )

    assertEquals(
      ratePlan.ratePlanName,
      "Everyday+"
    )

    val priceData = Newspaper2025P1Migration.priceData(
      subscription,
      invoicePreview,
      account
    )

    assertEquals(
      priceData,
      Right(PriceData("GBP", BigDecimal(200.97), BigDecimal(209.97), "Quarter"))
    )
  }

  test("priceData (296144)") {
    // Subscription fixture: 296144 (comes as "productName": "Newspaper Digital Voucher") # anomaly
    // 296144; Newspaper - Subscription Card; Sixday+; Month

    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/296144/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2025P1/296144/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/296144/invoice-preview.json")

    // Here we check the rate plan product name and rate plan name
    // and then we perform the main test Newspaper2025P1Migration.priceData

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get

    assertEquals(
      ratePlan.productName,
      "Newspaper Digital Voucher"
    )

    assertEquals(
      ratePlan.ratePlanName,
      "Sixday+"
    )

    val priceData = Newspaper2025P1Migration.priceData(
      subscription,
      invoicePreview,
      account
    )

    assertEquals(
      priceData,
      Right(PriceData("GBP", BigDecimal(58.99), BigDecimal(61.99), "Month"))
    )
  }

  test("priceData (A-S00553498)") {
    // Subscription fixture: A-S00553498
    // A-S00553498; Newspaper - Home Delivery; Everyday+; Month

    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/A-S00553498/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2025P1/A-S00553498/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/A-S00553498/invoice-preview.json")

    // Here we check the rate plan product name and rate plan name
    // and then we perform the main test Newspaper2025P1Migration.priceData

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get

    assertEquals(
      ratePlan.productName,
      "Newspaper Delivery"
    )

    assertEquals(
      ratePlan.ratePlanName,
      "Everyday+"
    )

    val priceData = Newspaper2025P1Migration.priceData(
      subscription,
      invoicePreview,
      account
    )

    assertEquals(
      priceData,
      Right(PriceData("GBP", BigDecimal(80.99), BigDecimal(83.99), "Month"))
    )
  }

  // The following subscription is interesting.
  // It moves from ReadyForEstimation to EstimationFailed in AWS, and only AWS,
  // without any indication of what the cause might be.
  // It's the only subscription of Newspaper2025P1 with that behavior 🤔

  // Subscription fixture: 344070
  // val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/344070-EstimationFailed/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/Newspaper2025P1/344070-EstimationFailed/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/344070-EstimationFailed/invoice-preview.json")

  test("priceData (344070-EstimationFailed)") {
    // Subscription fixture: 344070-EstimationFailed

    // The `EstimationFailed` processing stage has been decommissioned

    val subscription =
      Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/344070-EstimationFailed/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2025P1/344070-EstimationFailed/account.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/344070-EstimationFailed/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get

    assertEquals(
      ratePlan.productName,
      "Newspaper Voucher"
    )

    assertEquals(
      ratePlan.ratePlanName,
      "Sixday+"
    )

    val priceData = Newspaper2025P1Migration.priceData(
      subscription,
      invoicePreview,
      account
    )

    assertEquals(
      priceData,
      Right(PriceData("GBP", BigDecimal(58.99), BigDecimal(61.99), "Month"))
    )
  }

  test("Newspaper2025P1Migration.amendmentOrderPayload (276579)") {

    // We start with the same subscription that we started priceData with. The one with the fully
    // detailed ratePlan (that we also used for ZuoraOrdersApiPrimitives.ratePlanChargesToChargeOverrides)

    // Subscription fixture: 276579
    // 276579; Newspaper - Voucher Book; Sixday+; Month

    val subscription = Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/276579/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2025P1/276579/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/276579/invoice-preview.json")

    val startDate = LocalDate.of(2025, 8, 5)
    val oldPrice = BigDecimal(67.5)
    val estimatedNewPrice = BigDecimal(87.0)
    val commsPrice = estimatedNewPrice

    val cohortItem = CohortItem(
      subscriptionName = subscription.subscriptionNumber,
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
      amendmentEffectiveDate = Some(startDate),
      currency = Some("EUR"),
      oldPrice = Some(oldPrice),
      commsPrice = Some(commsPrice),
      estimatedNewPrice = Some(estimatedNewPrice),
      billingPeriod = Some("Quarter"),
      migrationExtraAttributes = None
    )

    // We now collect the arguments of Newspaper2025P1Migration.amendmentOrderPayload

    val orderDate = LocalDate.of(2025, 7, 16) // LocalDate.now()
    val accountNumber = subscription.accountNumber
    val subscriptionNumber = subscription.subscriptionNumber
    val effectDate = startDate
    val priceCap = 1.2

    assertEquals(
      Newspaper2025P1Migration.amendmentOrderPayload(
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
             |    "orderDate": "2025-07-16",
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
             |                        "ratePlanId": "8a12910195aa4f620195b0e2bc0c139d"
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
             |                        "productRatePlanId": "2c92a0fc56fe26ba0157040c5ea17f6a",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f5015709cdedbd246b",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 22.56
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f3015709d10a436f52",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 2.57
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fe56fe33ff015704325d87494c",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 11.54
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe26b6015709d078df4a80",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 15.71
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe26b6015709cfc1500a2e",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 11.54
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fd56fe26b6015709ced61a032e",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 11.54
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fc56fe26ba015709cf4bbd3d1c",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 11.54
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

  test("Newspaper2025P1Migration.amendmentOrderPayload (277526-Discounts-Adjustment) [with discount removal]") {

    // Here we use 277526-Discounts-Adjustment to test the discount removal variant of Newspaper2025P1Migration.amendmentOrderPayload

    // To trigger that we just need to set the cohort item extra attributes with
    // a "removeDiscount": true

    val subscription =
      Fixtures.subscriptionFromJson("Migrations/Newspaper2025P1/277526-Discounts-Adjustment/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/Newspaper2025P1/277526-Discounts-Adjustment/account.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("Migrations/Newspaper2025P1/277526-Discounts-Adjustment/invoice-preview.json")

    val startDate = LocalDate.of(2025, 8, 5)
    val oldPrice = BigDecimal(67.5)
    val estimatedNewPrice = BigDecimal(87.0)
    val commsPrice = estimatedNewPrice

    val cohortItem = CohortItem(
      subscriptionName = subscription.subscriptionNumber,
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
      amendmentEffectiveDate = Some(startDate),
      currency = Some("EUR"),
      oldPrice = Some(oldPrice),
      commsPrice = Some(commsPrice),
      estimatedNewPrice = Some(estimatedNewPrice),
      billingPeriod = Some("Quarter"),
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01", "removeDiscount": true }""")
    )

    // We now collect the arguments of Newspaper2025P1Migration.amendmentOrderPayload

    val orderDate = LocalDate.of(2025, 7, 16) // LocalDate.now()
    val accountNumber = subscription.accountNumber
    val subscriptionNumber = subscription.subscriptionNumber
    val effectDate = startDate
    val priceCap = 1.2

    assertEquals(
      Newspaper2025P1Migration.amendmentOrderPayload(
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
             |    "orderDate": "2025-07-16",
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
             |                        "ratePlanId": "8a1281f09187876601918cb7010d0274"
             |                    }
             |                },
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
             |                        "ratePlanId": "8a1281f09187876601918cb7011c02dd"
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
             |                        "productRatePlanId": "2c92a0ff56fe33f50157040bbdcf3ae4",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "2c92a0ff56fe33f5015709cce7ad1aea",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 11.54
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
             |                                        "listPrice": 14.75
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
             |                                        "listPrice": 10.85
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
             |                                        "listPrice": 10.85
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
             |                                        "listPrice": 10.85
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
             |                                        "listPrice": 10.85
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
             |                                        "listPrice": 14.74
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "2c92a0fc56fe26ba01570418eddd26e1",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 2.57
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
