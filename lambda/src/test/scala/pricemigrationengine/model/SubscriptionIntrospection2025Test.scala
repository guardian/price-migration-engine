package pricemigrationengine.model

import pricemigrationengine.Fixtures
import pricemigrationengine.migrations.Membership2025Migration

import java.time.LocalDate

class SI2025ExtractionsTest extends munit.FunSuite {

  // ----------------------------------------------------
  // SI2025RateplanFromSubAndInvoices
  // ----------------------------------------------------

  test("SI2025RateplanFromSubAndInvoices.determineRatePlan (1)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/SubscriptionIntrospection2025/subscription5-multiple-products-in-invoice-preview/subscription.json"
      )

    val invoiceList =
      Fixtures.invoiceListFromJson(
        "model/SubscriptionIntrospection2025/subscription5-multiple-products-in-invoice-preview/invoice-preview.json"
      )

    // Here we test that we determine the right rate plan using the `SubAndInvoices` method
    // in the case of two products in the invoice preview.

    val ratePlan =
      SI2025RateplanFromSubAndInvoices.determineRatePlan(subscription, invoiceList)
    assertEquals(
      ratePlan.get.ratePlanName,
      "Supporter - annual (2023 Price)"
    )
  }

  test("SI2025RateplanFromSubAndInvoices.determineRatePlan (2)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/SubscriptionIntrospection2025/subscription6-multiple-products-in-invoice-preview/subscription.json"
      )

    val invoiceList =
      Fixtures.invoiceListFromJson(
        "model/SubscriptionIntrospection2025/subscription6-multiple-products-in-invoice-preview/invoice-preview.json"
      )

    // Here we test that we determine the right rate plan using the `SubAndInvoices` method
    // in the case of two products in the invoice preview.

    val ratePlan =
      SI2025RateplanFromSubAndInvoices.determineRatePlan(subscription, invoiceList).get
    assertEquals(
      ratePlan.ratePlanName,
      "Supporter - monthly (2023 Price)"
    )

    assertEquals(
      SI2025Extractions.determineCurrency(ratePlan).get,
      "GBP"
    )

    assertEquals(
      SI2025Extractions.determineBillingPeriod(ratePlan).get,
      Monthly
    )

    assertEquals(
      SI2025Extractions.determineOldPrice(ratePlan),
      BigDecimal(7.0)
    )

    assertEquals(
      Membership2025Migration.priceGridNewPrices.get((Monthly, "GBP")).get,
      BigDecimal(10.0)
    )

    assertEquals(
      Membership2025Migration.priceData(
        subscription,
        invoiceList,
      ),
      Right(PriceData("GBP", BigDecimal(7.0), BigDecimal(10.0), "Month"))
    )
  }

  test("SI2025RateplanFromSubAndInvoices.determineRatePlan (3)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/SubscriptionIntrospection2025/subscription7-product-and-discount-in-invoice-preview/subscription.json"
      )

    val invoiceList =
      Fixtures.invoiceListFromJson(
        "model/SubscriptionIntrospection2025/subscription7-product-and-discount-in-invoice-preview/invoice-preview.json"
      )

    // In this case we have two active products on the subscription itself, and one of them
    // is a discount. This is a particular case of the more general case of handling
    // more than one discounts on the subscription while looking for the target rate plan.

    val ratePlan =
      SI2025RateplanFromSubAndInvoices.determineRatePlan(subscription, invoiceList).get
    assertEquals(
      ratePlan.ratePlanName,
      "Supporter - annual (2023 Price)"
    )

    assertEquals(
      SI2025Extractions.determineCurrency(ratePlan).get,
      "GBP"
    )

    assertEquals(
      SI2025Extractions.determineBillingPeriod(ratePlan).get,
      Annual
    )

    assertEquals(
      SI2025Extractions.determineOldPrice(ratePlan),
      BigDecimal(75.0)
    )

    assertEquals(
      Membership2025Migration.priceGridNewPrices.get((Annual, "GBP")).get,
      BigDecimal(100.0)
    )

    assertEquals(
      Membership2025Migration.priceData(
        subscription,
        invoiceList,
      ),
      Right(PriceData("GBP", BigDecimal(75.0), BigDecimal(100.0), "Annual"))
    )
  }

  // ----------------------------------------------------
  // SI2025RateplanFromSub
  // ----------------------------------------------------

  test("SI2025RateplanFromSub.uniquelyDeterminedActiveNonDiscountRatePlan (subscription2)") {
    // Subscription 2
    // Was selected due to having an active discount alongside the rate plan
    // and the discount comes first to test that discounts are filtered out

    val subscription =
      Fixtures.subscriptionFromJson("model/SubscriptionIntrospection2025/subscription2/subscription.json")

    val ratePlan = SI2025RateplanFromSub.uniquelyDeterminedActiveNonDiscountRatePlan(subscription)
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

  test("SI2025RateplanFromSub.uniquelyDeterminedActiveNonDiscountRatePlan (subscription8)") {
    // This sub has two active rate plans, so technically it would fail
    // `SI2025RateplanFromSub.determineRatePlan`, but on of the rate plans
    // is a [GW Oct 18 - Six for Six - ROW] which expired in 2021-01-15 but has not been removed.
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/SubscriptionIntrospection2025/subscription8-two-active-rate-plans-one-zombie/subscription.json"
      )
    // assertEquals(
    //  SI2025RateplanFromSub.uniquelyDeterminedActiveNonDiscountRatePlan(subscription).isDefined,
    //  true
    // )
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

  test("SI2025Extractions.getDiscountByRatePlanName (Percentage)") {
    val subscription =
      Fixtures.subscriptionFromJson("model/SubscriptionIntrospection2025/subscription3-with-discount/subscription.json")
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

  test("SI2025Extractions.getDiscountByRatePlanName (Adjustment)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/SubscriptionIntrospection2025/subscription4-511760-Discount-Adjustment/subscription.json"
      )
    assertEquals(
      SI2025Extractions.getDiscountByRatePlanName(subscription, "Adjustment"),
      Some(
        ZuoraRatePlan(
          id = "8a128efc91a138d30191bb00a80c281f",
          productName = "Discounts",
          productRatePlanId = "2c92a0ff5e09bd67015e0a93efe86d2e",
          ratePlanName = "Customer Experience Adjustment - Voucher",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "2c92a0ff5e09bd67015e0a93f0026d34",
              name = "Adjustment charge",
              number = "C-01103430",
              currency = "GBP",
              price = Some(-4.15),
              billingPeriod = Some("Month"),
              chargedThroughDate = Some(LocalDate.of(2025, 8, 4)),
              processedThroughDate = Some(LocalDate.of(2025, 7, 4)),
              specificBillingPeriod = None,
              endDateCondition = Some("Subscription_End"),
              upToPeriodsType = None,
              upToPeriods = None,
              billingDay = Some("DefaultFromCustomer"),
              triggerEvent = Some("CustomerAcceptance"),
              triggerDate = None,
              discountPercentage = None,
              originalOrderDate = Some(LocalDate.of(2017, 8, 24)),
              effectiveStartDate = Some(LocalDate.of(2017, 9, 4)),
              effectiveEndDate = Some(LocalDate.of(2025, 9, 4))
            )
          ),
          lastChangeType = Some("Add")
        )
      )
    )
  }

  test("SI2025Extractions.getPercentageOrAdjustementDiscount (finding Percentage)") {
    val subscription =
      Fixtures.subscriptionFromJson("model/SubscriptionIntrospection2025/subscription3-with-discount/subscription.json")

    // The subscription has a "Percentage" discount, so that's what we expect

    assertEquals(
      SI2025Extractions.getPercentageOrAdjustementDiscount(subscription),
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

  test("SI2025Extractions.getPercentageOrAdjustementDiscount (finding Adjustment)") {
    val subscription =
      Fixtures.subscriptionFromJson(
        "model/SubscriptionIntrospection2025/subscription4-511760-Discount-Adjustment/subscription.json"
      )

    // The subscription has a "* Adjustment *" discount, so that's what we expect

    assertEquals(
      SI2025Extractions.getPercentageOrAdjustementDiscount(subscription),
      Some(
        ZuoraRatePlan(
          id = "8a128efc91a138d30191bb00a80c281f",
          productName = "Discounts",
          productRatePlanId = "2c92a0ff5e09bd67015e0a93efe86d2e",
          ratePlanName = "Customer Experience Adjustment - Voucher",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "2c92a0ff5e09bd67015e0a93f0026d34",
              name = "Adjustment charge",
              number = "C-01103430",
              currency = "GBP",
              price = Some(-4.15),
              billingPeriod = Some("Month"),
              chargedThroughDate = Some(LocalDate.of(2025, 8, 4)),
              processedThroughDate = Some(LocalDate.of(2025, 7, 4)),
              specificBillingPeriod = None,
              endDateCondition = Some("Subscription_End"),
              upToPeriodsType = None,
              upToPeriods = None,
              billingDay = Some("DefaultFromCustomer"),
              triggerEvent = Some("CustomerAcceptance"),
              triggerDate = None,
              discountPercentage = None,
              originalOrderDate = Some(LocalDate.of(2017, 8, 24)),
              effectiveStartDate = Some(LocalDate.of(2017, 9, 4)),
              effectiveEndDate = Some(LocalDate.of(2025, 9, 4))
            )
          ),
          lastChangeType = Some("Add")
        )
      )
    )
  }

  test("SI2025Extractions.subscriptionHasActiveDiscounts (no discounts)") {
    val subscription =
      Fixtures.subscriptionFromJson("model/SubscriptionIntrospection2025/subscription1/subscription.json")
    val date = LocalDate.of(2025, 9, 23)
    assertEquals(
      SI2025Extractions.subscriptionHasActiveDiscounts(subscription, date),
      false
    )
  }

  test("SI2025Extractions.subscriptionHasActiveDiscounts (discounts)") {
    val subscription =
      Fixtures.subscriptionFromJson("model/SubscriptionIntrospection2025/subscription3-with-discount/subscription.json")
    val date = LocalDate.of(2025, 9, 23)
    assertEquals(
      SI2025Extractions.subscriptionHasActiveDiscounts(subscription, date),
      true
    )
  }

  // ----------------------------------------------------
  // SI2025Templates
  // ----------------------------------------------------

  test("SI2025Templates.priceData") {
    val subscription =
      Fixtures.subscriptionFromJson("model/SubscriptionIntrospection2025/subscription1/subscription.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("model/SubscriptionIntrospection2025/subscription1/invoice-preview.json")
    val priceData = SI2025Templates.priceData(subscription, invoicePreview)
    assertEquals(priceData, Right(PriceData("USD", BigDecimal(90.0), BigDecimal(2.71), "Quarter")))
  }
}
