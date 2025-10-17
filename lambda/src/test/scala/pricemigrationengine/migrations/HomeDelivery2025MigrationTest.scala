package pricemigrationengine.migrations

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.Fixtures

import java.time.LocalDate

class HomeDelivery2025MigrationTest extends munit.FunSuite {

  test("decoding") {
    val s = """{ "brandTitle": "Label 01" }"""
    val attribute: Newspaper2025P1ExtraAttributes = upickle.default.read[Newspaper2025P1ExtraAttributes](s)
    assertEquals(attribute, Newspaper2025P1ExtraAttributes("Label 01"))
  }

  test("getLabelFromMigrationExtraAttributes (1)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01" }"""),
    )
    val label = HomeDelivery2025Migration.getLabelFromMigrationExtraAttributes(cohortItem)
    assertEquals(label, Some("Label 01"))
  }

  test("getLabelFromMigrationExtraAttributes (2)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01", "removeDiscount": true }"""),
    )
    val label = HomeDelivery2025Migration.getLabelFromMigrationExtraAttributes(cohortItem)
    assertEquals(label, Some("Label 01"))
  }

  test("decideShouldRemoveDiscount (1)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "Label 01" }"""),
    )
    val label = HomeDelivery2025Migration.decideShouldRemoveDiscount(cohortItem)
    assertEquals(label, false)
  }

  test("decideShouldRemoveDiscount (2)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "the Guardian", "removeDiscount": true }"""),
    )
    val label = HomeDelivery2025Migration.decideShouldRemoveDiscount(cohortItem)
    assertEquals(label, true)
  }

  test("decideShouldRemoveDiscount (3)") {
    val cohortItem = CohortItem(
      subscriptionName = "A-000001",
      processingStage = ReadyForEstimation,
      migrationExtraAttributes = Some("""{ "brandTitle": "the Guardian", "removeDiscount": false }"""),
    )
    val label = HomeDelivery2025Migration.decideShouldRemoveDiscount(cohortItem)
    assertEquals(label, false)
  }

  test("priceLookUp") {
    assertEquals(
      HomeDelivery2025Migration.priceLookUp(HomeDelivery2025Everyday, Monthly),
      Some(BigDecimal(83.99))
    )

    assertEquals(
      HomeDelivery2025Migration.priceLookUp(HomeDelivery2025Sixday, SemiAnnual),
      Some(BigDecimal(443.94))
    )

    assertEquals(
      HomeDelivery2025Migration.priceLookUp(HomeDelivery2025Weekend, Quarterly),
      Some(BigDecimal(104.97))
    )

    assertEquals(
      HomeDelivery2025Migration.priceLookUp(HomeDelivery2025Saturday, Monthly),
      Some(BigDecimal(20.99))
    )

    assertEquals(
      HomeDelivery2025Migration.priceLookUp(HomeDelivery2025Saturday, SemiAnnual),
      Some(BigDecimal(125.94))
    )
  }

  // ---------------------------------------------------
  // A-S00252266
  // Newspaper - Home Delivery
  // Sixday
  // Month
  // val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/A-S00252266/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/HomeDelivery2025/A-S00252266/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/A-S00252266/invoice-preview.json")

  // ---------------------------------------------------
  // A-S00256852
  // Newspaper - Home Delivery
  // Weekend
  // Month
  // val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/A-S00256852/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/HomeDelivery2025/A-S00256852/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/A-S00256852/invoice-preview.json")

  // ---------------------------------------------------
  // GA0000464
  // Newspaper - Home Delivery
  // Saturday
  // Quarter
  // val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/GA0000464/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/HomeDelivery2025/GA0000464/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/GA0000464/invoice-preview.json")

  // ---------------------------------------------------
  // GA0004508
  // Newspaper - Home Delivery
  // Weekend
  // Quarter
  // val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/GA0004508/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/HomeDelivery2025/GA0004508/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/GA0004508/invoice-preview.json")

  // ---------------------------------------------------
  // GA0006731
  // Newspaper - Home Delivery
  // Saturday
  // Month
  // val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/GA0006731/subscription.json")
  // val account = Fixtures.accountFromJson("Migrations/HomeDelivery2025/GA0006731/account.json")
  // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/GA0006731/invoice-preview.json")

  // decideDeliveryPattern

  test("decideDeliveryPattern (A-S00252266)") {
    // A-S00252266
    // Newspaper - Home Delivery
    // Sixday
    // Month
    val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/A-S00252266/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/A-S00252266/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get
    assertEquals(
      HomeDelivery2025Migration.decideDeliveryPattern(ratePlan),
      Some(HomeDelivery2025Sixday)
    )
  }

  test("decideDeliveryPattern (A-S00256852)") {
    // A-S00256852
    // Newspaper - Home Delivery
    // Weekend
    // Month
    val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/A-S00256852/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/A-S00256852/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get
    assertEquals(
      HomeDelivery2025Migration.decideDeliveryPattern(ratePlan),
      Some(HomeDelivery2025Weekend)
    )
  }

  test("decideDeliveryPattern (GA0000464)") {
    // GA0000464
    // Newspaper - Home Delivery
    // Saturday
    // Quarter
    val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/GA0000464/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/GA0000464/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get
    assertEquals(
      HomeDelivery2025Migration.decideDeliveryPattern(ratePlan),
      Some(HomeDelivery2025Saturday)
    )
  }

  test("decideDeliveryPattern (GA0004508)") {
    // GA0004508
    // Newspaper - Home Delivery
    // Weekend
    // Quarter
    val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/GA0004508/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/GA0004508/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get
    assertEquals(
      HomeDelivery2025Migration.decideDeliveryPattern(ratePlan),
      Some(HomeDelivery2025Weekend)
    )
  }

  test("decideDeliveryPattern (GA0006731)") {
    // GA0006731
    // Newspaper - Home Delivery
    // Saturday
    // Month
    val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/GA0006731/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/GA0006731/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get
    assertEquals(
      HomeDelivery2025Migration.decideDeliveryPattern(ratePlan),
      Some(HomeDelivery2025Saturday)
    )
  }

  // priceData

  test("priceData (A-S00252266)") {
    // A-S00252266
    // Newspaper - Home Delivery
    // Sixday
    // Month
    val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/A-S00252266/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/HomeDelivery2025/A-S00252266/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/A-S00252266/invoice-preview.json")

    // Here we are testing priceData, but to be confident that the price data
    // is correct, we are going to test all the intermediary values of the
    // for yield construct.

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get

    assertEquals(
      ratePlan,
      ZuoraRatePlan(
        id = "8a1295df970ba0430197195b9e3e50a6",
        productName = "Newspaper Delivery",
        productRatePlanId = "2c92a0ff560d311b0156136f2afe5315",
        ratePlanName = "Sixday",
        ratePlanCharges = List(
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0ff560d311b0156136f2c43533f",
            name = "Saturday",
            number = "C-05166360",
            currency = "GBP",
            price = Some(BigDecimal(14.74)),
            billingPeriod = Some("Month"),
            chargedThroughDate = Some(LocalDate.of(2025, 7, 30)),
            processedThroughDate = Some(LocalDate.of(2025, 6, 30)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 2, 20)),
            effectiveStartDate = Some(LocalDate.of(2024, 3, 30)),
            effectiveEndDate = Some(LocalDate.of(2025, 9, 30))
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0ff560d311b0156136f2c015337",
            name = "Tuesday",
            number = "C-05166359",
            currency = "GBP",
            price = Some(BigDecimal(10.85)),
            billingPeriod = Some("Month"),
            chargedThroughDate = Some(LocalDate.of(2025, 7, 30)),
            processedThroughDate = Some(LocalDate.of(2025, 6, 30)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 2, 20)),
            effectiveStartDate = Some(LocalDate.of(2024, 3, 30)),
            effectiveEndDate = Some(LocalDate.of(2025, 9, 30))
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0ff560d311b0156136f2bc2532f",
            name = "Monday",
            number = "C-05166358",
            currency = "GBP",
            price = Some(BigDecimal(10.85)),
            billingPeriod = Some("Month"),
            chargedThroughDate = Some(LocalDate.of(2025, 7, 30)),
            processedThroughDate = Some(LocalDate.of(2025, 6, 30)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 2, 20)),
            effectiveStartDate = Some(LocalDate.of(2024, 3, 30)),
            effectiveEndDate = Some(LocalDate.of(2025, 9, 30))
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0ff560d311b0156136f2b8c5327",
            name = "Thursday",
            number = "C-05166357",
            currency = "GBP",
            price = Some(BigDecimal(10.85)),
            billingPeriod = Some("Month"),
            chargedThroughDate = Some(LocalDate.of(2025, 7, 30)),
            processedThroughDate = Some(LocalDate.of(2025, 6, 30)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 2, 20)),
            effectiveStartDate = Some(LocalDate.of(2024, 3, 30)),
            effectiveEndDate = Some(LocalDate.of(2025, 9, 30))
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0ff560d311b0156136f2b50531f",
            name = "Friday",
            number = "C-05166356",
            currency = "GBP",
            price = Some(BigDecimal(10.85)),
            billingPeriod = Some("Month"),
            chargedThroughDate = Some(LocalDate.of(2025, 7, 30)),
            processedThroughDate = Some(LocalDate.of(2025, 6, 30)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 2, 20)),
            effectiveStartDate = Some(LocalDate.of(2024, 3, 30)),
            effectiveEndDate = Some(LocalDate.of(2025, 9, 30))
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "2c92a0ff560d311b0156136f2b185317",
            name = "Wednesday",
            number = "C-05166355",
            currency = "GBP",
            price = Some(BigDecimal(10.85)),
            billingPeriod = Some("Month"),
            chargedThroughDate = Some(LocalDate.of(2025, 7, 30)),
            processedThroughDate = Some(LocalDate.of(2025, 6, 30)),
            specificBillingPeriod = None,
            endDateCondition = Some("Subscription_End"),
            upToPeriodsType = None,
            upToPeriods = None,
            billingDay = Some("ChargeTriggerDay"),
            triggerEvent = Some("CustomerAcceptance"),
            triggerDate = None,
            discountPercentage = None,
            originalOrderDate = Some(LocalDate.of(2024, 2, 20)),
            effectiveStartDate = Some(LocalDate.of(2024, 3, 30)),
            effectiveEndDate = Some(LocalDate.of(2025, 9, 30))
          )
        ),
        lastChangeType = Some("Add")
      )
    )

    val deliveryPattern = HomeDelivery2025Migration.decideDeliveryPattern(ratePlan).get

    assertEquals(
      deliveryPattern,
      HomeDelivery2025Sixday
    )

    val priceData = HomeDelivery2025Migration.priceData(
      subscription,
      invoicePreview,
      account
    )

    assertEquals(
      priceData,
      Right(PriceData("GBP", BigDecimal(68.99), BigDecimal(73.99), "Month"))
    )
  }

  test("priceData (A-S00256852)") {
    // A-S00256852
    // Newspaper - Home Delivery
    // Weekend
    // Month
    val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/A-S00256852/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/HomeDelivery2025/A-S00256852/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/A-S00256852/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get
    val deliveryPattern = HomeDelivery2025Migration.decideDeliveryPattern(ratePlan).get

    assertEquals(
      deliveryPattern,
      HomeDelivery2025Weekend
    )

    val priceData = HomeDelivery2025Migration.priceData(
      subscription,
      invoicePreview,
      account
    )

    assertEquals(
      priceData,
      Right(PriceData("GBP", BigDecimal(31.99), BigDecimal(34.99), "Month"))
    )
  }

  test("priceData (GA0000464)") {
    // GA0000464
    // Newspaper - Home Delivery
    // Saturday
    // Quarter
    val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/GA0000464/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/HomeDelivery2025/GA0000464/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/GA0000464/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get
    val deliveryPattern = HomeDelivery2025Migration.decideDeliveryPattern(ratePlan).get

    assertEquals(
      deliveryPattern,
      HomeDelivery2025Saturday
    )

    val priceData = HomeDelivery2025Migration.priceData(
      subscription,
      invoicePreview,
      account
    )

    assertEquals(
      priceData,
      Right(PriceData("GBP", BigDecimal(59.97), BigDecimal(62.97), "Quarter"))
    )
  }

  test("priceData (GA0004508)") {
    // GA0004508
    // Newspaper - Home Delivery
    // Weekend
    // Quarter
    val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/GA0004508/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/HomeDelivery2025/GA0004508/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/GA0004508/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get
    val deliveryPattern = HomeDelivery2025Migration.decideDeliveryPattern(ratePlan).get

    assertEquals(
      deliveryPattern,
      HomeDelivery2025Weekend
    )

    val priceData = HomeDelivery2025Migration.priceData(
      subscription,
      invoicePreview,
      account
    )

    assertEquals(
      priceData,
      Right(PriceData("GBP", BigDecimal(95.97), BigDecimal(104.97), "Quarter"))
    )
  }

  test("priceData (GA0006731)") {
    // GA0006731
    // Newspaper - Home Delivery
    // Saturday
    // Month
    val subscription = Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/GA0006731/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/HomeDelivery2025/GA0006731/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/GA0006731/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get
    val deliveryPattern = HomeDelivery2025Migration.decideDeliveryPattern(ratePlan).get

    assertEquals(
      deliveryPattern,
      HomeDelivery2025Saturday
    )

    val priceData = HomeDelivery2025Migration.priceData(
      subscription,
      invoicePreview,
      account
    )

    assertEquals(
      priceData,
      Right(PriceData("GBP", BigDecimal(19.99), BigDecimal(20.99), "Month"))
    )
  }

  test("priceData (A-S01588918-EstimationFailed)") {
    // A-S01588918

    // This sub is for HomeDelivery2025 what 344070-EstimationFailed was for Newspaper2025P1, meaning came up
    // during the EstimationFailed investigation. For Newspaper2025P1 we didn't find much, but here
    // there was a problem with HomeDelivery2025Migration.decideDeliveryPattern due to a space in the subscription's
    // rate plan's ratePlanName. Which lead to https://github.com/guardian/price-migration-engine/pull/1180

    // The `EstimationFailed` processing stage has been decommissioned

    val subscription =
      Fixtures.subscriptionFromJson("Migrations/HomeDelivery2025/A-S01588918-EstimationFailed/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/HomeDelivery2025/A-S01588918-EstimationFailed/account.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("Migrations/HomeDelivery2025/A-S01588918-EstimationFailed/invoice-preview.json")

    val ratePlan = SI2025RateplanFromSubAndInvoices.determineRatePlan_Deprecated(subscription, invoicePreview).get
    val deliveryPattern = HomeDelivery2025Migration.decideDeliveryPattern(ratePlan).get

    assertEquals(
      deliveryPattern,
      HomeDelivery2025Saturday
    )

    val priceData = HomeDelivery2025Migration.priceData(
      subscription,
      invoicePreview,
      account
    )

    assertEquals(
      priceData,
      Right(PriceData("GBP", BigDecimal(17.99), BigDecimal(20.99), "Month"))
    )
  }
}
