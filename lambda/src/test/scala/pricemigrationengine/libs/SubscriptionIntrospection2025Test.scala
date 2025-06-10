package pricemigrationengine.libs

import pricemigrationengine.Fixtures
import pricemigrationengine.model._
import java.time.LocalDate

// This module has its own fixtures at test/resources/libs/SubscriptionIntrospection2025/

// For the subscription, for instance subscription1, I added all the fixtures we usually
// download for migrations:
// val subscription = Fixtures.subscriptionFromJson("libs/SubscriptionIntrospection2025/subscription1/subscription.json")
// val account = Fixtures.accountFromJson("libs/SubscriptionIntrospection2025/subscription1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("libs/SubscriptionIntrospection2025/subscription1/invoice-preview.json")
// val catalogue = Fixtures.productCatalogueFromJson("libs/SubscriptionIntrospection2025/subscription1/catalogue.json")

// ----------------------------------------------------------------------------------
// subscription1:
// subscription1 is a "Guardian Weekly - Domestic"
// It has 3 rate plans
// - rate plan name: GW Oct 18 - Quarterly - Domestic / "lastChangeType" : "Remove"
// - rate plan name: GW Oct 18 - Quarterly - Domestic / "lastChangeType" : "Add"
// - rate plan name: Guardian Weekly Quarterly        / "lastChangeType" : "Remove"
// ----------------------------------------------------------------------------------

// Subscription 2
// Was selected due to having an active discount alongside the ratePlanName: "GW Oct 18 - Annual - Domestic"
// Which I also put on first position; to help test the active rate plan resolution
// val subscription = Fixtures.subscriptionFromJson("libs/SubscriptionIntrospection2025/subscription2/subscription.json")

class SI2025ExtractionsTest extends munit.FunSuite {

  // ----------------------------------------------------
  // SI2025RateplanFromSubAndInvoices
  // ----------------------------------------------------

