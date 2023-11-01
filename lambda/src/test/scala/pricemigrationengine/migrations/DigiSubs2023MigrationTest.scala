package pricemigrationengine.migrations

import pricemigrationengine.model._

import java.time.LocalDate
import pricemigrationengine.Fixtures
import pricemigrationengine.handlers.NotificationHandler.thereIsEnoughNotificationLeadTime
import pricemigrationengine.migrations.DigiSubs2023Migration.{
  getPriceFromRatePlanCharge,
  newPriceLookup,
  priceData,
  subscriptionRatePlan,
  subscriptionRatePlanCharge,
  updateOfRatePlansToCurrent
}
import pricemigrationengine.model.CohortTableFilter.SalesforcePriceRiceCreationComplete
class DigiSubs2023MigrationTest extends munit.FunSuite {
  test("monthly (1)") {

    assertEquals(newPriceLookup("GBP", Monthly).toOption.get, BigDecimal(14.99))
    assertEquals(newPriceLookup("GBP", Quarterly).toOption.get, BigDecimal(44.94))
    assertEquals(newPriceLookup("USD", Annual).toOption.get, BigDecimal(249))

    val account = Fixtures.accountFromJson("DigiSubs2023/monthly/regular/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("DigiSubs2023/monthly/regular/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("DigiSubs2023/monthly/regular/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("DigiSubs2023/monthly/regular/invoice-preview.json")

    val zuoraRatePlan1 = subscriptionRatePlan(subscription).toOption.get // If we fail here, that's part of the plan

    assertEquals(
      zuoraRatePlan1,
      ZuoraRatePlan(
        "8a12822d88f6dea80189006c5aa90928",
        "Digital Pack",
        "2c92a0fb4edd70c8014edeaa4eae220a",
        "Digital Pack Monthly",
        List(
          ZuoraRatePlanCharge(
            "2c92a0fb4edd70c9014edeaa50342192",
            "Digital Pack Monthly",
            "C-00293574",
            "GBP",
            Some(11.99),
            Some("Month"),
            Some(LocalDate.of(2023, 10, 28)),
            Some(LocalDate.of(2023, 9, 28)),
            None,
            Some("Subscription_End"),
            None,
            None,
            Some("ChargeTriggerDay"),
            Some("CustomerAcceptance"),
            None,
            None
          )
        ),
        None
      )
    )

    val ratePlanCharge1 = subscriptionRatePlanCharge(subscription, zuoraRatePlan1).toOption.get

    assertEquals(
      ratePlanCharge1,
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a0fb4edd70c9014edeaa50342192",
        name = "Digital Pack Monthly",
        number = "C-00293574",
        currency = "GBP",
        price = Some(11.99),
        billingPeriod = Some("Month"),
        chargedThroughDate = Some(LocalDate.of(2023, 10, 28)),
        processedThroughDate = Some(LocalDate.of(2023, 9, 28)),
        specificBillingPeriod = None,
        endDateCondition = Some("Subscription_End"),
        upToPeriodsType = None,
        upToPeriods = None,
        billingDay = Some("ChargeTriggerDay"),
        triggerEvent = Some("CustomerAcceptance"),
        triggerDate = None,
        discountPercentage = None
      )
    )

    val price = getPriceFromRatePlanCharge(subscription, ratePlanCharge1).toOption.get

    assertEquals(price, BigDecimal(11.99))

    assertEquals(priceData(subscription).toOption.get, PriceData("GBP", BigDecimal(11.99), BigDecimal(14.99), "Month"))

    assertEquals(
      updateOfRatePlansToCurrent(subscription, LocalDate.of(2024, 1, 1)).toOption.get,
      ZuoraSubscriptionUpdate(
        add = List(
          AddZuoraRatePlan(
            "2c92a0fb4edd70c8014edeaa4eae220a", // Digital Pack Monthly (rate plan)
            LocalDate.of(2024, 1, 1),
            chargeOverrides = List(
              ChargeOverride(
                productRatePlanChargeId = "2c92a0fb4edd70c9014edeaa50342192",
                billingPeriod = "Month",
                price = BigDecimal(14.99)
              )
            )
          )
        ), // "Digital Pack Monthly" rate plan
        remove = List(
          RemoveZuoraRatePlan(
            "8a12822d88f6dea80189006c5aa90928",
            LocalDate.of(2024, 1, 1)
          )
        ),
        currentTerm = None,
        currentTermPeriodType = None
      )
    )
  }

  test("monthly (2) discounted") {
    // Here we are only going to check what is specific to discounted subscriptions
    // For instance we do not need to check currencyToNewPriceMonthlies, but we might want to check subscriptionRatePlan
    // considering that the initial implementation (which somehow assumed that there was only one rate plan on
    // the subscription) was probably incorrect.

    val account = Fixtures.accountFromJson("DigiSubs2023/monthly/discounted/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("DigiSubs2023/monthly/discounted/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("DigiSubs2023/monthly/discounted/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("DigiSubs2023/monthly/discounted/invoice-preview.json")

    val zuoraRatePlan1 = subscriptionRatePlan(subscription).toOption.get

    assertEquals(
      zuoraRatePlan1,
      ZuoraRatePlan(
        "8a12894e8ae6c727018af5114ba93daf",
        "Digital Pack",
        "2c92a0fb4edd70c8014edeaa4e972204",
        "Digital Pack Annual",
        List(
          ZuoraRatePlanCharge(
            "2c92a0fb4edd70c9014edeaa5001218c",
            "Digital Pack Annual",
            "C-04655064",
            "GBP",
            Some(149.0),
            Some("Annual"),
            None,
            None,
            None,
            Some("Subscription_End"),
            None,
            None,
            Some("ChargeTriggerDay"),
            Some("CustomerAcceptance"),
            None,
            None
          )
        ),
        None
      )
    )

    val ratePlanCharge1 = subscriptionRatePlanCharge(subscription, zuoraRatePlan1).toOption.get

    assertEquals(
      ratePlanCharge1,
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a0fb4edd70c9014edeaa5001218c",
        name = "Digital Pack Annual",
        number = "C-04655064",
        currency = "GBP",
        price = Some(149.0),
        billingPeriod = Some("Annual"),
        chargedThroughDate = None,
        processedThroughDate = None,
        specificBillingPeriod = None,
        endDateCondition = Some("Subscription_End"),
        upToPeriodsType = None,
        upToPeriods = None,
        billingDay = Some("ChargeTriggerDay"),
        triggerEvent = Some("CustomerAcceptance"),
        triggerDate = None,
        discountPercentage = None
      )
    )

    val price = getPriceFromRatePlanCharge(subscription, ratePlanCharge1).toOption.get

    assertEquals(price, BigDecimal(149.0))

    assertEquals(priceData(subscription).toOption.get, PriceData("GBP", BigDecimal(149.0), BigDecimal(149), "Annual"))

    assertEquals(
      updateOfRatePlansToCurrent(subscription, LocalDate.of(2024, 1, 1)).toOption.get,
      ZuoraSubscriptionUpdate(
        add = List(
          AddZuoraRatePlan(
            "2c92a0fb4edd70c8014edeaa4e972204", // Digital Pack Annual (rate plan)
            LocalDate.of(2024, 1, 1),
            chargeOverrides = List(
              ChargeOverride(
                productRatePlanChargeId = "2c92a0fb4edd70c9014edeaa5001218c",
                billingPeriod = "Annual",
                price = BigDecimal(149)
              )
            )
          )
        ), // "Digital Pack Monthly" rate plan
        remove = List(
          RemoveZuoraRatePlan(
            "8a12894e8ae6c727018af5114ba93daf",
            LocalDate.of(2024, 1, 1)
          )
        ),
        currentTerm = None,
        currentTermPeriodType = None
      )
    )
  }
  test("thereIsEnoughNotificationLeadTime behaves correctly (DigiSubs2023_Batch1)") {
    // Here we are testing and calibrating the timing required for a start of emailing on 22 August 2023

    val today = LocalDate.of(2023, 10, 9)

    val itemStartDate1 = LocalDate.of(2023, 11, 8) // +30 days
    val itemStartDate2 = LocalDate.of(2023, 11, 9) // +31 days
    val itemStartDate3 = LocalDate.of(2023, 11, 10) // +32 days
    val itemStartDate4 = LocalDate.of(2023, 11, 11) // +33 days (earliest start date for DigiSubs2023_Batch1)
    val itemStartDate5 = LocalDate.of(2023, 11, 12) // +34 days

    val cohortItem1 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate1))
    val cohortItem2 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate2))
    val cohortItem3 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate3))
    val cohortItem4 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate4))
    val cohortItem5 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate5))

    val cohortSpec =
      CohortSpec("DigiSubs2023_Batch1", "BrazeCampaignName", LocalDate.of(2000, 1, 1), LocalDate.of(2023, 1, 1))

    // Reminder, the date `LocalDate.of(2023, 1, 1)`, is used to compute start dates, but has no play in thereIsEnoughNotificationLeadTime.
    // We pass the cohortSpec only to decide the
    //   - emailMaxNotificationLeadTime
    //   - emailMinNotificationLeadTime

    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem1), false) // +30 days
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem2), false) // +31 days
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem3), true) // +32 days
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem4), true) // +33 days
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem5), true) // +34 days
  }
  test("quarterly") {

    assertEquals(newPriceLookup("GBP", Monthly).toOption.get, BigDecimal(14.99))
    assertEquals(newPriceLookup("GBP", Quarterly).toOption.get, BigDecimal(44.94))
    assertEquals(newPriceLookup("USD", Annual).toOption.get, BigDecimal(249))

    val account = Fixtures.accountFromJson("DigiSubs2023/quarterly/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("DigiSubs2023/quarterly/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("DigiSubs2023/quarterly/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("DigiSubs2023/quarterly/invoice-preview.json")

    val zuoraRatePlan1 = subscriptionRatePlan(subscription).toOption.get // If we fail here, that's part of the plan

    assertEquals(
      zuoraRatePlan1,
      ZuoraRatePlan(
        "8a128bc887fb31e501880408ec0e77de",
        "Digital Pack",
        "2c92a0fb4edd70c8014edeaa4e8521fe",
        "Digital Pack Quarterly",
        List(
          ZuoraRatePlanCharge(
            "2c92a0fb4edd70c9014edeaa4fd42186",
            "Digital Pack Quarterly",
            "C-00266638",
            "GBP",
            Some(35.95),
            Some("Quarter"),
            Some(LocalDate.of(2023, 11, 10)),
            Some(LocalDate.of(2023, 8, 10)),
            None,
            Some("Subscription_End"),
            None,
            None,
            Some("ChargeTriggerDay"),
            Some("CustomerAcceptance"),
            None,
            None
          )
        ),
        None
      )
    )

    val ratePlanCharge1 = subscriptionRatePlanCharge(subscription, zuoraRatePlan1).toOption.get

    assertEquals(
      ratePlanCharge1,
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a0fb4edd70c9014edeaa4fd42186",
        name = "Digital Pack Quarterly",
        number = "C-00266638",
        currency = "GBP",
        price = Some(35.95),
        billingPeriod = Some("Quarter"),
        chargedThroughDate = Some(LocalDate.of(2023, 11, 10)),
        processedThroughDate = Some(LocalDate.of(2023, 8, 10)),
        specificBillingPeriod = None,
        endDateCondition = Some("Subscription_End"),
        upToPeriodsType = None,
        upToPeriods = None,
        billingDay = Some("ChargeTriggerDay"),
        triggerEvent = Some("CustomerAcceptance"),
        triggerDate = None,
        discountPercentage = None
      )
    )

    val price = getPriceFromRatePlanCharge(subscription, ratePlanCharge1).toOption.get

    assertEquals(price, BigDecimal(35.95))

    assertEquals(
      priceData(subscription).toOption.get,
      PriceData("GBP", BigDecimal(35.95), BigDecimal(44.94), "Quarterly")
    )

    assertEquals(
      updateOfRatePlansToCurrent(subscription, LocalDate.of(2024, 1, 1)).toOption.get,
      ZuoraSubscriptionUpdate(
        add = List(
          AddZuoraRatePlan(
            "2c92a0fb4edd70c8014edeaa4e8521fe", // Digital Pack Monthly (rate plan)
            LocalDate.of(2024, 1, 1),
            chargeOverrides = List(
              ChargeOverride(
                productRatePlanChargeId = "2c92a0fb4edd70c9014edeaa4fd42186",
                billingPeriod = "Quarterly",
                price = BigDecimal(44.94)
              )
            )
          )
        ), // "Digital Pack Monthly" rate plan
        remove = List(
          RemoveZuoraRatePlan(
            "8a128bc887fb31e501880408ec0e77de",
            LocalDate.of(2024, 1, 1)
          )
        ),
        currentTerm = None,
        currentTermPeriodType = None
      )
    )
  }
  test("annual") {

    assertEquals(newPriceLookup("GBP", Monthly).toOption.get, BigDecimal(14.99))
    assertEquals(newPriceLookup("GBP", Quarterly).toOption.get, BigDecimal(44.94))
    assertEquals(newPriceLookup("USD", Annual).toOption.get, BigDecimal(249))

    val account = Fixtures.accountFromJson("DigiSubs2023/annual/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("DigiSubs2023/annual/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("DigiSubs2023/annual/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("DigiSubs2023/annual/invoice-preview.json")

    val zuoraRatePlan1 = subscriptionRatePlan(subscription).toOption.get // If we fail here, that's part of the plan

    assertEquals(
      zuoraRatePlan1,
      ZuoraRatePlan(
        "8a128ab58b7f7f47018b89a3dd7c3d04",
        "Digital Pack",
        "2c92a0fb4edd70c8014edeaa4e972204",
        "Digital Pack Annual",
        List(
          ZuoraRatePlanCharge(
            "2c92a0fb4edd70c9014edeaa5001218c",
            "Digital Pack Annual",
            "C-04065127",
            "GBP",
            Some(119.0),
            Some("Annual"),
            Some(LocalDate.of(2023, 11, 17)),
            Some(LocalDate.of(2022, 11, 17)),
            None,
            Some("Subscription_End"),
            None,
            None,
            Some("ChargeTriggerDay"),
            Some("CustomerAcceptance"),
            None,
            None
          )
        ),
        None
      )
    )

    val ratePlanCharge1 = subscriptionRatePlanCharge(subscription, zuoraRatePlan1).toOption.get

    assertEquals(
      ratePlanCharge1,
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a0fb4edd70c9014edeaa5001218c",
        name = "Digital Pack Annual",
        number = "C-04065127",
        currency = "GBP",
        price = Some(119.0),
        billingPeriod = Some("Annual"),
        chargedThroughDate = Some(LocalDate.of(2023, 11, 17)),
        processedThroughDate = Some(LocalDate.of(2022, 11, 17)),
        specificBillingPeriod = None,
        endDateCondition = Some("Subscription_End"),
        upToPeriodsType = None,
        upToPeriods = None,
        billingDay = Some("ChargeTriggerDay"),
        triggerEvent = Some("CustomerAcceptance"),
        triggerDate = None,
        discountPercentage = None
      )
    )

    val price = getPriceFromRatePlanCharge(subscription, ratePlanCharge1).toOption.get

    assertEquals(price, BigDecimal(119.0))

    assertEquals(
      priceData(subscription).toOption.get,
      PriceData("GBP", BigDecimal(119.0), BigDecimal(149), "Annual")
    )

    assertEquals(
      updateOfRatePlansToCurrent(subscription, LocalDate.of(2024, 1, 1)).toOption.get,
      ZuoraSubscriptionUpdate(
        add = List(
          AddZuoraRatePlan(
            "2c92a0fb4edd70c8014edeaa4e972204", // Digital Pack Monthly (rate plan)
            LocalDate.of(2024, 1, 1),
            chargeOverrides = List(
              ChargeOverride(
                productRatePlanChargeId = "2c92a0fb4edd70c9014edeaa5001218c",
                billingPeriod = "Annual",
                price = BigDecimal(149)
              )
            )
          )
        ), // "Digital Pack Monthly" rate plan
        remove = List(
          RemoveZuoraRatePlan(
            "8a128ab58b7f7f47018b89a3dd7c3d04",
            LocalDate.of(2024, 1, 1)
          )
        ),
        currentTerm = None,
        currentTermPeriodType = None
      )
    )
  }
  test("thereIsEnoughNotificationLeadTime behaves correctly (DigiSubs2023_Batch2)") {
    // Here we are testing and calibrating the timing required for a start of emailing on 6 November 2023

    val today = LocalDate.of(2023, 11, 6)

    val itemStartDate1 = LocalDate.of(2023, 12, 6) // +30 days
    val itemStartDate2 = LocalDate.of(2023, 12, 7) // +31 days
    val itemStartDate3 = LocalDate.of(2023, 12, 8) // +32 days
    val itemStartDate4 = LocalDate.of(2023, 12, 9) // +33 days (earliest start date for DigiSubs2023_Batch2)
    val itemStartDate5 = LocalDate.of(2023, 12, 10) // +34 days

    val cohortItem1 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate1))
    val cohortItem2 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate2))
    val cohortItem3 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate3))
    val cohortItem4 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate4))
    val cohortItem5 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate5))

    val cohortSpec =
      CohortSpec("DigiSubs2023_Batch2", "BrazeCampaignName", LocalDate.of(2000, 1, 1), LocalDate.of(2023, 1, 1))

    // Reminder, the date `LocalDate.of(2023, 1, 1)`, is used to compute start dates, but has no play in thereIsEnoughNotificationLeadTime.
    // We pass the cohortSpec only to decide the
    //   - emailMaxNotificationLeadTime
    //   - emailMinNotificationLeadTime

    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem1), false) // +30 days
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem2), false) // +31 days
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem3), true) // +32 days
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem4), true) // +33 days
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem5), true) // +34 days
  }
}
