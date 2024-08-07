package pricemigrationengine.migrations

import pricemigrationengine.model._

import java.time.LocalDate
import pricemigrationengine.Fixtures
import pricemigrationengine.handlers.NotificationHandler
import pricemigrationengine.util.StartDates

class GW2024MigrationTest extends munit.FunSuite {

  test("Price lookup (version 1) is correct") {
    assertEquals(
      GW2024Migration.getNewPrice(Monthly, "GBP"),
      Some(BigDecimal(15))
    )
    assertEquals(
      GW2024Migration.getNewPrice(Quarterly, "ROW (USD)"),
      Some(BigDecimal(99))
    )
  }

  // -------------------------------------

  test("Rate plan (s) determination is correct (standard)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/standard/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/GW2024/standard/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/GW2024/standard/invoice-preview.json")
    // val catalogue = Fixtures.productCatalogueFromJson("Migrations/GW2024/standard/catalogue.json")
    assertEquals(
      GW2024Migration.subscriptionToMigrationRatePlans(subscription),
      List(
        ZuoraRatePlan(
          id = "8a128d6988b434050188d210ef897dc2",
          productName = "Guardian Weekly - Domestic",
          productRatePlanId = "2c92a0fe6619b4b901661aa8e66c1692",
          ratePlanName = "GW Oct 18 - Annual - Domestic",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "2c92a0fe6619b4b901661aa8e6811695",
              name = "GW Oct 18 - Annual - Domestic",
              number = "C-02169680",
              currency = "USD",
              price = Some(BigDecimal(300.0)),
              billingPeriod = Some("Annual"),
              chargedThroughDate = Some(LocalDate.of(2024, 6, 19)),
              processedThroughDate = Some(LocalDate.of(2023, 6, 19)),
              specificBillingPeriod = None,
              endDateCondition = Some("Subscription_End"),
              upToPeriodsType = None,
              upToPeriods = None,
              billingDay = Some("ChargeTriggerDay"),
              triggerEvent = Some("CustomerAcceptance"),
              triggerDate = None,
              discountPercentage = None,
              originalOrderDate = Some(LocalDate.of(2020, 6, 8)),
              effectiveStartDate = Some(LocalDate.of(2020, 6, 19)),
              effectiveEndDate = Some(LocalDate.of(2024, 6, 19))
            )
          ),
          lastChangeType = Some("Add")
        )
      )
    )
  }

  test("Rate plan (s) determination is correct (ROW-DomesticRatePlan)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/ROW-DomesticRatePlan/subscription.json")
    assertEquals(
      GW2024Migration.subscriptionToMigrationRatePlans(subscription),
      List(
        ZuoraRatePlan(
          id = "8a128d0788e11b350188f63ab4995f7a",
          productName = "Guardian Weekly - Domestic",
          productRatePlanId = "2c92a0fe6619b4b901661aa8e66c1692",
          ratePlanName = "GW Oct 18 - Annual - Domestic",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "2c92a0fe6619b4b901661aa8e6811695",
              name = "GW Oct 18 - Annual - Domestic",
              number = "C-02182036",
              currency = "USD",
              price = Some(BigDecimal(300.0)),
              billingPeriod = Some("Annual"),
              chargedThroughDate = Some(LocalDate.of(2024, 6, 26)),
              processedThroughDate = Some(LocalDate.of(2023, 6, 26)),
              specificBillingPeriod = None,
              endDateCondition = Some("Subscription_End"),
              upToPeriodsType = None,
              upToPeriods = None,
              billingDay = Some("ChargeTriggerDay"),
              triggerEvent = Some("CustomerAcceptance"),
              triggerDate = None,
              discountPercentage = None,
              originalOrderDate = Some(LocalDate.of(2020, 6, 15)),
              effectiveStartDate = Some(LocalDate.of(2020, 6, 26)),
              effectiveEndDate = Some(LocalDate.of(2024, 6, 26))
            )
          ),
          lastChangeType = None
        )
      )
    )
  }

  // -------------------------------------

  test("Rate plan determination is correct (standard)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/standard/subscription.json")
    assertEquals(
      GW2024Migration.subscriptionToMigrationRatePlan(subscription),
      Some(
        ZuoraRatePlan(
          id = "8a128d6988b434050188d210ef897dc2",
          productName = "Guardian Weekly - Domestic",
          productRatePlanId = "2c92a0fe6619b4b901661aa8e66c1692",
          ratePlanName = "GW Oct 18 - Annual - Domestic",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "2c92a0fe6619b4b901661aa8e6811695",
              name = "GW Oct 18 - Annual - Domestic",
              number = "C-02169680",
              currency = "USD",
              price = Some(BigDecimal(300.0)),
              billingPeriod = Some("Annual"),
              chargedThroughDate = Some(LocalDate.of(2024, 6, 19)),
              processedThroughDate = Some(LocalDate.of(2023, 6, 19)),
              specificBillingPeriod = None,
              endDateCondition = Some("Subscription_End"),
              upToPeriodsType = None,
              upToPeriods = None,
              billingDay = Some("ChargeTriggerDay"),
              triggerEvent = Some("CustomerAcceptance"),
              triggerDate = None,
              discountPercentage = None,
              originalOrderDate = Some(LocalDate.of(2020, 6, 8)),
              effectiveStartDate = Some(LocalDate.of(2020, 6, 19)),
              effectiveEndDate = Some(LocalDate.of(2024, 6, 19))
            )
          ),
          lastChangeType = Some("Add")
        )
      )
    )
  }

  test("Rate plan determination is correct (ROW-DomesticRatePlan)") {
    // This test is used to show the difference between lastChangeTypeIsAdd and lastChangeTypeIsNotRemove
    // This is a subscription where `lastChangeType` is not defined, therefore lastChangeTypeIsAdd would not select it
    // but lastChangeTypeIsNotRemove would

    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/ROW-DomesticRatePlan/subscription.json")
    assertEquals(
      GW2024Migration.subscriptionToMigrationRatePlan(subscription),
      Some(
        ZuoraRatePlan(
          id = "8a128d0788e11b350188f63ab4995f7a",
          productName = "Guardian Weekly - Domestic",
          productRatePlanId = "2c92a0fe6619b4b901661aa8e66c1692",
          ratePlanName = "GW Oct 18 - Annual - Domestic",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "2c92a0fe6619b4b901661aa8e6811695",
              name = "GW Oct 18 - Annual - Domestic",
              number = "C-02182036",
              currency = "USD",
              price = Some(BigDecimal(300.0)),
              billingPeriod = Some("Annual"),
              chargedThroughDate = Some(LocalDate.of(2024, 6, 26)),
              processedThroughDate = Some(LocalDate.of(2023, 6, 26)),
              specificBillingPeriod = None,
              endDateCondition = Some("Subscription_End"),
              upToPeriodsType = None,
              upToPeriods = None,
              billingDay = Some("ChargeTriggerDay"),
              triggerEvent = Some("CustomerAcceptance"),
              triggerDate = None,
              discountPercentage = None,
              originalOrderDate = Some(LocalDate.of(2020, 6, 15)),
              effectiveStartDate = Some(LocalDate.of(2020, 6, 26)),
              effectiveEndDate = Some(LocalDate.of(2024, 6, 26))
            )
          ),
          lastChangeType = None
        )
      )
    )
  }

  // -------------------------------------

  test("subscriptionToMigrationCurrency is correct (standard)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/standard/subscription.json")
    assertEquals(
      GW2024Migration.subscriptionToCurrency(subscription),
      Some("USD")
    )
  }

  test("subscriptionToMigrationCurrency is correct (ROW-DomesticRatePlan)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/ROW-DomesticRatePlan/subscription.json")
    assertEquals(
      GW2024Migration.subscriptionToCurrency(subscription),
      Some("USD")
    )
  }

  // -------------------------------------

  test("isROW is correct (standard: USD paying from the US)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/standard/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GW2024/standard/account.json")
    assertEquals(
      GW2024Migration.isROW(subscription, account),
      Some(false)
    )
  }

  test("isROW is correct (row: USD paying from Hong Kong)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/ROW-DomesticRatePlan/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GW2024/ROW-DomesticRatePlan/account.json")
    assertEquals(
      GW2024Migration.isROW(subscription, account),
      Some(true)
    )
  }

  test("isROW is correct (row: USD paying from United Arab Emirates)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/ROW-DomesticRatePlan/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GW2024/ROW-DomesticRatePlan/account.json")
    assertEquals(
      GW2024Migration.isROW(subscription, account),
      Some(true)
    )
  }

  // -------------------------------------

  test("subscriptionToExtendedCurrency (standard)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/standard/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GW2024/standard/account.json")
    assertEquals(
      GW2024Migration.subscriptionToExtendedCurrency(subscription, account),
      Some("USD")
    )
  }

  test("subscriptionToExtendedCurrency (ROW-DomesticRatePlan)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/ROW-DomesticRatePlan/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GW2024/ROW-DomesticRatePlan/account.json")
    assertEquals(
      GW2024Migration.subscriptionToExtendedCurrency(subscription, account),
      Some("ROW (USD)")
    )
  }

  // -------------------------------------

  test("subscriptionToBillingPeriod") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/standard/subscription.json")
    assertEquals(
      GW2024Migration.subscriptionToBillingPeriod(subscription),
      Some(Annual)
    )
  }

  // -------------------------------------

  test("getNewPrice (version 2) (standard)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/standard/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GW2024/standard/account.json")
    assertEquals(
      GW2024Migration.getNewPrice(subscription, account),
      Some(BigDecimal(360))
    )
  }

  test("getNewPrice (version 2) (ROW-DomesticRatePlan)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/ROW-DomesticRatePlan/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GW2024/ROW-DomesticRatePlan/account.json")
    assertEquals(
      GW2024Migration.getNewPrice(subscription, account),
      Some(BigDecimal(396))
    )
  }

  // -------------------------------------

  test("priceData (standard)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/standard/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GW2024/standard/account.json")
    assertEquals(
      GW2024Migration.priceData(subscription, account),
      Right(PriceData("USD", BigDecimal(300), BigDecimal(360), "Annual"))
    )
  }

  test("priceData (ROW-DomesticRatePlan)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/ROW-DomesticRatePlan/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/GW2024/ROW-DomesticRatePlan/account.json")
    assertEquals(
      GW2024Migration.priceData(subscription, account),
      Right(PriceData("USD", BigDecimal(300), BigDecimal(396), "Annual"))
    )
  }

  // ------------------------------------

  test("last price rise date") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/standard/subscription.json")
    assertEquals(
      GW2024Migration.subscriptionToLastPriceMigrationDate(subscription),
      Some(LocalDate.of(2020, 6, 8))
    )
  }

  test("last price rise date") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/NotTwoPriceRisesWithinAYear/subscription.json")
    assertEquals(
      GW2024Migration.subscriptionToLastPriceMigrationDate(subscription),
      Some(LocalDate.of(2023, 7, 5))
    )
  }

  // ------------------------------------

  test("StartDate [no price rise within a year of the last price rise] policy (trivial case)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/standard/subscription.json")
    // The last price rise date for this subscription is LocalDate.of(2020, 6, 8)
    // That's more that a year ago at the time these lines are written (2024-03-13)
    // Therefore the StartDates policy function is not going to increase the lower bound if
    // we set the lower bound to today: 2024-03-13.

    val cohortSpec = CohortSpec("GW2024", "", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1))
    assertEquals(
      StartDates.noPriceRiseWithinAYearOfLastPriceRisePolicyUpdate(cohortSpec, subscription, LocalDate.of(2024, 3, 13)),
      LocalDate.of(2024, 3, 13)
    )
  }

  test("StartDate [no price rise within a year of the last price rise] policy (non trivial case)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/NotTwoPriceRisesWithinAYear/subscription.json")
    // The last price rise date for this subscription is LocalDate.of(2023, 7, 5)
    // That's less than a year ago at the time these lines are written (2024-03-13)
    // Therefore the StartDates policy function is going to increase the lower bound to 2024-07-05 (one year after
    // last price rise) if we present it with a previous lower bound of 2024-03-13.

    val cohortSpec = CohortSpec("GW2024", "", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1))
    assertEquals(
      StartDates.noPriceRiseWithinAYearOfLastPriceRisePolicyUpdate(cohortSpec, subscription, LocalDate.of(2024, 3, 13)),
      LocalDate.of(2024, 7, 5)
    )
  }

  // ------------------------------------

  test("EstimationResult") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/standard/subscription.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("Migrations/GW2024/standard/invoice-preview.json")
    val account = Fixtures.accountFromJson("Migrations/GW2024/standard/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Migrations/GW2024/standard/catalogue.json")

    val cohortSpec = CohortSpec("GW2024", "", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 5, 20))

    val startDateLowerBound = LocalDate.of(2025, 1, 1)

    val estimationResult =
      EstimationResult(account, catalogue, subscription, invoicePreview, startDateLowerBound, cohortSpec)

    assertEquals(
      estimationResult,
      Right(
        EstimationData(
          subscriptionName = "SUBSCRIPTION-NUMBER",
          startDate = LocalDate.of(2025, 6, 19),
          currency = "USD",
          oldPrice = BigDecimal(300.0),
          estimatedNewPrice = BigDecimal(360),
          billingPeriod = "Annual"
        )
      )
    )
  }

  // ------------------------------------
  // Notification timetable

  // Note that as part of the test, we purposely set CohortSpec's
  // earliestPriceMigrationStartDate to Jan 1st

  val cohortSpec = CohortSpec("GW2024", "", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1))

  // The startDates in the dynamo table are spread from `2024-05-20` and `2025-05-19`

  // Here are are going to test that the notifications are going to start on April 1st

  // First let's ensure that the GW2024 start of notification window is at 49

  assertEquals(GW2024Migration.maxLeadTime, 49)

  // And that this is the value the Notification Handler itself thinks is right

  assertEquals(NotificationHandler.maxLeadTime(cohortSpec), 49)

  // Then let us make sure that for an items with a start date of exactly `2024-05-20`,
  // April 1st is the first day that we get clearance for notification

  assertEquals(LocalDate.of(2024, 4, 1).plusDays(49), LocalDate.of(2024, 5, 20))

  // ------------------------------------

  test("zUpdate") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/standard/subscription.json")
    assertEquals(
      GW2024Migration.zuoraUpdate(
        subscription: ZuoraSubscription,
        effectiveDate = LocalDate.of(2024, 5, 1),
        oldPrice = BigDecimal(360),
        estimatedNewPrice = BigDecimal(400), // estimated price below 360 * 1.25
        GW2024Migration.priceCap
      ),
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a0fe6619b4b901661aa8e66c1692",
              contractEffectiveDate = LocalDate.of(2024, 5, 1),
              chargeOverrides = List(
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fe6619b4b901661aa8e6811695",
                  billingPeriod = "Annual",
                  price = BigDecimal(400) // the estimated price
                )
              )
            )
          ),
          remove = List(
            RemoveZuoraRatePlan(
              ratePlanId = "8a128d6988b434050188d210ef897dc2",
              contractEffectiveDate = LocalDate.of(2024, 5, 1)
            )
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }

  test("zUpdate (with price capping)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/GW2024/standard/subscription.json")
    assertEquals(
      GW2024Migration.zuoraUpdate(
        subscription: ZuoraSubscription,
        effectiveDate = LocalDate.of(2024, 5, 1),
        oldPrice = BigDecimal(360),
        estimatedNewPrice = BigDecimal(600), // estimated price above 360 * 1.25
        GW2024Migration.priceCap
      ),
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a0fe6619b4b901661aa8e66c1692",
              contractEffectiveDate = LocalDate.of(2024, 5, 1),
              chargeOverrides = List(
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fe6619b4b901661aa8e6811695",
                  billingPeriod = "Annual",
                  price = BigDecimal(450.00) // price capped at 360 * 1.25
                )
              )
            )
          ),
          remove = List(
            RemoveZuoraRatePlan(
              ratePlanId = "8a128d6988b434050188d210ef897dc2",
              contractEffectiveDate = LocalDate.of(2024, 5, 1)
            )
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }
}