  test("SI2025RateplanFromSubAndInvoices.invoicePreviewToChargeNumber") {
    /*
      We are specifically after this rate plan:
      {
          "id" : "8a12865b96d3500b0196e182a5685157",
          "lastChangeType" : "Add",
          "productId" : "2c92a0ff6619bf8901661aa3247c4b1d",
          "productName" : "Guardian Weekly - Domestic",
          "productSku" : "ABC-00000029",
          "productRatePlanId" : "2c92a0fe6619b4b301661aa494392ee2",
          "productRatePlanNumber" : "PRP-00000121",
          "ratePlanName" : "GW Oct 18 - Quarterly - Domestic",
          "subscriptionProductFeatures" : [ ],
          "externallyManagedPlanId" : null,
          "subscriptionRatePlanNumber" : "SRP-05479654",
          "isFromExternalCatalog" : false,
          "ratePlanCharges" : [ {
            "id" : "8a12865b96d3500b0196e182a56a5159",
            "originalChargeId" : "8a12964790498d88019067fa9cc36243",
            "productRatePlanChargeId" : "2c92a0fe6619b4b601661aa8b74e623f",
            "number" : "C-05719965",
            "name" : "GW Oct 18 - Quarterly - Domestic",
            "productRatePlanChargeNumber" : "PRPC-00000191",
            "type" : "Recurring",
            "model" : "FlatFee",
            "originalListPrice" : null,
            "uom" : null,
            "version" : 2,
            "subscriptionChargeDeliverySchedule" : null,
            "numberOfDeliveries" : null,
            "priceChangeOption" : "NoChange",
            "priceIncreasePercentage" : null,
            "currency" : "USD",
            "chargeModelConfiguration" : null,
            "inputArgumentId" : null,
            "includedUnits" : null,
            "overagePrice" : null,
            "applyDiscountTo" : null,
            "discountLevel" : null,
            "discountClass" : null,
            "applyToBillingPeriodPartially" : false,
            "billingDay" : "ChargeTriggerDay",
            "listPriceBase" : "Per_Billing_Period",
            "specificListPriceBase" : null,
            "billingPeriod" : "Quarter",
            "specificBillingPeriod" : null,
            "billingTiming" : "IN_ADVANCE",
            "ratingGroup" : null,
            "billingPeriodAlignment" : "AlignToCharge",
            "quantity" : 1.000000000,
            "prorationOption" : null,
            "isStackedDiscount" : false,
            "reflectDiscountInNetAmount" : false,
            "smoothingModel" : null,
            "numberOfPeriods" : null,
            "overageCalculationOption" : null,
            "overageUnusedUnitsCreditOption" : null,
            "unusedUnitsCreditRates" : null,
            "usageRecordRatingOption" : null,
            "segment" : 1,
            "effectiveStartDate" : "2024-08-18",
            "effectiveEndDate" : "2026-05-18",
            "processedThroughDate" : "2025-05-18",
            "chargedThroughDate" : "2025-08-18",
            "done" : false,
            "triggerDate" : null,
            "triggerEvent" : "CustomerAcceptance",
            "endDateCondition" : "Subscription_End",
            "upToPeriodsType" : null,
            "upToPeriods" : null,
            "specificEndDate" : null,
            "mrr" : 30.000000000,
            "dmrc" : 0.000000000,
            "tcv" : 630.000000000,
            "dtcv" : 360.000000000,
            "originalOrderDate" : "2024-06-30",
            "amendedByOrderOn" : "2025-05-18",
            "description" : "",
            "HolidayStart__c" : null,
            "HolidayEnd__c" : null,
            "ForceSync__c" : null,
            "salesPrice" : null,
            "taxable" : null,
            "taxCode" : null,
            "taxMode" : null,
            "tiers" : null,
            "discountApplyDetails" : null,
            "pricingSummary" : "USD90",
            "price" : 90.000000000,
            "discountAmount" : null,
            "discountPercentage" : null
          } ]
        }
     */

    val invoicePreview =
      Fixtures.invoiceListFromJson("libs/SubscriptionIntrospection2025/subscription1/invoice-preview.json")
    val chargeNumber =
      SI2025RateplanFromSubAndInvoices.invoicePreviewToChargeNumber(invoicePreview)
    assertEquals(chargeNumber, Some("C-05719965"))
  }

