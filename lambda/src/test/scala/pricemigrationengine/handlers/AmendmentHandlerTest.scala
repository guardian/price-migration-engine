package pricemigrationengine.handlers

import pricemigrationengine.model.{ZuoraRatePlanCharge, ZuoraSubscriptionUpdate, _}

import java.time.LocalDate
import pricemigrationengine.Fixtures
import pricemigrationengine.handlers.AmendmentHandler.checkExpirationTiming
import pricemigrationengine.model.CohortTableFilter.NotificationSendDateWrittenToSalesforce

class AmendmentHandlerTest extends munit.FunSuite {
  test("Membership2023 Amendment") {

    // This test is going to be used as discovery to reverse engineer the way `updateOfRatePlansToCurrent` works.

    val account = Fixtures.accountFromJson("Membership2023/Batch1/GBP/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Membership2023/Batch1/GBP/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Membership2023/Batch1/GBP/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Membership2023/Batch1/GBP/invoice-preview.json")
    val effectiveDate = LocalDate.of(2023, 5, 13) // monthly on the 13th
    val priceCorrectionFactor = 1

    // The effective date must be a billing date

    // ZuoraInvoiceItem.items finds the invoice items corresponding to that billing date

    val invoiceItems = ZuoraInvoiceItem.items(invoicePreview, subscription, effectiveDate)
    val invoiceItemsCheck =
      List(ZuoraInvoiceItem("SUBSCRIPTION-NUMBER", LocalDate.of(2023, 5, 13), "C-00400194", "Supporter"))
    assertEquals(invoiceItems, invoiceItemsCheck)

    // Now that we have an invoice Item, which carries a chargeNumber, in this case "C-00400194", we can use it to
    // extract rate plan charges to get a collection of ZuoraRatePlanCharges

    val ratePlanCharges = ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItems.head).toSeq
    val ratePlanChargesCheck = List(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a0f94c547592014c69f5b1204f80",
        name = "Supporter Membership - Monthly",
        number = "C-00400194",
        currency = "GBP",
        price = Some(5.0),
        billingPeriod = Some("Month"),
        chargedThroughDate = Some(LocalDate.of(2023, 4, 13)),
        processedThroughDate = Some(LocalDate.of(2023, 3, 13)),
        specificBillingPeriod = None,
        endDateCondition = Some("Subscription_End"),
        upToPeriodsType = None,
        upToPeriods = None,
        billingDay = Some("ChargeTriggerDay"),
        triggerEvent = Some("ContractEffective"),
        triggerDate = None,
        discountPercentage = None
      )
    )

    assertEquals(
      ratePlanCharges,
      ratePlanChargesCheck
    )

    // And now that we have a ZuoraRatePlanCharge, we can use it to find a matching rate plans.

    val ratePlans = ZuoraRatePlan.ratePlan(subscription, ratePlanCharges.head).toSeq

    assertEquals(
      ratePlans,
      List(
        ZuoraRatePlan(
          id = "8a1298708461aecd01846fbbdb9f543d",
          productName = "Supporter",
          productRatePlanId = "2c92a0f94c547592014c69f5b0ff4f7e",
          ratePlanName = "Non Founder Supporter - monthly",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "2c92a0f94c547592014c69f5b1204f80",
              name = "Supporter Membership - Monthly",
              number = "C-00400194",
              currency = "GBP",
              price = Some(5.0),
              billingPeriod = Some("Month"),
              chargedThroughDate = Some(LocalDate.of(2023, 4, 13)),
              processedThroughDate = Some(LocalDate.of(2023, 3, 13)),
              specificBillingPeriod = None,
              endDateCondition = Some("Subscription_End"),
              upToPeriodsType = None,
              upToPeriods = None,
              billingDay = Some("ChargeTriggerDay"),
              triggerEvent = Some("ContractEffective"),
              triggerDate = None,
              discountPercentage = None
            )
          ),
          lastChangeType = None
        )
      )
    )

    val update = ZuoraSubscriptionUpdate.updateOfRatePlansToCurrentMembership2023(
      subscription,
      invoicePreview,
      effectiveDate: LocalDate
    )

    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(AddZuoraRatePlan("8a1287c586832d250186a2040b1548fe", LocalDate.of(2023, 5, 13))),
          remove = List(RemoveZuoraRatePlan("8a1298708461aecd01846fbbdb9f543d", LocalDate.of(2023, 5, 13))),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }
  test("Check subscription's end date versus the cohort item's start (price increase) date") {
    // Stage 1
    val subscription1 = Fixtures.subscriptionFromJson("Membership2023/Batch1/GBP/subscription.json")
    val item1 =
      CohortItem("SUBSCRIPTION-NUMBER", NotificationSendDateWrittenToSalesforce, Some(LocalDate.of(2023, 4, 10)))
    // subscription1.termEndDate is 2023-11-09
    // item's startDate is LocalDate.of(2023, 4, 10)
    // This is the good case
    assertEquals(checkExpirationTiming(item1, subscription1), Right(()))

    // Stage 2
    val subscription2 = Fixtures.subscriptionFromJson("Membership2023/Batch1/GBP/subscription.json")
    val item2 = CohortItem("SUBSCRIPTION-NUMBER", NotificationSendDateWrittenToSalesforce, None)
    // item's startDate is None, this triggers the AmendmentDataFailure
    assertEquals(checkExpirationTiming(item2, subscription2).isLeft, true)

    // Stage 3
    val subscription3 = Fixtures.subscriptionFromJson("Membership2023/Batch1/GBP/subscription.json")
    val item3 =
      CohortItem("SUBSCRIPTION-NUMBER", NotificationSendDateWrittenToSalesforce, Some(LocalDate.of(2024, 4, 1)))
    // subscription3.termEndDate is 2023-11-09
    // item's startDate is LocalDate.of(2024, 1, 1)
    // This triggers the ExpiringSubscriptionFailure case
    assertEquals(checkExpirationTiming(item3, subscription3).isLeft, true)
  }
}
