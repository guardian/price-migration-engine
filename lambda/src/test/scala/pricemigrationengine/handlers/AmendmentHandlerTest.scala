package pricemigrationengine.handlers

import pricemigrationengine.model.{ZuoraRatePlanCharge, ZuoraSubscriptionUpdate, _}

import java.time.LocalDate
import pricemigrationengine.Fixtures
import pricemigrationengine.handlers.AmendmentHandler.checkExpirationTiming
import pricemigrationengine.model.CohortTableFilter.NotificationSendDateWrittenToSalesforce

class AmendmentHandlerTest extends munit.FunSuite {
  test("Membership2023 Amendment Batch1") {

    // This test is going to be used as discovery to reverse engineer the way `updateOfRatePlansToCurrent` works.

    val account = Fixtures.accountFromJson("Membership2023/Batch1/GBP/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Membership2023/Batch1/GBP/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Membership2023/Batch1/GBP/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Membership2023/Batch1/GBP/invoice-preview.json")

    // The effective date must be a billing date
    val effectiveDate = LocalDate.of(2023, 5, 13) // monthly on the 13th

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

    val update = Membership2023.updateOfRatePlansToCurrent_Monthlies(
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
  test("Membership2023 Amendment Batch3 / annuals / GBP") {

    // This test is going to be used as discovery to reverse engineer the way `updateOfRatePlansToCurrent` works.

    val account = Fixtures.accountFromJson("Membership2023/Batch3/GBP/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Membership2023/Batch3/GBP/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Membership2023/Batch3/GBP/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Membership2023/Batch3/GBP/invoice-preview.json")

    // The effective date must be a billing date
    val effectiveDate = LocalDate.of(2024, 1, 20) // 2024-01-20

    // ZuoraInvoiceItem.items finds the invoice items corresponding to that billing date
    val invoiceItems = ZuoraInvoiceItem.items(invoicePreview, subscription, effectiveDate)
    // value: List(ZuoraInvoiceItem(SUBSCRIPTION-NUMBER,2024-01-20,C-00821078,Supporter))

    val invoiceItemsCheck =
      List(ZuoraInvoiceItem("SUBSCRIPTION-NUMBER", LocalDate.of(2024, 1, 20), "C-00821078", "Supporter"))
    assertEquals(invoiceItems, invoiceItemsCheck)

    // Now that we have an invoice Item, which carries a chargeNumber, in this case "C-00821078", we can use it to
    // extract rate plan charges to get a collection of ZuoraRatePlanCharges

    val ratePlanCharges = ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItems.head).toSeq
    val ratePlanChargesCheck = List(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a0fb4c5481db014c69f4a2013bbf",
        name = "Supporter Membership - Annual",
        number = "C-00821078",
        currency = "GBP",
        price = Some(49.0),
        billingPeriod = Some("Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 1, 20)),
        processedThroughDate = Some(LocalDate.of(2023, 1, 20)),
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
          id = "8a129a6385cd03d80185cdd67d0c114a",
          productName = "Supporter",
          productRatePlanId = "2c92a0fb4c5481db014c69f4a1e03bbd",
          ratePlanName = "Non Founder Supporter - annual",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "2c92a0fb4c5481db014c69f4a2013bbf",
              name = "Supporter Membership - Annual",
              number = "C-00821078",
              currency = "GBP",
              price = Some(49.0),
              billingPeriod = Some("Annual"),
              chargedThroughDate = Some(LocalDate.of(2024, 1, 20)),
              processedThroughDate = Some(LocalDate.of(2023, 1, 20)),
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

    val update = Membership2023.updateOfRatePlansToCurrent_Annuals(
      subscription,
      invoicePreview,
      effectiveDate: LocalDate
    )

    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(AddZuoraRatePlan("8a129ce886834fa90186a20c3ee70b6a", LocalDate.of(2024, 1, 20))),
          remove = List(RemoveZuoraRatePlan("8a129a6385cd03d80185cdd67d0c114a", LocalDate.of(2024, 1, 20))),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }
  test("Membership2023 Amendment Batch3 / annuals / USD") {

    // This test is going to be used as discovery to reverse engineer the way `updateOfRatePlansToCurrent` works.

    val account = Fixtures.accountFromJson("Membership2023/Batch3/USD/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Membership2023/Batch3/USD/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Membership2023/Batch3/USD/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Membership2023/Batch3/USD/invoice-preview.json")

    // The effective date must be a billing date
    val effectiveDate = LocalDate.of(2024, 1, 20) // 2024-01-20

    // ZuoraInvoiceItem.items finds the invoice items corresponding to that billing date
    val invoiceItems = ZuoraInvoiceItem.items(invoicePreview, subscription, effectiveDate)
    // value: List(ZuoraInvoiceItem(SUBSCRIPTION-NUMBER,2024-01-20,C-00821078,Supporter))

    val invoiceItemsCheck =
      List(ZuoraInvoiceItem("SUBSCRIPTION-NUMBER", LocalDate.of(2024, 1, 20), "C-00821076", "Supporter"))
    assertEquals(invoiceItems, invoiceItemsCheck)

    // Now that we have an invoice Item, which carries a chargeNumber, in this case "C-00821078", we can use it to
    // extract rate plan charges to get a collection of ZuoraRatePlanCharges

    val ratePlanCharges = ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItems.head).toSeq
    val ratePlanChargesCheck = List(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a0fb4c5481db014c69f4a2013bbf",
        name = "Supporter Membership - Annual",
        number = "C-00821076",
        currency = "USD",
        price = Some(69.0),
        billingPeriod = Some("Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 1, 20)),
        processedThroughDate = Some(LocalDate.of(2023, 1, 20)),
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
          id = "8a129a6385cd03d80185cddc4afd1a2d",
          productName = "Supporter",
          productRatePlanId = "2c92a0fb4c5481db014c69f4a1e03bbd",
          ratePlanName = "Non Founder Supporter - annual",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "2c92a0fb4c5481db014c69f4a2013bbf",
              name = "Supporter Membership - Annual",
              number = "C-00821076",
              currency = "USD",
              price = Some(69.0),
              billingPeriod = Some("Annual"),
              chargedThroughDate = Some(LocalDate.of(2024, 1, 20)),
              processedThroughDate = Some(LocalDate.of(2023, 1, 20)),
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

    val update = Membership2023.updateOfRatePlansToCurrent_Annuals(
      subscription,
      invoicePreview,
      effectiveDate: LocalDate
    )

    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(AddZuoraRatePlan("8a129ce886834fa90186a20c3ee70b6a", LocalDate.of(2024, 1, 20))),
          remove = List(RemoveZuoraRatePlan("8a129a6385cd03d80185cddc4afd1a2d", LocalDate.of(2024, 1, 20))),
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
  test("SupporterPlus2023V1V2 Amendment (monthly standard)") {

    val account = Fixtures.accountFromJson("SupporterPlus2023V1V2/monthly-standard/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("SupporterPlus2023V1V2/monthly-standard/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("SupporterPlus2023V1V2/monthly-standard/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("SupporterPlus2023V1V2/monthly-standard/invoice-preview.json")

    // The effective date must be a billing date
    // Here we get the next billing date according to the invoice preview.
    val effectiveDate = LocalDate.of(2023, 8, 1) // annual: 2023-08-01

    // ZuoraInvoiceItem.items finds the invoice items corresponding to that billing date
    val invoiceItems = ZuoraInvoiceItem.items(invoicePreview, subscription, effectiveDate)

    val invoiceItemsCheck =
      List(ZuoraInvoiceItem("SUBSCRIPTION-NUMBER", LocalDate.of(2023, 8, 1), "C-04240692", "Supporter Plus"))
    assertEquals(invoiceItems, invoiceItemsCheck)

    // Now that we have an invoice Item, which carries a chargeNumber, in this case "C-04417974", we can use it to
    // extract rate plan charges to get a collection of ZuoraRatePlanCharges

    val ratePlanCharges = ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItems.head).toSeq
    val ratePlanChargesCheck = List(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "8a12865b8219d9b401822106194e64e3",
        name = "Supporter Plus Monthly Charge",
        number = "C-04240692",
        currency = "GBP",
        price = Some(10.0),
        billingPeriod = Some("Month"),
        chargedThroughDate = Some(LocalDate.of(2023, 8, 1)),
        processedThroughDate = Some(LocalDate.of(2023, 7, 1)),
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
          id = "8a129d388962fe000189679374ce42a1",
          productName = "Supporter Plus",
          productRatePlanId = "8a12865b8219d9b401822106192b64dc",
          ratePlanName = "Supporter Plus Monthly",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "8a12865b8219d9b401822106194e64e3",
              name = "Supporter Plus Monthly Charge",
              number = "C-04240692",
              currency = "GBP",
              price = Some(10.0),
              billingPeriod = Some("Month"),
              chargedThroughDate = Some(LocalDate.of(2023, 8, 1)),
              processedThroughDate = Some(LocalDate.of(2023, 7, 1)),
              specificBillingPeriod = None,
              endDateCondition = Some("Subscription_End"),
              upToPeriodsType = None,
              upToPeriods = None,
              billingDay = Some("ChargeTriggerDay"),
              triggerEvent = Some("CustomerAcceptance"),
              triggerDate = None,
              discountPercentage = None
            )
          ),
          lastChangeType = Some("Add")
        )
      )
    )

    val item =
      CohortItem(
        subscriptionName = "SUBSCRIPTION-NUMBER",
        processingStage = NotificationSendDateWrittenToSalesforce,
        startDate = Some(LocalDate.of(2024, 7, 2)),
        currency = Some("USD"),
        oldPrice = Some(BigDecimal(120)),
        estimatedNewPrice = Some(BigDecimal(120)),
        billingPeriod = Some("Annual")
      )

    val update = SupporterRevenue2023V1V2.updateOfRatePlansToCurrent(
      item,
      subscription,
      invoicePreview,
      effectiveDate: LocalDate
    )

    // note: the product rate plan id we are removing it: 8a12865b8219d9b40182210618a464ba, but the subscription can
    // have a slightly different "effective" rate plan with it's own id, in this case 8a128432890171d1018914866bee0e7f

    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(AddZuoraRatePlan("8a128ed885fc6ded01860228f77e3d5a", LocalDate.of(2023, 8, 1))),
          remove = List(RemoveZuoraRatePlan("8a129d388962fe000189679374ce42a1", LocalDate.of(2023, 8, 1))),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }
  test("SupporterPlus2023V1V2 Amendment (annual standard)") {

    val account = Fixtures.accountFromJson("SupporterPlus2023V1V2/annual-standard/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("SupporterPlus2023V1V2/annual-standard/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("SupporterPlus2023V1V2/annual-standard/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("SupporterPlus2023V1V2/annual-standard/invoice-preview.json")

    // The effective date must be a billing date
    // Here we get the next billing date according to the invoice preview.
    val effectiveDate = LocalDate.of(2024, 7, 2) // annual: 2024-07-02

    // ZuoraInvoiceItem.items finds the invoice items corresponding to that billing date
    val invoiceItems = ZuoraInvoiceItem.items(invoicePreview, subscription, effectiveDate)

    val invoiceItemsCheck =
      List(ZuoraInvoiceItem("SUBSCRIPTION-NUMBER", LocalDate.of(2024, 7, 2), "C-04417974", "Supporter Plus"))
    assertEquals(invoiceItems, invoiceItemsCheck)

    // Now that we have an invoice Item, which carries a chargeNumber, in this case "C-04417974", we can use it to
    // extract rate plan charges to get a collection of ZuoraRatePlanCharges

    val ratePlanCharges = ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItems.head).toSeq
    val ratePlanChargesCheck = List(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "8a12865b8219d9b40182210618c664c1",
        name = "Supporter Plus Annual Charge",
        number = "C-04417974",
        currency = "USD",
        price = Some(120.0),
        billingPeriod = Some("Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 7, 2)),
        processedThroughDate = Some(LocalDate.of(2023, 7, 2)),
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
          id = "8a128432890171d1018914866bee0e7f",
          productName = "Supporter Plus",
          productRatePlanId = "8a12865b8219d9b40182210618a464ba",
          ratePlanName = "Supporter Plus Annual",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "8a12865b8219d9b40182210618c664c1",
              name = "Supporter Plus Annual Charge",
              number = "C-04417974",
              currency = "USD",
              price = Some(120.0),
              billingPeriod = Some("Annual"),
              chargedThroughDate = Some(LocalDate.of(2024, 7, 2)),
              processedThroughDate = Some(LocalDate.of(2023, 7, 2)),
              specificBillingPeriod = None,
              endDateCondition = Some("Subscription_End"),
              upToPeriodsType = None,
              upToPeriods = None,
              billingDay = Some("ChargeTriggerDay"),
              triggerEvent = Some("CustomerAcceptance"),
              triggerDate = None,
              discountPercentage = None
            )
          ),
          lastChangeType = None
        )
      )
    )

    val item =
      CohortItem(
        subscriptionName = "SUBSCRIPTION-NUMBER",
        processingStage = NotificationSendDateWrittenToSalesforce,
        startDate = Some(LocalDate.of(2024, 7, 2)),
        currency = Some("USD"),
        oldPrice = Some(BigDecimal(120)),
        estimatedNewPrice = Some(BigDecimal(120)),
        billingPeriod = Some("Annual")
      )

    val update = SupporterRevenue2023V1V2.updateOfRatePlansToCurrent(
      item,
      subscription,
      invoicePreview,
      effectiveDate: LocalDate
    )

    // note: the product rate plan id we are removing it: 8a12865b8219d9b40182210618a464ba, but the subscription can
    // have a slightly different "effective" rate plan with it's own id, in this case 8a128432890171d1018914866bee0e7f

    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(AddZuoraRatePlan("8a128ed885fc6ded01860228f77e3d5a", LocalDate.of(2024, 7, 2))),
          remove = List(RemoveZuoraRatePlan("8a128432890171d1018914866bee0e7f", LocalDate.of(2024, 7, 2))),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }
}
