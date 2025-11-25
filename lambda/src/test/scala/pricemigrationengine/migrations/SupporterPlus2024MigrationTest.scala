package pricemigrationengine.migrations

import pricemigrationengine.model._
import upickle.default._

import java.time.LocalDate
import pricemigrationengine.Fixtures

class SupporterPlus2024MigrationTest extends munit.FunSuite {

  // -----------------------------------
  // Cancellation saves

  test("hasCancellationSaveDiscount") {
    val subscriptionNo =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-with-cancellation-save/subscription-no.json")
    val subscriptionYes =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-with-cancellation-save/subscription-yes.json")
    assertEquals(
      SupporterPlus2024Migration.hasCancellationSaveDiscount(subscriptionNo),
      false
    )
    assertEquals(
      SupporterPlus2024Migration.hasCancellationSaveDiscount(subscriptionYes),
      true
    )
  }
  test("cancellationSaveDiscountEffectiveDate") {
    val subscriptionNo =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-with-cancellation-save/subscription-no.json")
    val subscriptionYes =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-with-cancellation-save/subscription-yes.json")
    assertEquals(
      SupporterPlus2024Migration.cancellationSaveDiscountEffectiveDate(subscriptionNo),
      None
    )
    assertEquals(
      SupporterPlus2024Migration.cancellationSaveDiscountEffectiveDate(subscriptionYes),
      Some(LocalDate.of(2024, 7, 5))
    )
  }
  test("isUnderActiveCancellationSavePolicy") {
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-with-cancellation-save/subscription-yes.json")
    // The cancellation dave effective date is LocalDate.of(2024, 7, 5)
    // The cancellation save effective date plus 6 months is LocalDate.of(2025, 1, 5)
    assertEquals(
      SupporterPlus2024Migration.isUnderActiveCancellationSavePolicy(subscription, LocalDate.of(2024, 7, 5)),
      true
    )
    assertEquals(
      SupporterPlus2024Migration.isUnderActiveCancellationSavePolicy(subscription, LocalDate.of(2025, 1, 4)),
      true
    )
    assertEquals(
      SupporterPlus2024Migration.isUnderActiveCancellationSavePolicy(subscription, LocalDate.of(2025, 1, 5)),
      false
    )
    assertEquals(
      SupporterPlus2024Migration.isUnderActiveCancellationSavePolicy(subscription, LocalDate.of(2025, 1, 6)),
      false
    )
  }

  // -----------------------------------
  // Price Grid

  test("Price Grid (Old)") {
    assertEquals(
      SupporterPlus2024Migration.getOldPrice(Monthly, "USD"),
      Some(13.0)
    )
    assertEquals(
      SupporterPlus2024Migration.getOldPrice(Annual, "EUR"),
      Some(95.0)
    )
  }
  test("Price Grid (New)") {
    assertEquals(
      SupporterPlus2024Migration.getNewPrice(Monthly, "USD"),
      Some(15.0)
    )
    assertEquals(
      SupporterPlus2024Migration.getNewPrice(Annual, "EUR"),
      Some(120.0)
    )
  }

  // -----------------------------------
  // Rate plans extraction

  // The monthly is GBP without a contribution [10, 0]
  // The annual is a AUD with contribution [160, 340]
  // sub-without-LastChangeType is a EUR without a contribution [6, 0]
  //   ... is meant to ensure that we know how to extract the rate plan if it doesn't carry a LastChangeType.
  //   The story with LastChangeTypes is
  //     - present with "Add"     : most recently added
  //     - present with "Remove"  : most recently removed
  //     - not present            : most recently added
  // I also modified the base price from the original 10 to 6, to test the price capping.

