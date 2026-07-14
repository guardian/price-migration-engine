package pricemigrationengine.migrations

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.Fixtures

import java.time.LocalDate

class SupporterPlus2026Test extends munit.FunSuite {
  test("monthliesOverSixWeeks date calculations") {
    assertEquals(
      SupporterPlus2026Migration.monthliesOverSixWeeks(LocalDate.of(2026, 7, 20), Annual),
      LocalDate.of(2026, 7, 20)
    )
    assertEquals(
      SupporterPlus2026Migration.monthliesOverSixWeeks(LocalDate.of(2026, 9, 1), Monthly),
      LocalDate.of(2026, 9, 1)
    )
    assert(
      List(LocalDate.of(2026, 7, 21), LocalDate.of(2026, 8, 21))
        .contains(SupporterPlus2026Migration.monthliesOverSixWeeks(LocalDate.of(2026, 7, 21), Monthly))
    )
  }

  test("price data (1)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/01/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/01/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/01/invoice-preview.json")

    assertEquals(
      SupporterPlus2026Migration.priceData(subscription, invoicePreview),
      Right(PriceData("USD", BigDecimal(15.0), BigDecimal(18.0), "Month"))
    )
  }

  test("standard 1 year policy (1)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/01/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/01/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/01/invoice-preview.json")

    val lowerBound = LocalDate.of(2026, 7, 2)

    // The subscription was acquired on 2026-06-30, we expect the lowerbound to be updated to 2027-06-30

    assertEquals(
      AmendmentEffectiveDateCalculator.noPriceRiseDuringSubscriptionFirstYearPolicyUpdate(lowerBound, subscription),
      LocalDate.of(2027, 6, 30)
    )
  }

  test("standard 1 year policy (2)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/02/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/02/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/02/invoice-preview.json")

    val lowerBound = LocalDate.of(2026, 7, 2)

    // The subscription was acquired on 2 Aug 2017, we expect the lowerbound to not change

    assertEquals(
      AmendmentEffectiveDateCalculator.noPriceRiseDuringSubscriptionFirstYearPolicyUpdate(lowerBound, subscription),
      LocalDate.of(2026, 7, 2)
    )
  }

  test("get active discounts") {
    /*
      05:

      Annually EUR
      Acquired on 26 Nov 2016
      Got that one to test the one year discount policy.

      The discount has dates
        "effectiveStartDate": "2024-12-30"
        "effectiveEndDate": "2025-12-30"

      The supporter plus rate plan has dates
        "effectiveStartDate": "2024-12-20"
        "effectiveEndDate": "2026-12-20"
     */

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/05/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/05/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/05/invoice-preview.json")

    assertEquals(
      SI2025Extractions.getActiveDiscountsPossiblyAfterEffectiveEndDate(subscription),
      List(
        ZuoraRatePlan(
          id = "8a128f399b353104019b39fc32a763dc",
          productName = "Discounts",
          productRatePlanId = "8a128adf8b64bcfd018b6b6fdc7674f5",
          ratePlanName = "Cancellation Save Discount - 25% off for 12 months",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "8a129c288b64d798018b6b711d8845d0",
              name = "Cancellation Save Discount - 25% off for 12 months",
              number = "C-06219793",
              currency = "EUR",
              price = None,
              billingPeriod = Some("Month"),
              chargedThroughDate = Some(LocalDate.of(2025, 1, 20)),
              processedThroughDate = Some(LocalDate.of(2024, 12, 30)),
              specificBillingPeriod = None,
              endDateCondition = Some("Fixed_Period"),
              upToPeriodsType = Some("Months"),
              upToPeriods = Some(12),
              billingDay = Some("ChargeTriggerDay"),
              triggerEvent = Some("CustomerAcceptance"),
              triggerDate = None,
              discountPercentage = Some(25.0),
              originalOrderDate = Some(LocalDate.of(2024, 12, 15)),
              effectiveStartDate = Some(LocalDate.of(2024, 12, 30)),
              effectiveEndDate = Some(LocalDate.of(2025, 12, 30))
            )
          ),
          lastChangeType = Some("Add")
        )
      )
    )
  }

  test("SupporterPlus2026Migration.annualWithDiscountOneYearPolicy (1)") {
    /*
      05:

      Annually EUR
      Acquired on 26 Nov 2016
      Got that one to test the one year discount policy.

      The discount has dates
        "effectiveStartDate": "2024-12-30"
        "effectiveEndDate": "2025-12-30"

      The supporter plus rate plan has dates
        "effectiveStartDate": "2024-12-20"
        "effectiveEndDate": "2026-12-20"
     */

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/05/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/05/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/05/invoice-preview.json")

    // The discounts end on 2025-12-30, we expect the lower bound to rise to 2026-12-30

    assertEquals(
      SupporterPlus2026Migration.annualWithDiscountOneYearPolicy(LocalDate.of(2026, 7, 2), subscription),
      LocalDate.of(2026, 12, 30)
    )
  }

  test("SupporterPlus2026Migration.annualWithDiscountOneYearPolicy (2)") {
    /*
      06:

      Annually EUR
      Acquired on 26 Nov 2016

      The discount has dates
        "effectiveStartDate": "2020-12-30",
        "effectiveEndDate": "2021-12-30",

      The supporter plus rate plan has dates
        "effectiveStartDate": "2024-12-20"
        "effectiveEndDate": "2026-12-20"
     */

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/06/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/06/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/06/invoice-preview.json")

    // The discounts endded on 2021-12-30, the lower bound should remain the same

    assertEquals(
      SupporterPlus2026Migration.annualWithDiscountOneYearPolicy(LocalDate.of(2026, 7, 2), subscription),
      LocalDate.of(2026, 7, 2)
    )
  }

  test("priceData (1)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/01/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/01/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/01/invoice-preview.json")

    assertEquals(
      SupporterPlus2026Migration.priceData(subscription, invoicePreview),
      Right(PriceData("USD", BigDecimal(15.0), BigDecimal(18.0), "Month"))
    )
  }

  test("priceData (2) [01-variant1-non-zero-contribution]") {
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/01-variant1-non-zero-contribution/subscription.json")
    val account =
      Fixtures.accountFromJson("Migrations/SupporterPlus2026/01-variant1-non-zero-contribution/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson(
      "Migrations/SupporterPlus2026/01-variant1-non-zero-contribution/invoice-preview.json"
    )

    val ratePlan = SI2025RateplanFromSub
      .uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(
        subscription,
        LocalDate.of(2026, 7, 3)
      )
      .get

    // Here we want to highly the difference between the standard
    // SI2025Extractions.determineOldPrice(ratePlan: ZuoraRatePlan)
    // and
    // SupporterPlus2026Migration.determineOldPrice(ratePlan: ZuoraRatePlan)

    assertEquals(
      SI2025Extractions.determineOldPrice(ratePlan),
      BigDecimal(104.0) // 89.0 + 15
    )

    assertEquals(
      SupporterPlus2026Migration.determineOldPrice(ratePlan),
      BigDecimal(15.0)
    )

    // Here we need to have the same price data as the original version, thereby showing that we have
    // picked up only the main charge

    assertEquals(
      SupporterPlus2026Migration.priceData(subscription, invoicePreview),
      Right(PriceData("USD", BigDecimal(15.0), BigDecimal(18.0), "Month"))
    )
  }

  test("priceData (3)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/02/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/02/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/02/invoice-preview.json")

    assertEquals(
      SupporterPlus2026Migration.priceData(subscription, invoicePreview),
      Right(PriceData("GBP", BigDecimal(12.0), BigDecimal(14.0), "Month"))
    )
  }

  test("priceData (4)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/03/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/03/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/03/invoice-preview.json")

    // Here we do not need to do the discount variants.

    assertEquals(
      SupporterPlus2026Migration.priceData(subscription, invoicePreview),
      Right(PriceData("EUR", BigDecimal(120.0), BigDecimal(140.0), "Annual"))
    )
  }

  test("subscriptionToContributionAmount (1)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/01/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/01/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/01/invoice-preview.json")

    assertEquals(
      SupporterPlus2026Migration.subscriptionToContributionAmount(subscription),
      Some(BigDecimal(0.0))
    )
  }

  test("subscriptionToContributionAmount (2) [01-variant1-non-zero-contribution]") {
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/01-variant1-non-zero-contribution/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/01-variant1-non-zero-contribution/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/01-variant1-non-zero-contribution/invoice-preview.json"

    assertEquals(
      SupporterPlus2026Migration.subscriptionToContributionAmount(subscription),
      Some(BigDecimal(89.0))
    )
  }

  test("extractEmailExtraAttributes (1)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/01/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/01/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/01/invoice-preview.json")

    val cohortSpec = CohortSpec("SupporterPlus2026", LocalDate.of(2026, 7, 1))

    // Note that we are defining the CohortItem only with the attributes we need
    // That particular value would not happen in the wild.
    val cohortItem = CohortItem(
      subscriptionName = subscription.subscriptionNumber,
      processingStage = CohortTableFilter.SalesforcePriceRiseCreationComplete,
      oldPrice = Some(BigDecimal(12.0)), // we can pass any amount here, doesn't have to be consistent with subscription
      commsPrice = Some(14.0), // we can pass any amount here, doesn't have to be consistent with subscription
    )

    val contribution = BigDecimal(0.0) // 01 carries 0.0

    assertEquals(
      SupporterPlus2026Migration.extractEmailExtraAttributes(cohortSpec, cohortItem, subscription),
      Some(
        SP2026EmailExtraAttributes(
          contribution.toString(),
          "12.0",
          "14.0"
        )
      )
    )
  }

  test("extractEmailExtraAttributes (2) [01-variant1-non-zero-contribution]") {
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/01-variant1-non-zero-contribution/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/01-variant1-non-zero-contribution/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/01-variant1-non-zero-contribution/invoice-preview.json"

    val cohortSpec = CohortSpec("SupporterPlus2026", LocalDate.of(2026, 7, 1))

    // Note that we are defining the CohortItem only with the attributes we need
    // That particular value would not happen in the wild.
    val cohortItem = CohortItem(
      subscriptionName = subscription.subscriptionNumber,
      processingStage = CohortTableFilter.SalesforcePriceRiseCreationComplete,
      oldPrice = Some(BigDecimal(12.0)), // we can pass any amount here, doesn't have to be consistent with subscription
      commsPrice = Some(14.0), // we can pass any amount here, doesn't have to be consistent with subscription
    )

    val contribution = BigDecimal(89.0) // 01 carries 89.0

    assertEquals(
      SupporterPlus2026Migration.extractEmailExtraAttributes(cohortSpec, cohortItem, subscription),
      Some(
        SP2026EmailExtraAttributes(
          contribution.toString(),
          "101.0",
          "103.0"
        )
      )
    )
  }

  test(
    "SupporterPlus2026Migration.oneYearSinceLastProductSwitchPolicyGetProductNameRatePlanNamePairOpt (1) simple case, one rate plan"
  ) {

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/01/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/01/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/01/invoice-preview.json")

    val today = LocalDate.of(2026, 7, 10)

    // Testing that we correctly read the pair in a very simple case

    assertEquals(
      SupporterPlus2026Migration
        .oneYearSinceLastProductSwitchPolicyGetProductNameRatePlanNamePairOpt(today, subscription),
      Some(("Supporter Plus", "Supporter Plus V2 - Monthly"))
    )
  }

  test(
    "SupporterPlus2026Migration.oneYearSinceLastProductSwitchPolicyGetProductNameRatePlanNamePairOpt (2) more complex case, multiple products"
  ) {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/02/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/02/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/02/invoice-preview.json")

    val today = LocalDate.of(2026, 7, 10)

    // We have multiple products, some expired on this sub, here the determination is easy, because
    // we are essentially reading the uniquely determined active rate plan

    assertEquals(
      SupporterPlus2026Migration
        .oneYearSinceLastProductSwitchPolicyGetProductNameRatePlanNamePairOpt(today, subscription),
      Some(("Supporter Plus", "Supporter Plus V2 - Monthly"))
    )
  }

  test(
    "SupporterPlus2026Migration.oneYearSinceLastProductSwitchPolicyGetRatePlans (1) simple case, one rate plan"
  ) {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/01/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/01/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/01/invoice-preview.json")

    val today = LocalDate.of(2026, 7, 10)

    // Here we want to check that we retrieve just one rate plan, with the correct names

    assertEquals(
      SupporterPlus2026Migration
        .oneYearSinceLastProductSwitchPolicyGetRatePlans(
          subscription,
          "Supporter Plus",
          "Supporter Plus V2 - Monthly"
        )
        .size,
      1
    )

    // We also want to check that if we are submitting incorrect names, then we get empty list

    assertEquals(
      SupporterPlus2026Migration
        .oneYearSinceLastProductSwitchPolicyGetRatePlans(
          subscription,
          "Supporter Minus",
          "Supporter Plus V2 - Monthly"
        )
        .size,
      0
    )
  }

  test(
    "SupporterPlus2026Migration.oneYearSinceLastProductSwitchPolicyGetRatePlans (2) more complex case, multiple products"
  ) {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/02/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/02/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/02/invoice-preview.json")

    val today = LocalDate.of(2026, 7, 10)

    // Here, we retrieve 2 rate plans, because although just one is active with the corresponding names
    // (the one compatible with today's date)

    // We have the following on the sub
    // (Supporter Plus, "Supporter Plus V2 - Monthly") from 2023-07-30 to 2024-10-30
    // (Supporter Plus, "Supporter Plus V2 - Monthly") from 2024-10-30 to 2026-08-02

    assertEquals(
      SupporterPlus2026Migration
        .oneYearSinceLastProductSwitchPolicyGetRatePlans(
          subscription,
          "Supporter Plus",
          "Supporter Plus V2 - Monthly"
        )
        .size,
      2
    )

    // We also want to check that if we are submitting incorrect names, then we get empty list
    assertEquals(
      SupporterPlus2026Migration
        .oneYearSinceLastProductSwitchPolicyGetRatePlans(
          subscription,
          "Supporter Minus",
          "Supporter Plus V2 - Monthly"
        )
        .size,
      0
    )
  }

  test(
    "SupporterPlus2026Migration.oneYearSinceLastProductSwitchPolicyRatePlansToLowerBoundEffectiveDate (1) simple case, one rate plan"
  ) {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/01/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/01/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/01/invoice-preview.json")

    val today = LocalDate.of(2026, 7, 10)

    val ratePlans = SupporterPlus2026Migration
      .oneYearSinceLastProductSwitchPolicyGetRatePlans(
        subscription,
        "Supporter Plus",
        "Supporter Plus V2 - Monthly"
      )

    // Here we have one rate plan and the following dates
    // "effectiveStartDate": "2026-06-30",
    // "effectiveEndDate": "2027-06-30",

    // We expect the lowerbound to raise to { 2026-06-30 + 1 year } == 2027-06-30
    // In this case, we get the same value as the standard 1 year policy by the way

    assertEquals(
      SupporterPlus2026Migration
        .oneYearSinceLastProductSwitchPolicyRatePlansToLowerBoundEffectiveDate(
          today,
          ratePlans
        ),
      LocalDate.of(2027, 6, 30)
    )
  }

  test(
    "SupporterPlus2026Migration.oneYearSinceLastProductSwitchPolicyRatePlansToLowerBoundEffectiveDate (2) more complex case, multiple products"
  ) {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/02/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/02/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/02/invoice-preview.json")

    // We have the following on the sub
    // (Supporter Plus, "Supporter Plus V2 - Monthly") from 2023-07-30 to 2024-10-30
    // (Supporter Plus, "Supporter Plus V2 - Monthly") from 2024-10-30 to 2026-08-02

    val ratePlans = SupporterPlus2026Migration
      .oneYearSinceLastProductSwitchPolicyGetRatePlans(
        subscription,
        "Supporter Plus",
        "Supporter Plus V2 - Monthly"
      )

    // Here the reference date is the old start date, meaning 2023-07-30
    // That plus 1 year is 2024-07-30
    // With today at LocalDate.of(2026, 7, 10), we are getting today

    assertEquals(
      SupporterPlus2026Migration
        .oneYearSinceLastProductSwitchPolicyRatePlansToLowerBoundEffectiveDate(
          LocalDate.of(2026, 7, 10),
          ratePlans
        ),
      LocalDate.of(2026, 7, 10)
    )

    // But with today at LocalDate.of(2023, 1, 1), just to see the computation raise to 2024-07-30,
    // have

    assertEquals(
      SupporterPlus2026Migration
        .oneYearSinceLastProductSwitchPolicyRatePlansToLowerBoundEffectiveDate(
          LocalDate.of(2023, 1, 1),
          ratePlans
        ),
      LocalDate.of(2024, 7, 30)
    )
  }

  test(
    "SupporterPlus2026Migration.oneYearSinceLastProductSwitchPolicy (1) simple case, one rate plan"
  ) {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/01/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/01/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/01/invoice-preview.json")

    // Here we have one rate plan and the following dates
    // "effectiveStartDate": "2026-06-30",
    // "effectiveEndDate": "2027-06-30",

    val today = LocalDate.of(2026, 7, 10)

    assertEquals(
      SupporterPlus2026Migration
        .oneYearSinceLastProductSwitchPolicy(
          today,
          subscription
        ),
      LocalDate.of(2027, 6, 30) // one year later the effectiveStartDate
    )
  }

  test(
    "SupporterPlus2026Migration.oneYearSinceLastProductSwitchPolicy (2) more complex case, multiple products"
  ) {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/02/subscription.json")
    // val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/02/account.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/02/invoice-preview.json")

    // We have the following on the sub
    // (Supporter Plus, "Supporter Plus V2 - Monthly") from 2023-07-30 to 2024-10-30
    // (Supporter Plus, "Supporter Plus V2 - Monthly") from 2024-10-30 to 2026-08-02

    val today = LocalDate.of(2026, 7, 10)

    assertEquals(
      SupporterPlus2026Migration
        .oneYearSinceLastProductSwitchPolicy(
          today,
          subscription
        ),
      LocalDate.of(2026, 7, 10) // policy didn't change anything
    )
  }

  test("EstimationResult (1)") {

    // Monthly,USD
    // Acquired in 30 Jun 2026, used to test the basic 1 year policy.

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/01/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/01/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/01/invoice-preview.json")

    val amendmentEffectiveDateLowerBound = LocalDate.of(2026, 8, 1)
    val cohortSpec = CohortSpec("SupporterPlus2026", LocalDate.of(2026, 8, 19))

    assertEquals(
      EstimationResult.apply(account, subscription, invoicePreview, amendmentEffectiveDateLowerBound, cohortSpec),
      Right(
        EstimationData(
          subscriptionName = subscription.subscriptionNumber,
          amendmentEffectiveDate = LocalDate.of(2026, 8, 30),
          currency = "USD",
          oldPrice = BigDecimal(15.0),
          estimatedNewPrice = BigDecimal(18.0),
          commsPrice = BigDecimal(18.0),
          billingPeriod = "Month"
        )
      )
    )
  }

  test("EstimationResult (2)") {

    // Monthly,USD
    // Acquired in 30 Jun 2026, used to test the basic 1 year policy.

    // This is a copy of [01] with an extra contribution artificially set to 89.0.
    // This is to test that the old price is picked up accurately.

    val subscription =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/01-variant1-non-zero-contribution/subscription.json")
    val account =
      Fixtures.accountFromJson("Migrations/SupporterPlus2026/01-variant1-non-zero-contribution/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson(
      "Migrations/SupporterPlus2026/01-variant1-non-zero-contribution/invoice-preview.json"
    )

    val amendmentEffectiveDateLowerBound = LocalDate.of(2026, 8, 1)
    val cohortSpec = CohortSpec("SupporterPlus2026", LocalDate.of(2026, 8, 19))

    // Here the Estimation is identical to that of '[01]', because the extra contribution amount
    // is not affecting it

    assertEquals(
      EstimationResult.apply(account, subscription, invoicePreview, amendmentEffectiveDateLowerBound, cohortSpec),
      Right(
        EstimationData(
          subscriptionName = subscription.subscriptionNumber,
          amendmentEffectiveDate = LocalDate.of(2026, 8, 30),
          currency = "USD",
          oldPrice = BigDecimal(15.0),
          estimatedNewPrice = BigDecimal(18.0),
          commsPrice = BigDecimal(18.0),
          billingPeriod = "Month"
        )
      )
    )
  }

  test("amendment payload (1)") {

    // Monthly,USD
    // Acquired in 30 Jun 2026, used to test the basic 1 year policy.

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/01/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/01/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/01/invoice-preview.json")

    val cohortItem: CohortItem = CohortItem(
      subscriptionName = "subscriptionNumber",
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
      currency = Some("USD"),

      // Pre migration price
      oldPrice = Some(BigDecimal(15.0)),

      // Price derived from the Estimation step, without capping
      estimatedNewPrice = Some(BigDecimal(18.0)),

      // Price (with possible capping) used in the communication to the user and sent to Salesforce
      commsPrice = Some(BigDecimal(18.0)),

      //
      billingPeriod = Some("Month")
    )

    val payload = SupporterPlus2026Migration.amendmentOrderPayload(
      cohortItem,
      LocalDate.of(2026, 7, 13), // order date
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2026, 8, 30), // effect date
      subscription, // Zuora subscription
      BigDecimal(18.0), // comms price
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2026-07-13",
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
             |                            "triggerDate": "2026-08-30"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2026-08-30"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2026-08-30"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a128f5e9f01fcf4019f1af23b1d7f59"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2026-08-30"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2026-08-30"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2026-08-30"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "8a128ed885fc6ded018602296ace3eb8",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "8a128ed885fc6ded018602296af13eba",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 18
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "8a128d7085fc6dec01860234cd075270",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 0
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

  test("amendment payload (2)") {

    // Monthly,USD
    // Acquired in 30 Jun 2026, used to test the basic 1 year policy.
    // This is a copy of [01] with an extra contribution artificially set to 89.0.
    // This is to test that the old price is picked up accurately.

    val subscription =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/01-variant1-non-zero-contribution/subscription.json")
    val account =
      Fixtures.accountFromJson("Migrations/SupporterPlus2026/01-variant1-non-zero-contribution/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson(
      "Migrations/SupporterPlus2026/01-variant1-non-zero-contribution/invoice-preview.json"
    )

    val cohortItem: CohortItem = CohortItem(
      subscriptionName = "subscriptionNumber",
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
      currency = Some("USD"),

      // Pre migration price
      oldPrice = Some(BigDecimal(15.0)),

      // Price derived from the Estimation step, without capping
      estimatedNewPrice = Some(BigDecimal(18.0)),

      // Price (with possible capping) used in the communication to the user and sent to Salesforce
      commsPrice = Some(BigDecimal(18.0)),

      //
      billingPeriod = Some("Month")
    )

    val payload = SupporterPlus2026Migration.amendmentOrderPayload(
      cohortItem,
      LocalDate.of(2026, 7, 13), // order date
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2026, 8, 30), // effect date
      subscription, // Zuora subscription
      BigDecimal(18.0), // comms price
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2026-07-13",
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
             |                            "triggerDate": "2026-08-30"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2026-08-30"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2026-08-30"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a128f5e9f01fcf4019f1af23b1d7f59"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2026-08-30"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2026-08-30"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2026-08-30"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "8a128ed885fc6ded018602296ace3eb8",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "8a128ed885fc6ded018602296af13eba",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 18
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Month"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "8a128d7085fc6dec01860234cd075270",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 89
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

  test("amendment payload (3)") {

    /*
    06:

    Annually EUR
    Acquired on 26 Nov 2016

    Extra contribution: 211.0

    The discount has dates
      "effectiveStartDate": "2020-12-30",
      "effectiveEndDate": "2021-12-30",

    The supporter plus rate plan has dates
      "effectiveStartDate": "2024-12-20"
      "effectiveEndDate": "2026-12-20"
     */

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2026/06/subscription.json")
    val account = Fixtures.accountFromJson("Migrations/SupporterPlus2026/06/account.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2026/06/invoice-preview.json")

    val cohortItem: CohortItem = CohortItem(
      subscriptionName = "subscriptionNumber",
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
      currency = Some("EUR"),

      // Pre migration price
      oldPrice = Some(BigDecimal(120.0)),

      // Price derived from the Estimation step, without capping
      estimatedNewPrice = Some(BigDecimal(150.0)),

      // Price (with possible capping) used in the communication to the user and sent to Salesforce
      commsPrice = Some(BigDecimal(150.0)),

      //
      billingPeriod = Some("Annual")
    )

    val payload = SupporterPlus2026Migration.amendmentOrderPayload(
      cohortItem,
      LocalDate.of(2026, 7, 14), // order date
      "accountNumber",
      "subscriptionNumber",
      LocalDate.of(2026, 9, 1), // effect date
      subscription, // Zuora subscription
      BigDecimal(150.0), // comms price
      invoicePreview,
    )

    assertEquals(
      payload,
      Right(
        ujson.read(
          s"""{
             |    "orderDate": "2026-07-14",
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
             |                            "triggerDate": "2026-09-01"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2026-09-01"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2026-09-01"
             |                        }
             |                    ],
             |                    "removeProduct": {
             |                        "ratePlanId": "8a128f399b353104019b39fc32be63f8"
             |                    }
             |                },
             |                {
             |                    "type": "AddProduct",
             |                    "triggerDates": [
             |                        {
             |                            "name": "ContractEffective",
             |                            "triggerDate": "2026-09-01"
             |                        },
             |                        {
             |                            "name": "ServiceActivation",
             |                            "triggerDate": "2026-09-01"
             |                        },
             |                        {
             |                            "name": "CustomerAcceptance",
             |                            "triggerDate": "2026-09-01"
             |                        }
             |                    ],
             |                    "addProduct": {
             |                        "productRatePlanId": "8a128ed885fc6ded01860228f77e3d5a",
             |                        "chargeOverrides": [
             |                            {
             |                                "productRatePlanChargeId": "8a128ed885fc6ded01860228f7cb3d5f",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 150
             |                                    }
             |                                },
             |                                "billing": {
             |                                    "billingPeriod": "Annual"
             |                                }
             |                            },
             |                            {
             |                                "productRatePlanChargeId": "8a12892d85fc6df4018602451322287f",
             |                                "pricing": {
             |                                    "recurringFlatFee": {
             |                                        "listPrice": 211
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