  test("SI2025RateplanFromSubAndInvoices.ratePlanChargeNumberToMatchingRatePlan") {
    val subscription =
      Fixtures.subscriptionFromJson("libs/SubscriptionIntrospection2025/subscription1/subscription.json")

    val ratePlan =
      SI2025RateplanFromSubAndInvoices.ratePlanChargeNumberToMatchingRatePlan(
        subscription,
        "C-05719965" // <- value from the previous test, otherwise can be read from the fixture data.
      )
    assertEquals(
      ratePlan,
      Some(
        ZuoraRatePlan(
          id = "8a12865b96d3500b0196e182a5685157",
          productName = "Guardian Weekly - Domestic",
          productRatePlanId = "2c92a0fe6619b4b301661aa494392ee2",
          ratePlanName = "GW Oct 18 - Quarterly - Domestic",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "2c92a0fe6619b4b601661aa8b74e623f",
              name = "GW Oct 18 - Quarterly - Domestic",
              number = "C-05719965",
              currency = "USD",
              price = Some(BigDecimal(90.0)),
              billingPeriod = Some("Quarter"),
              chargedThroughDate = Some(LocalDate.of(2025, 8, 18)),
              processedThroughDate = Some(LocalDate.of(2025, 5, 18)),
              specificBillingPeriod = None,
              endDateCondition = Some("Subscription_End"),
              upToPeriodsType = None,
              upToPeriods = None,
              billingDay = Some("ChargeTriggerDay"),
              triggerEvent = Some("CustomerAcceptance"),
              triggerDate = None,
              discountPercentage = None,
              originalOrderDate = Some(LocalDate.of(2024, 6, 30)),
              effectiveStartDate = Some(LocalDate.of(2024, 8, 18)),
              effectiveEndDate = Some(LocalDate.of(2026, 5, 18))
            )
          ),
          lastChangeType = Some("Add")
        )
      )
    )
  }

  // ----------------------------------------------------
  // SI2025RateplanFromSub
  // ----------------------------------------------------

  // Subscription 2
  // Was selected due to having an active discount alongside the rate plan
  // and the discount comes first to test that discounts are filtered out

  test("SI2025RateplanFromSub.determineRatePlan (subscription2)") {
    val subscription =
      Fixtures.subscriptionFromJson("libs/SubscriptionIntrospection2025/subscription2/subscription.json")

    val ratePlan = SI2025RateplanFromSub.determineRatePlan(subscription)
    assertEquals(
      ratePlan,
      Some(
        ZuoraRatePlan(
          id = "8a12820a92f75e4b0192fb2364496183",
          productName = "Guardian Weekly - Domestic",
          productRatePlanId = "2c92a0fe6619b4b901661aa8e66c1692",
          ratePlanName = "GW Oct 18 - Annual - Domestic",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "2c92a0fe6619b4b901661aa8e6811695",
              name = "GW Oct 18 - Annual - Domestic",
              number = "C-06089952",
              currency = "EUR",
              price = Some(BigDecimal(318.0)),
              billingPeriod = Some("Annual"),
              chargedThroughDate = Some(LocalDate.of(2025, 12, 24)),
              processedThroughDate = Some(LocalDate.of(2024, 12, 24)),
              specificBillingPeriod = None,
              endDateCondition = Some("Subscription_End"),
              upToPeriodsType = None,
              upToPeriods = None,
              billingDay = Some("ChargeTriggerDay"),
              triggerEvent = Some("CustomerAcceptance"),
              triggerDate = None,
              discountPercentage = None,
              originalOrderDate = Some(LocalDate.of(2024, 11, 5)),
              effectiveStartDate = Some(LocalDate.of(2024, 12, 24)),
              effectiveEndDate = Some(LocalDate.of(2025, 12, 24))
            )
          ),
          lastChangeType = Some("Add")
        )
      )
    )
  }

  // ----------------------------------------------------
  // SI2025Extractions
  // ----------------------------------------------------

  test("SI2025Extractions.determineCurrency") {
    val ratePlan = ZuoraRatePlan(
      id = "8a12865b96d3500b0196e182a5685157",
      productName = "Guardian Weekly - Domestic",
      productRatePlanId = "2c92a0fe6619b4b301661aa494392ee2",
      ratePlanName = "GW Oct 18 - Quarterly - Domestic",
      ratePlanCharges = List(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fe6619b4b601661aa8b74e623f",
          name = "GW Oct 18 - Quarterly - Domestic",
          number = "C-05719965",
          currency = "USD",
          price = Some(BigDecimal(90.0)),
          billingPeriod = Some("Quarter"),
          chargedThroughDate = Some(LocalDate.of(2025, 8, 18)),
          processedThroughDate = Some(LocalDate.of(2025, 5, 18)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 6, 30)),
          effectiveStartDate = Some(LocalDate.of(2024, 8, 18)),
          effectiveEndDate = Some(LocalDate.of(2026, 5, 18))
        )
      ),
      lastChangeType = Some("Add")
    )
    val currency = SI2025Extractions.determineCurrency(ratePlan)
    assertEquals(currency, Some("USD"))
  }

  test("SI2025Extractions.determineBillingPeriod") {
    val ratePlan = ZuoraRatePlan(
      id = "8a12865b96d3500b0196e182a5685157",
      productName = "Guardian Weekly - Domestic",
      productRatePlanId = "2c92a0fe6619b4b301661aa494392ee2",
      ratePlanName = "GW Oct 18 - Quarterly - Domestic",
      ratePlanCharges = List(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fe6619b4b601661aa8b74e623f",
          name = "GW Oct 18 - Quarterly - Domestic",
          number = "C-05719965",
          currency = "USD",
          price = Some(BigDecimal(90.0)),
          billingPeriod = Some("Quarter"),
          chargedThroughDate = Some(LocalDate.of(2025, 8, 18)),
          processedThroughDate = Some(LocalDate.of(2025, 5, 18)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 6, 30)),
          effectiveStartDate = Some(LocalDate.of(2024, 8, 18)),
          effectiveEndDate = Some(LocalDate.of(2026, 5, 18))
        )
      ),
      lastChangeType = Some("Add")
    )
    val billingPeriod = SI2025Extractions.determineBillingPeriod(ratePlan)
    assertEquals(billingPeriod, Some(Quarterly))
  }

  test("SI2025Extractions.determineOldPrice") {
    val ratePlan = ZuoraRatePlan(
      id = "8a12865b96d3500b0196e182a5685157",
      productName = "Guardian Weekly - Domestic",
      productRatePlanId = "2c92a0fe6619b4b301661aa494392ee2",
      ratePlanName = "GW Oct 18 - Quarterly - Domestic",
      ratePlanCharges = List(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fe6619b4b601661aa8b74e623f",
          name = "GW Oct 18 - Quarterly - Domestic",
          number = "C-05719965",
          currency = "USD",
          price = Some(BigDecimal(90.0)),
          billingPeriod = Some("Quarter"),
          chargedThroughDate = Some(LocalDate.of(2025, 8, 18)),
          processedThroughDate = Some(LocalDate.of(2025, 5, 18)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 6, 30)),
          effectiveStartDate = Some(LocalDate.of(2024, 8, 18)),
          effectiveEndDate = Some(LocalDate.of(2026, 5, 18))
        )
      ),
      lastChangeType = Some("Add")
    )
    val price = SI2025Extractions.determineOldPrice(ratePlan)
    assertEquals(price, BigDecimal(90.0))
  }

  test("SI2025Extractions.determineLastPriceMigrationDate") {
    val ratePlan = ZuoraRatePlan(
      id = "8a12865b96d3500b0196e182a5685157",
      productName = "Guardian Weekly - Domestic",
      productRatePlanId = "2c92a0fe6619b4b301661aa494392ee2",
      ratePlanName = "GW Oct 18 - Quarterly - Domestic",
      ratePlanCharges = List(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "2c92a0fe6619b4b601661aa8b74e623f",
          name = "GW Oct 18 - Quarterly - Domestic",
          number = "C-05719965",
          currency = "USD",
          price = Some(BigDecimal(90.0)),
          billingPeriod = Some("Quarter"),
          chargedThroughDate = Some(LocalDate.of(2025, 8, 18)),
          processedThroughDate = Some(LocalDate.of(2025, 5, 18)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 6, 30)),
          effectiveStartDate = Some(LocalDate.of(2024, 8, 18)),
          effectiveEndDate = Some(LocalDate.of(2026, 5, 18))
        )
      ),
      lastChangeType = Some("Add")
    )
    assertEquals(
      SI2025Extractions.determineLastPriceMigrationDate(ratePlan),
      Some(LocalDate.of(2024, 6, 30))
    )
  }

  // ----------------------------------------------------
  // SI2025Templates
  // ----------------------------------------------------

  test("SI2025Templates.priceData") {
    val subscription =
      Fixtures.subscriptionFromJson("libs/SubscriptionIntrospection2025/subscription1/subscription.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("libs/SubscriptionIntrospection2025/subscription1/invoice-preview.json")
    val priceData = SI2025Templates.priceData(subscription, invoicePreview)
    assertEquals(priceData, Right(PriceData("USD", BigDecimal(90.0), BigDecimal(2.71), "Quarter")))
  }
}