  test("extracting `Supporter Plus V2` rate plan (monthly)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/monthly/subscription.json")
    val ratePlanCharge1 = ZuoraRatePlanCharge(
      productRatePlanChargeId = "8a128ed885fc6ded018602296af13eba",
      name = "Supporter Plus Monthly Charge",
      number = "C-04648407",
      currency = "GBP",
      price = Some(10.0),
      billingPeriod = Some("Month"),
      chargedThroughDate = Some(LocalDate.of(2024, 9, 30)),
      processedThroughDate = Some(LocalDate.of(2024, 8, 30)),
      specificBillingPeriod = None,
      endDateCondition = Some("Subscription_End"),
      upToPeriodsType = None,
      upToPeriods = None,
      billingDay = Some("ChargeTriggerDay"),
      triggerEvent = Some("CustomerAcceptance"),
      triggerDate = None,
      discountPercentage = None,
      originalOrderDate = Some(LocalDate.of(2023, 10, 1)),
      effectiveStartDate = Some(LocalDate.of(2023, 9, 30)),
      effectiveEndDate = Some(LocalDate.of(2025, 2, 27))
    )
    val ratePlanCharge2 = ZuoraRatePlanCharge(
      productRatePlanChargeId = "8a128d7085fc6dec01860234cd075270",
      name = "Contribution",
      number = "C-04648406",
      currency = "GBP",
      price = Some(0.0),
      billingPeriod = Some("Month"),
      chargedThroughDate = Some(LocalDate.of(2024, 9, 30)),
      processedThroughDate = Some(LocalDate.of(2024, 8, 30)),
      specificBillingPeriod = None,
      endDateCondition = Some("Subscription_End"),
      upToPeriodsType = None,
      upToPeriods = None,
      billingDay = Some("ChargeTriggerDay"),
      triggerEvent = Some("CustomerAcceptance"),
      triggerDate = None,
      discountPercentage = None,
      originalOrderDate = Some(LocalDate.of(2023, 10, 1)),
      effectiveStartDate = Some(LocalDate.of(2023, 9, 30)),
      effectiveEndDate = Some(LocalDate.of(2025, 2, 27))
    )
    assertEquals(
      SupporterPlus2024Migration.getSupporterPlusV2RatePlan(subscription),
      Right(
        ZuoraRatePlan(
          id = "8a12908b8dd07f56018de8f4950923b8",
          productName = "Supporter Plus",
          productRatePlanId = "8a128ed885fc6ded018602296ace3eb8",
          ratePlanName = "Supporter Plus V2 - Monthly",
          ratePlanCharges = List(ratePlanCharge1, ratePlanCharge2),
          lastChangeType = Some("Add")
        )
      )
    )
  }
  test("extracting `Supporter Plus V2` rate plan (annual)") {
    // The original subscription price is 160, the normal old price,
    // but I edited the annual/subscription.json it to 150 to make sure we
    // read the right price from the subscription.

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/annual/subscription.json")
    val ratePlanCharge1 = ZuoraRatePlanCharge(
      productRatePlanChargeId = "8a128ed885fc6ded01860228f7cb3d5f",
      name = "Supporter Plus Annual Charge",
      number = "C-04819663",
      currency = "AUD",
      price = Some(150.0),
      billingPeriod = Some("Annual"),
      chargedThroughDate = Some(LocalDate.of(2024, 11, 11)),
      processedThroughDate = Some(LocalDate.of(2023, 11, 11)),
      specificBillingPeriod = None,
      endDateCondition = Some("Subscription_End"),
      upToPeriodsType = None,
      upToPeriods = None,
      billingDay = Some("ChargeTriggerDay"),
      triggerEvent = Some("CustomerAcceptance"),
      triggerDate = None,
      discountPercentage = None,
      originalOrderDate = Some(LocalDate.of(2023, 11, 26)),
      effectiveStartDate = Some(LocalDate.of(2023, 11, 11)),
      effectiveEndDate = Some(LocalDate.of(2024, 11, 11))
    )
    val ratePlanCharge2 = ZuoraRatePlanCharge(
      productRatePlanChargeId = "8a12892d85fc6df4018602451322287f",
      name = "Contribution",
      number = "C-04819662",
      currency = "AUD",
      price = Some(340.0),
      billingPeriod = Some("Annual"),
      chargedThroughDate = Some(LocalDate.of(2024, 11, 11)),
      processedThroughDate = Some(LocalDate.of(2023, 11, 11)),
      specificBillingPeriod = None,
      endDateCondition = Some("Subscription_End"),
      upToPeriodsType = None,
      upToPeriods = None,
      billingDay = Some("ChargeTriggerDay"),
      triggerEvent = Some("CustomerAcceptance"),
      triggerDate = None,
      discountPercentage = None,
      originalOrderDate = Some(LocalDate.of(2023, 11, 26)),
      effectiveStartDate = Some(LocalDate.of(2023, 11, 11)),
      effectiveEndDate = Some(LocalDate.of(2024, 11, 11))
    )
    assertEquals(
      SupporterPlus2024Migration.getSupporterPlusV2RatePlan(subscription),
      Right(
        ZuoraRatePlan(
          id = "8a12820a8c0ff963018c2504ba045b2f",
          productName = "Supporter Plus",
          productRatePlanId = "8a128ed885fc6ded01860228f77e3d5a",
          ratePlanName = "Supporter Plus V2 - Annual",
          ratePlanCharges = List(ratePlanCharge1, ratePlanCharge2),
          lastChangeType = Some("Add")
        )
      )
    )
  }
  test("extracting `Supporter Plus V2` rate plan (sub-without-LastChangeType)") {
    // The original subscription price is 160, the normal old price,
    // but I edited the annual/subscription.json it to 150 to make sure we
    // read the right price from the subscription.

    val subscription =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-without-LastChangeType/subscription.json")
    val ratePlanCharge1 = ZuoraRatePlanCharge(
      productRatePlanChargeId = "8a128ed885fc6ded018602296af13eba",
      name = "Supporter Plus Monthly Charge",
      number = "C-05466358",
      currency = "EUR",
      price = Some(6.0),
      billingPeriod = Some("Month"),
      chargedThroughDate = Some(LocalDate.of(2024, 9, 5)),
      processedThroughDate = Some(LocalDate.of(2024, 8, 5)),
      specificBillingPeriod = None,
      endDateCondition = Some("Subscription_End"),
      upToPeriodsType = None,
      upToPeriods = None,
      billingDay = Some("ChargeTriggerDay"),
      triggerEvent = Some("CustomerAcceptance"),
      triggerDate = None,
      discountPercentage = None,
      originalOrderDate = Some(LocalDate.of(2024, 4, 5)),
      effectiveStartDate = Some(LocalDate.of(2024, 4, 5)),
      effectiveEndDate = Some(LocalDate.of(2025, 4, 5))
    )
    val ratePlanCharge2 = ZuoraRatePlanCharge(
      productRatePlanChargeId = "8a128d7085fc6dec01860234cd075270",
      name = "Contribution",
      number = "C-05466357",
      currency = "EUR",
      price = Some(3.0),
      billingPeriod = Some("Month"),
      chargedThroughDate = Some(LocalDate.of(2024, 9, 5)),
      processedThroughDate = Some(LocalDate.of(2024, 8, 5)),
      specificBillingPeriod = None,
      endDateCondition = Some("Subscription_End"),
      upToPeriodsType = None,
      upToPeriods = None,
      billingDay = Some("ChargeTriggerDay"),
      triggerEvent = Some("CustomerAcceptance"),
      triggerDate = None,
      discountPercentage = None,
      originalOrderDate = Some(LocalDate.of(2024, 4, 5)),
      effectiveStartDate = Some(LocalDate.of(2024, 4, 5)),
      effectiveEndDate = Some(LocalDate.of(2025, 4, 5))
    )
    assertEquals(
      SupporterPlus2024Migration.getSupporterPlusV2RatePlan(subscription),
      Right(
        ZuoraRatePlan(
          id = "8a12838d8ea33f0f018eaf3a06bd27ea",
          productName = "Supporter Plus",
          productRatePlanId = "8a128ed885fc6ded018602296ace3eb8",
          ratePlanName = "Supporter Plus V2 - Monthly",
          ratePlanCharges = List(ratePlanCharge1, ratePlanCharge2),
          lastChangeType = None
        )
      )
    )
  }

  test("extracting `Supporter Plus V2` rate plan base charge (monthly)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/monthly/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.getSupporterPlusBaseRatePlanCharge(
        subscription.subscriptionNumber,
        SupporterPlus2024Migration.getSupporterPlusV2RatePlan(subscription).toOption.get
      ),
      Right(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "8a128ed885fc6ded018602296af13eba",
          name = "Supporter Plus Monthly Charge",
          number = "C-04648407",
          currency = "GBP",
          price = Some(10.0),
          billingPeriod = Some("Month"),
          chargedThroughDate = Some(LocalDate.of(2024, 9, 30)),
          processedThroughDate = Some(LocalDate.of(2024, 8, 30)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2023, 10, 1)),
          effectiveStartDate = Some(LocalDate.of(2023, 9, 30)),
          effectiveEndDate = Some(LocalDate.of(2025, 2, 27))
        )
      )
    )
  }
  test("extracting `Supporter Plus V2` rate plan base charge (annual)") {
    // The original subscription price is 160, the normal old price,
    // but I edited the annual/subscription.json it to 150 to make sure we
    // read the right price from the subscription.
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/annual/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.getSupporterPlusBaseRatePlanCharge(
        subscription.subscriptionNumber,
        SupporterPlus2024Migration.getSupporterPlusV2RatePlan(subscription).toOption.get
      ),
      Right(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "8a128ed885fc6ded01860228f7cb3d5f",
          name = "Supporter Plus Annual Charge",
          number = "C-04819663",
          currency = "AUD",
          price = Some(150.0),
          billingPeriod = Some("Annual"),
          chargedThroughDate = Some(LocalDate.of(2024, 11, 11)),
          processedThroughDate = Some(LocalDate.of(2023, 11, 11)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2023, 11, 26)),
          effectiveStartDate = Some(LocalDate.of(2023, 11, 11)),
          effectiveEndDate = Some(LocalDate.of(2024, 11, 11))
        )
      )
    )
  }
  test("extracting `Supporter Plus V2` rate plan base charge (sub-without-LastChangeType)") {
    // The original subscription price is 160, the normal old price,
    // but I edited the annual/subscription.json it to 150 to make sure we
    // read the right price from the subscription.

    val subscription =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-without-LastChangeType/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.getSupporterPlusBaseRatePlanCharge(
        subscription.subscriptionNumber,
        SupporterPlus2024Migration.getSupporterPlusV2RatePlan(subscription).toOption.get
      ),
      Right(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "8a128ed885fc6ded018602296af13eba",
          name = "Supporter Plus Monthly Charge",
          number = "C-05466358",
          currency = "EUR",
          price = Some(6.0),
          billingPeriod = Some("Month"),
          chargedThroughDate = Some(LocalDate.of(2024, 9, 5)),
          processedThroughDate = Some(LocalDate.of(2024, 8, 5)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 4, 5)),
          effectiveStartDate = Some(LocalDate.of(2024, 4, 5)),
          effectiveEndDate = Some(LocalDate.of(2025, 4, 5))
        )
      )
    )
  }

  test("extracting `Supporter Plus V2` rate plan contribution charge (monthly)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/monthly/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.getSupporterPlusContributionRatePlanCharge(
        subscription.subscriptionNumber,
        SupporterPlus2024Migration.getSupporterPlusV2RatePlan(subscription).toOption.get
      ),
      Right(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "8a128d7085fc6dec01860234cd075270",
          name = "Contribution",
          number = "C-04648406",
          currency = "GBP",
          price = Some(0.0),
          billingPeriod = Some("Month"),
          chargedThroughDate = Some(LocalDate.of(2024, 9, 30)),
          processedThroughDate = Some(LocalDate.of(2024, 8, 30)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2023, 10, 1)),
          effectiveStartDate = Some(LocalDate.of(2023, 9, 30)),
          effectiveEndDate = Some(LocalDate.of(2025, 2, 27))
        )
      )
    )
  }
  test("extracting `Supporter Plus V2` rate plan contribution charge (annual)") {
    // The original subscription price is 160, the normal old price,
    // but I edited the annual/subscription.json it to 150 to make sure we
    // read the right price from the subscription.
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/annual/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.getSupporterPlusContributionRatePlanCharge(
        subscription.subscriptionNumber,
        SupporterPlus2024Migration.getSupporterPlusV2RatePlan(subscription).toOption.get
      ),
      Right(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "8a12892d85fc6df4018602451322287f",
          name = "Contribution",
          number = "C-04819662",
          currency = "AUD",
          price = Some(340.0),
          billingPeriod = Some("Annual"),
          chargedThroughDate = Some(LocalDate.of(2024, 11, 11)),
          processedThroughDate = Some(LocalDate.of(2023, 11, 11)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2023, 11, 26)),
          effectiveStartDate = Some(LocalDate.of(2023, 11, 11)),
          effectiveEndDate = Some(LocalDate.of(2024, 11, 11))
        )
      )
    )
  }
  test("extracting `Supporter Plus V2` rate plan contribution charge (sub-without-LastChangeType)") {
    // The original subscription price is 160, the normal old price,
    // but I edited the annual/subscription.json it to 150 to make sure we
    // read the right price from the subscription.

    val subscription =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-without-LastChangeType/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.getSupporterPlusContributionRatePlanCharge(
        subscription.subscriptionNumber,
        SupporterPlus2024Migration.getSupporterPlusV2RatePlan(subscription).toOption.get
      ),
      Right(
        ZuoraRatePlanCharge(
          productRatePlanChargeId = "8a128d7085fc6dec01860234cd075270",
          name = "Contribution",
          number = "C-05466357",
          currency = "EUR",
          price = Some(3.0),
          billingPeriod = Some("Month"),
          chargedThroughDate = Some(LocalDate.of(2024, 9, 5)),
          processedThroughDate = Some(LocalDate.of(2024, 8, 5)),
          specificBillingPeriod = None,
          endDateCondition = Some("Subscription_End"),
          upToPeriodsType = None,
          upToPeriods = None,
          billingDay = Some("ChargeTriggerDay"),
          triggerEvent = Some("CustomerAcceptance"),
          triggerDate = None,
          discountPercentage = None,
          originalOrderDate = Some(LocalDate.of(2024, 4, 5)),
          effectiveStartDate = Some(LocalDate.of(2024, 4, 5)),
          effectiveEndDate = Some(LocalDate.of(2025, 4, 5))
        )
      )
    )
  }

  // -----------------------------------
  // Notification helpers sp2024_*

  test("sp2024_previous_base_amount (monthly)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/monthly/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.previousBaseAmount(subscription),
      Right(
        Some(BigDecimal(10))
      )
    )
  }
  test("sp2024_previous_base_amount (annual)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/annual/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.previousBaseAmount(subscription),
      Right(
        Some(BigDecimal(150))
      )
    )
  }
  test("sp2024_previous_base_amount (sub-without-LastChangeType)") {
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-without-LastChangeType/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.previousBaseAmount(subscription),
      Right(
        Some(BigDecimal(6))
      )
    )
  }

  test("sp2024_new_base_amount (monthly)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/monthly/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.newBaseAmount(subscription),
      Right(
        Some(BigDecimal(12))
      )
    )
  }
  test("sp2024_new_base_amount (annual)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/annual/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.newBaseAmount(subscription),
      Right(
        Some(
          BigDecimal(150 * 1.27)
        ) // the new price is 200, but it's too high (with the original 160, we would have been fine)
      )
    )
  }
  test("sp2024_new_base_amount (sub-without-LastChangeType)") {
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-without-LastChangeType/subscription.json")
    // Below we make it explicit that we expect a 27% charge to be applied to the base charge as part of the price rise
    val newBasePrice = 6 * 1.27
    assertEquals(
      SupporterPlus2024Migration.newBaseAmount(subscription),
      Right(Some(BigDecimal(newBasePrice)))
    )
  }

  test("sp2024_contribution_amount (monthly)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/monthly/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.contributionAmount(subscription),
      Right(
        Some(BigDecimal(0))
      )
    )
  }
  test("sp2024_contribution_amount (annual)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/annual/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.contributionAmount(subscription),
      Right(
        Some(BigDecimal(340))
      )
    )
  }
  test("sp2024_contribution_amount (sub-without-LastChangeType)") {
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-without-LastChangeType/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.contributionAmount(subscription),
      Right(
        Some(BigDecimal(3))
      )
    )
  }

  test("sp2024_previous_combined_amount (monthly)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/monthly/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.previousCombinedAmount(subscription),
      Right(
        Some(BigDecimal(10))
      )
    )
  }
  test("sp2024_previous_combined_amount (annual)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/annual/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.previousCombinedAmount(subscription),
      Right(
        Some(BigDecimal(490))
      )
    )
  }
  test("sp2024_previous_combined_amount (sub-without-LastChangeType)") {
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-without-LastChangeType/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.previousCombinedAmount(subscription),
      Right(
        Some(BigDecimal(6 + 3))
      )
    )
  }

  test("sp2024_new_combined_amount (monthly)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/monthly/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.newCombinedAmount(subscription),
      Right(
        Some(BigDecimal(12))
      )
    )
  }
  test("sp2024_new_combined_amount (annual)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/annual/subscription.json")
    val newCombinedAmount = 150 * 1.27 + 340
    assertEquals(
      SupporterPlus2024Migration.newCombinedAmount(subscription),
      Right(
        Some(BigDecimal(newCombinedAmount))
      )
    )
  }
  test("sp2024_new_combined_amount (sub-without-LastChangeType)") {
    val subscription =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-without-LastChangeType/subscription.json")

    // Base price with a capping and the existing contribution
    // And we are doing the rounding because floating point numbers are hard for computers.
    val newCombinedAmount = BigDecimal(6 * 1.27 + 3).setScale(2, BigDecimal.RoundingMode.HALF_UP)

    assertEquals(
      SupporterPlus2024Migration.newCombinedAmount(subscription),
      Right(
        Some(newCombinedAmount)
      )
    )
  }

  // -----------------------------------
  // priceData

  test("priceData (monthly)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/monthly/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.priceData(subscription),
      Right(PriceData("GBP", 10.0, 12.0, "Month"))
    )
  }
  test("priceData (annual)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/annual/subscription.json")

    // The original subscription price is 160, the normal old price,
    // but I edited the annual/subscription.json it to 150 to make sure we
    // read the right price from the subscription.

    // In this case we have the price from the subscription and the target price.

    assertEquals(
      SupporterPlus2024Migration.priceData(subscription),
      Right(PriceData("AUD", 150.0, 200.0, "Annual"))
    )
  }

  // -----------------------------------
  // EstimationResult

  test("EstimationResult (monthly)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/monthly/subscription.json")
    val invoices = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2024/monthly/invoices.json")
    val account = Fixtures.accountFromJson("Migrations/SupporterPlus2024/monthly/account.json")
    // val catalogue = Fixtures.productCatalogueFromJson("Migrations/SupporterPlus2024/monthly/catalogue.json")

    val cohortSpec = CohortSpec("SupporterPlus2024", "", LocalDate.of(2024, 9, 9))

    val startDateLowerBound = LocalDate.of(2024, 9, 9)

    val estimationResult = EstimationResult(account, subscription, invoices, startDateLowerBound, cohortSpec)

    assertEquals(
      estimationResult,
      Right(
        EstimationData(
          subscriptionName = "SUBSCRIPTION-NUMBER",
          amendmentEffectiveDate = LocalDate.of(2024, 9, 30),
          currency = "GBP",
          oldPrice = BigDecimal(10.0),
          estimatedNewPrice = BigDecimal(12.0),
          commsPrice = BigDecimal(12.0),
          billingPeriod = "Month"
        )
      )
    )
  }
  test("EstimationResult (annual)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/annual/subscription.json")
    val invoices = Fixtures.invoiceListFromJson("Migrations/SupporterPlus2024/annual/invoices.json")
    val account = Fixtures.accountFromJson("Migrations/SupporterPlus2024/annual/account.json")
    // val catalogue = Fixtures.productCatalogueFromJson("Migrations/SupporterPlus2024/annual/catalogue.json")

    val cohortSpec = CohortSpec("SupporterPlus2024", "", LocalDate.of(2024, 9, 9))

    val startDateLowerBound = LocalDate.of(2024, 9, 9)

    val estimationResult = EstimationResult(account, subscription, invoices, startDateLowerBound, cohortSpec)

    assertEquals(
      estimationResult,
      Right(
        EstimationData(
          subscriptionName = "SUBSCRIPTION-NUMBER",
          amendmentEffectiveDate = LocalDate.of(2024, 11, 11),
          currency = "AUD",
          oldPrice = BigDecimal(150.0),
          estimatedNewPrice = BigDecimal(200.0),
          commsPrice = BigDecimal(190.50),
          billingPeriod = "Annual"
        )
      )
    )
  }

  // -----------------------------------
  // braze names

  test("brazeName (monthly)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/monthly/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.brazeName(subscription),
      Right("SV_SP2_PriceRise2024")
    )
  }
  test("brazeName (annual)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/annual/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.brazeName(subscription),
      Right("SV_SP2_Contributors_PriceRise2024")
    )
  }

  // -----------------------------------
  // Orders API

  test("amendmentOrderPayload (monthly)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/monthly/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.amendmentOrderPayload(
        orderDate = LocalDate.of(2024, 10, 25),
        accountNumber = "74bff0f2",
        subscriptionNumber = subscription.subscriptionNumber,
        mainChargeEffectDate = LocalDate.of(2024, 11, 27),
        subscription = subscription,
        commsPrice = 12
      ),
      Right(
        ZuoraAmendmentOrderPayload(
          orderDate = LocalDate.of(2024, 10, 25),
          existingAccountNumber = "74bff0f2",
          subscriptions = List(
            ZuoraAmendmentOrderPayloadSubscription(
              subscriptionNumber = "SUBSCRIPTION-NUMBER",
              orderActions = List(
                ZuoraAmendmentOrderPayloadOrderActionRemove(
                  `type` = "RemoveProduct",
                  triggerDates = List(
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "ContractEffective",
                      triggerDate = LocalDate.of(2024, 11, 27)
                    ),
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "ServiceActivation",
                      triggerDate = LocalDate.of(2024, 11, 27)
                    ),
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "CustomerAcceptance",
                      triggerDate = LocalDate.of(2024, 11, 27)
                    )
                  ),
                  removeProduct = ZuoraAmendmentOrderPayloadOrderActionRemoveProduct(
                    ratePlanId = "8a12908b8dd07f56018de8f4950923b8"
                  )
                ),
                ZuoraAmendmentOrderPayloadOrderActionAdd(
                  `type` = "AddProduct",
                  triggerDates = List(
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "ContractEffective",
                      triggerDate = LocalDate.of(2024, 11, 27)
                    ),
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "ServiceActivation",
                      triggerDate = LocalDate.of(2024, 11, 27)
                    ),
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "CustomerAcceptance",
                      triggerDate = LocalDate.of(2024, 11, 27)
                    )
                  ),
                  addProduct = ZuoraAmendmentOrderPayloadOrderActionAddProduct(
                    productRatePlanId = "8a128ed885fc6ded018602296ace3eb8",
                    chargeOverrides = List(
                      ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride(
                        productRatePlanChargeId = "8a128ed885fc6ded018602296af13eba",
                        pricing = Map(
                          "recurringFlatFee" -> Map(
                            "listPrice" -> 12
                          )
                        ),
                        startDate = Map(
                          "triggerEvent" -> "SpecificDate",
                          "specificTriggerDate" -> LocalDate.of(2024, 11, 27).toString
                        )
                      ),
                      ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride(
                        productRatePlanChargeId = "8a128d7085fc6dec01860234cd075270",
                        pricing = Map(
                          "recurringFlatFee" -> Map(
                            "listPrice" -> 0.0
                          )
                        ),
                        startDate = Map(
                          "triggerEvent" -> "SpecificDate",
                          "specificTriggerDate" -> LocalDate.of(2024, 8, 30).toString
                        )
                      )
                    )
                  )
                )
              )
            )
          ),
          processingOptions = ZuoraAmendmentOrderPayloadProcessingOptions(false, false)
        )
      )
    )
  }
  test("amendmentOrderPayload (monthly), with artificial cap") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/monthly/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.amendmentOrderPayload(
        orderDate = LocalDate.of(2024, 10, 25),
        accountNumber = "74bff0f2",
        subscriptionNumber = subscription.subscriptionNumber,
        mainChargeEffectDate = LocalDate.of(2024, 11, 27),
        subscription = subscription,
        commsPrice = 11
      ),
      Right(
        ZuoraAmendmentOrderPayload(
          orderDate = LocalDate.of(2024, 10, 25),
          existingAccountNumber = "74bff0f2",
          subscriptions = List(
            ZuoraAmendmentOrderPayloadSubscription(
              subscriptionNumber = "SUBSCRIPTION-NUMBER",
              orderActions = List(
                ZuoraAmendmentOrderPayloadOrderActionRemove(
                  `type` = "RemoveProduct",
                  triggerDates = List(
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "ContractEffective",
                      triggerDate = LocalDate.of(2024, 11, 27)
                    ),
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "ServiceActivation",
                      triggerDate = LocalDate.of(2024, 11, 27)
                    ),
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "CustomerAcceptance",
                      triggerDate = LocalDate.of(2024, 11, 27)
                    )
                  ),
                  removeProduct = ZuoraAmendmentOrderPayloadOrderActionRemoveProduct(
                    ratePlanId = "8a12908b8dd07f56018de8f4950923b8"
                  )
                ),
                ZuoraAmendmentOrderPayloadOrderActionAdd(
                  `type` = "AddProduct",
                  triggerDates = List(
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "ContractEffective",
                      triggerDate = LocalDate.of(2024, 11, 27)
                    ),
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "ServiceActivation",
                      triggerDate = LocalDate.of(2024, 11, 27)
                    ),
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "CustomerAcceptance",
                      triggerDate = LocalDate.of(2024, 11, 27)
                    )
                  ),
                  addProduct = ZuoraAmendmentOrderPayloadOrderActionAddProduct(
                    productRatePlanId = "8a128ed885fc6ded018602296ace3eb8",
                    chargeOverrides = List(
                      ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride(
                        productRatePlanChargeId = "8a128ed885fc6ded018602296af13eba",
                        pricing = Map(
                          "recurringFlatFee" -> Map(
                            "listPrice" -> 11
                          )
                        ),
                        startDate = Map(
                          "triggerEvent" -> "SpecificDate",
                          "specificTriggerDate" -> LocalDate.of(2024, 11, 27).toString
                        )
                      ),
                      ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride(
                        productRatePlanChargeId = "8a128d7085fc6dec01860234cd075270",
                        pricing = Map(
                          "recurringFlatFee" -> Map(
                            "listPrice" -> 0.0
                          )
                        ),
                        startDate = Map(
                          "triggerEvent" -> "SpecificDate",
                          "specificTriggerDate" -> LocalDate.of(2024, 8, 30).toString
                        )
                      )
                    )
                  )
                )
              )
            )
          ),
          processingOptions = ZuoraAmendmentOrderPayloadProcessingOptions(false, false)
        )
      )
    )
  }
  test("amendmentOrderPayload (annual, with capping)") {
    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/annual/subscription.json")
    assertEquals(
      SupporterPlus2024Migration.amendmentOrderPayload(
        orderDate = LocalDate.of(2024, 9, 9),
        accountNumber = "d4a7d0af",
        subscriptionNumber = subscription.subscriptionNumber,
        mainChargeEffectDate = LocalDate.of(2023, 11, 11),
        subscription = subscription,
        commsPrice = 190.5
      ),
      Right(
        ZuoraAmendmentOrderPayload(
          orderDate = LocalDate.of(2024, 9, 9),
          existingAccountNumber = "d4a7d0af",
          subscriptions = List(
            ZuoraAmendmentOrderPayloadSubscription(
              subscriptionNumber = "SUBSCRIPTION-NUMBER",
              orderActions = List(
                ZuoraAmendmentOrderPayloadOrderActionRemove(
                  `type` = "RemoveProduct",
                  triggerDates = List(
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "ContractEffective",
                      triggerDate = LocalDate.of(2023, 11, 11)
                    ),
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "ServiceActivation",
                      triggerDate = LocalDate.of(2023, 11, 11)
                    ),
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "CustomerAcceptance",
                      triggerDate = LocalDate.of(2023, 11, 11)
                    )
                  ),
                  removeProduct = ZuoraAmendmentOrderPayloadOrderActionRemoveProduct(
                    ratePlanId = "8a12820a8c0ff963018c2504ba045b2f"
                  )
                ),
                ZuoraAmendmentOrderPayloadOrderActionAdd(
                  `type` = "AddProduct",
                  triggerDates = List(
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "ContractEffective",
                      triggerDate = LocalDate.of(2023, 11, 11)
                    ),
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "ServiceActivation",
                      triggerDate = LocalDate.of(2023, 11, 11)
                    ),
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "CustomerAcceptance",
                      triggerDate = LocalDate.of(2023, 11, 11)
                    )
                  ),
                  addProduct = ZuoraAmendmentOrderPayloadOrderActionAddProduct(
                    productRatePlanId = "8a128ed885fc6ded01860228f77e3d5a",
                    chargeOverrides = List(
                      ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride(
                        productRatePlanChargeId = "8a128ed885fc6ded01860228f7cb3d5f",
                        pricing = Map(
                          "recurringFlatFee" -> Map(
                            "listPrice" -> 190.50
                          )
                        ),
                        startDate = Map(
                          "triggerEvent" -> "SpecificDate",
                          "specificTriggerDate" -> LocalDate.of(2023, 11, 11).toString
                        )
                      ),
                      ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride(
                        productRatePlanChargeId = "8a12892d85fc6df4018602451322287f",
                        pricing = Map(
                          "recurringFlatFee" -> Map(
                            "listPrice" -> 340.0
                          )
                        ),
                        startDate = Map(
                          "triggerEvent" -> "SpecificDate",
                          "specificTriggerDate" -> LocalDate.of(2023, 11, 11).toString
                        )
                      )
                    )
                  )
                )
              )
            )
          ),
          processingOptions = ZuoraAmendmentOrderPayloadProcessingOptions(false, false)
        )
      )
    )
  }
  test("Correct JSON serialisation for ZuoraAmendmentOrderPayloadOrderActionRemove") {

    // Here we are testing the natural serialization of the whole payload, by upickle.
    // The purpose is to ensure that the model hierarchy leads to the same essential JSON structure.
    // See comment cada56ad for more details on the structure and information about how we sanitize the JSON
    // before we send it to Zuora.

    val data = ZuoraAmendmentOrderPayloadOrderActionRemove(
      `type` = "RemoveProduct",
      triggerDates = List(
        ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
          name = "ContractEffective",
          triggerDate = LocalDate.of(2024, 10, 11)
        ),
        ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
          name = "ServiceActivation",
          triggerDate = LocalDate.of(2024, 10, 11)
        ),
        ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
          name = "CustomerAcceptance",
          triggerDate = LocalDate.of(2024, 10, 11)
        )
      ),
      removeProduct = ZuoraAmendmentOrderPayloadOrderActionRemoveProduct(
        ratePlanId = "8a12820a8c0ff963018c2504ba045b2f"
      )
    )
    assertEquals(
      write(data, indent = 2),
      """{
        |  "$type": "ZuoraAmendmentOrderPayloadOrderActionRemove",
        |  "type": "RemoveProduct",
        |  "triggerDates": [
        |    {
        |      "name": "ContractEffective",
        |      "triggerDate": "2024-10-11"
        |    },
        |    {
        |      "name": "ServiceActivation",
        |      "triggerDate": "2024-10-11"
        |    },
        |    {
        |      "name": "CustomerAcceptance",
        |      "triggerDate": "2024-10-11"
        |    }
        |  ],
        |  "removeProduct": {
        |    "ratePlanId": "8a12820a8c0ff963018c2504ba045b2f"
        |  }
        |}""".stripMargin
    )
  }
  test("Correct JSON serialisation for ZuoraAmendmentOrderPayloadOrderActionAdd") {

    // Here we are testing the natural serialization of the whole payload, by upickle.
    // The purpose is to ensure that the model hierarchy leads to the same essential JSON structure.
    // See comment cada56ad for more details on the structure and information about how we sanitize the JSON
    // before we send it to Zuora.

    val data = ZuoraAmendmentOrderPayloadOrderActionAdd(
      `type` = "AddProduct",
      triggerDates = List(
        ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
          name = "ContractEffective",
          triggerDate = LocalDate.of(2024, 10, 11)
        ),
        ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
          name = "ServiceActivation",
          triggerDate = LocalDate.of(2024, 10, 11)
        ),
        ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
          name = "CustomerAcceptance",
          triggerDate = LocalDate.of(2024, 10, 11)
        )
      ),
      addProduct = ZuoraAmendmentOrderPayloadOrderActionAddProduct(
        productRatePlanId = "8a128ed885fc6ded01860228f77e3d5a",
        chargeOverrides = List(
          ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride(
            productRatePlanChargeId = "8a128ed885fc6ded01860228f7cb3d5f",
            pricing = Map(
              "recurringFlatFee" -> Map(
                "listPrice" -> 190.50
              )
            ),
            startDate = Map(
              "triggerEvent" -> "SpecificDate",
              "specificTriggerDate" -> LocalDate.of(2024, 11, 27).toString
            )
          ),
          ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride(
            productRatePlanChargeId = "8a12892d85fc6df4018602451322287f",
            pricing = Map(
              "recurringFlatFee" -> Map(
                "listPrice" -> 340.0
              )
            ),
            startDate = Map(
              "triggerEvent" -> "SpecificDate",
              "specificTriggerDate" -> LocalDate.of(2024, 11, 27).toString
            )
          )
        )
      )
    )
    assertEquals(
      write(data, indent = 2),
      """{
        |  "$type": "ZuoraAmendmentOrderPayloadOrderActionAdd",
        |  "type": "AddProduct",
        |  "triggerDates": [
        |    {
        |      "name": "ContractEffective",
        |      "triggerDate": "2024-10-11"
        |    },
        |    {
        |      "name": "ServiceActivation",
        |      "triggerDate": "2024-10-11"
        |    },
        |    {
        |      "name": "CustomerAcceptance",
        |      "triggerDate": "2024-10-11"
        |    }
        |  ],
        |  "addProduct": {
        |    "productRatePlanId": "8a128ed885fc6ded01860228f77e3d5a",
        |    "chargeOverrides": [
        |      {
        |        "productRatePlanChargeId": "8a128ed885fc6ded01860228f7cb3d5f",
        |        "pricing": {
        |          "recurringFlatFee": {
        |            "listPrice": 190.5
        |          }
        |        },
        |        "startDate": {
        |          "triggerEvent": "SpecificDate",
        |          "specificTriggerDate": "2024-11-27"
        |        }
        |      },
        |      {
        |        "productRatePlanChargeId": "8a12892d85fc6df4018602451322287f",
        |        "pricing": {
        |          "recurringFlatFee": {
        |            "listPrice": 340
        |          }
        |        },
        |        "startDate": {
        |          "triggerEvent": "SpecificDate",
        |          "specificTriggerDate": "2024-11-27"
        |        }
        |      }
        |    ]
        |  }
        |}""".stripMargin
    )
  }
  test(
    "Correct JSON serialisation for ZuoraAmendmentOrderPayload using SupporterPlus2024Migration.amendmentOrdersPayload"
  ) {

    // Here we are testing the natural serialization of the whole payload, by upickle.
    // The purpose is to ensure that the model hierarchy leads to the same essential JSON structure.
    // See comment cada56ad for more details on the structure and information about how we sanitize the JSON
    // before we send it to Zuora.

    val data = SupporterPlus2024Migration.amendmentOrdersPayload(
      orderDate = LocalDate.of(2024, 10, 24),
      accountNumber = "A01955911",
      subscriptionNumber = "A-S02019224",
      mainChargeEffectDate = LocalDate.of(2024, 11, 26),
      contributionChargeEffectDate = LocalDate.of(2024, 11, 26),
      removeRatePlanId = "8a128e208bdd4251018c0d5050970bd9",
      productRatePlanId = "8a128ed885fc6ded018602296ace3eb8",
      existingBaseProductRatePlanChargeId = "8a128ed885fc6ded018602296af13eba",
      existingContributionRatePlanChargeId = "8a128d7085fc6dec01860234cd075270",
      newBaseAmount = 15,
      newContributionAmount = 0
    )
    assertEquals(
      write(data, indent = 2),
      """{
        |  "orderDate": "2024-10-24",
        |  "existingAccountNumber": "A01955911",
        |  "subscriptions": [
        |    {
        |      "subscriptionNumber": "A-S02019224",
        |      "orderActions": [
        |        {
        |          "$type": "ZuoraAmendmentOrderPayloadOrderActionRemove",
        |          "type": "RemoveProduct",
        |          "triggerDates": [
        |            {
        |              "name": "ContractEffective",
        |              "triggerDate": "2024-11-26"
        |            },
        |            {
        |              "name": "ServiceActivation",
        |              "triggerDate": "2024-11-26"
        |            },
        |            {
        |              "name": "CustomerAcceptance",
        |              "triggerDate": "2024-11-26"
        |            }
        |          ],
        |          "removeProduct": {
        |            "ratePlanId": "8a128e208bdd4251018c0d5050970bd9"
        |          }
        |        },
        |        {
        |          "$type": "ZuoraAmendmentOrderPayloadOrderActionAdd",
        |          "type": "AddProduct",
        |          "triggerDates": [
        |            {
        |              "name": "ContractEffective",
        |              "triggerDate": "2024-11-26"
        |            },
        |            {
        |              "name": "ServiceActivation",
        |              "triggerDate": "2024-11-26"
        |            },
        |            {
        |              "name": "CustomerAcceptance",
        |              "triggerDate": "2024-11-26"
        |            }
        |          ],
        |          "addProduct": {
        |            "productRatePlanId": "8a128ed885fc6ded018602296ace3eb8",
        |            "chargeOverrides": [
        |              {
        |                "productRatePlanChargeId": "8a128ed885fc6ded018602296af13eba",
        |                "pricing": {
        |                  "recurringFlatFee": {
        |                    "listPrice": 15
        |                  }
        |                },
        |                "startDate": {
        |                  "triggerEvent": "SpecificDate",
        |                  "specificTriggerDate": "2024-11-26"
        |                }
        |              },
        |              {
        |                "productRatePlanChargeId": "8a128d7085fc6dec01860234cd075270",
        |                "pricing": {
        |                  "recurringFlatFee": {
        |                    "listPrice": 0
        |                  }
        |                },
        |                "startDate": {
        |                  "triggerEvent": "SpecificDate",
        |                  "specificTriggerDate": "2024-11-26"
        |                }
        |              }
        |            ]
        |          }
        |        }
        |      ]
        |    }
        |  ],
        |  "processingOptions": {
        |    "runBilling": false,
        |    "collectPayment": false
        |  }
        |}""".stripMargin
    )
  }
  test("amendmentOrderPayload (annual), two billing dates") {

    // This subscription is interesting it was added to the fixtures in August 2025 as an example
    // of a Supporter Plus subscription where the main charge and the extra contribution are both
    // annuals but happen at different dates. (Main charge: August 7th, Contribution: July 28th).

    // We are making a correction. Before this correction the engine would read one of the items from the
    // invoice preview, determine the active rate plan, get the first charge billing date
    // and use that for everything.

    // We want to correct this by having the main charge and the contribution charge happening
    // at their natural dates.

    // nb: No remaining subscription in this migration suffer from this problem. This one fixture
    // is not part of the migration. We are just using it to practice thinking more precisely about dates.

    val subscription = Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/two-billing-dates/subscription.json")

    val ratePlan = SupporterPlus2024Migration.getSupporterPlusV2RatePlan(subscription).toOption.get

    val contributionRatePlanCharge = SupporterPlus2024Migration
      .getSupporterPlusContributionRatePlanCharge(
        "SUBSCRIPTION-NUMBER",
        ratePlan: ZuoraRatePlan
      )
      .toOption
      .get

    // Here we are making sure that the contribution date is correctly read from the fixture
    assertEquals(contributionRatePlanCharge.processedThroughDate.get, LocalDate.of(2025, 7, 28))

    val mainRatePlanEffectDate = LocalDate.of(2025, 8, 7)
    // August start date for the main charge, as read from the subscription.
    // The july date will be read from the subscription itself

    assertEquals(
      SupporterPlus2024Migration.amendmentOrderPayload(
        orderDate = LocalDate.of(2025, 8, 5),
        accountNumber = "d0b03dda", // random string
        subscriptionNumber = subscription.subscriptionNumber,
        mainChargeEffectDate = mainRatePlanEffectDate,
        subscription = subscription,
        commsPrice = 101.0
      ),
      Right(
        ZuoraAmendmentOrderPayload(
          orderDate = LocalDate.of(2025, 8, 5),
          existingAccountNumber = "d0b03dda",
          subscriptions = List(
            ZuoraAmendmentOrderPayloadSubscription(
              subscriptionNumber = "SUBSCRIPTION-NUMBER",
              orderActions = List(
                ZuoraAmendmentOrderPayloadOrderActionRemove(
                  `type` = "RemoveProduct",
                  triggerDates = List(
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "ContractEffective",
                      triggerDate = LocalDate.of(2025, 8, 7)
                    ),
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "ServiceActivation",
                      triggerDate = LocalDate.of(2025, 8, 7)
                    ),
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "CustomerAcceptance",
                      triggerDate = LocalDate.of(2025, 8, 7)
                    )
                  ),
                  removeProduct = ZuoraAmendmentOrderPayloadOrderActionRemoveProduct(
                    ratePlanId = "8a129c6098448d8301984f22e29970f9"
                  )
                ),
                ZuoraAmendmentOrderPayloadOrderActionAdd(
                  `type` = "AddProduct",
                  triggerDates = List(
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "ContractEffective",
                      triggerDate = LocalDate.of(2025, 8, 7)
                    ),
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "ServiceActivation",
                      triggerDate = LocalDate.of(2025, 8, 7)
                    ),
                    ZuoraAmendmentOrderPayloadOrderActionTriggerDate(
                      name = "CustomerAcceptance",
                      triggerDate = LocalDate.of(2025, 8, 7)
                    )
                  ),
                  addProduct = ZuoraAmendmentOrderPayloadOrderActionAddProduct(
                    productRatePlanId = "8a128ed885fc6ded01860228f77e3d5a",
                    chargeOverrides = List(
                      ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride(
                        productRatePlanChargeId = "8a128ed885fc6ded01860228f7cb3d5f",
                        pricing = Map(
                          "recurringFlatFee" -> Map(
                            "listPrice" -> 101.0 // estimatedNewPrice
                          )
                        ),
                        startDate = Map(
                          "triggerEvent" -> "SpecificDate",
                          "specificTriggerDate" -> LocalDate.of(2025, 8, 7).toString
                        )
                      ),
                      ZuoraAmendmentOrderPayloadOrderActionAddProductChargeOverride(
                        productRatePlanChargeId = "8a12892d85fc6df4018602451322287f",
                        pricing = Map(
                          "recurringFlatFee" -> Map(
                            "listPrice" -> 5.0 // Existing contribution
                          )
                        ),
                        startDate = Map(
                          "triggerEvent" -> "SpecificDate",
                          "specificTriggerDate" -> LocalDate.of(2025, 7, 28).toString
                        )
                      )
                    )
                  )
                )
              )
            )
          ),
          processingOptions = ZuoraAmendmentOrderPayloadProcessingOptions(false, false)
        )
      )
    )
  }
}
