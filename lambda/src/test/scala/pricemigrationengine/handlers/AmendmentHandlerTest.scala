package pricemigrationengine.handlers

import pricemigrationengine.model._

import java.time.{LocalDate, ZoneOffset}
import pricemigrationengine.Fixtures
import pricemigrationengine.migrations.Membership2023Migration
import pricemigrationengine.model.CohortTableFilter.NotificationSendDateWrittenToSalesforce

class AmendmentHandlerTest extends munit.FunSuite {
  test("Membership2023 Amendment Batch1") {

    // This test is going to be used as discovery to reverse engineer the way `updateOfRatePlansToCurrent` works.

    val account = Fixtures.accountFromJson("Migrations/Membership2023/Batch1/GBP/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Migrations/Membership2023/Batch1/GBP/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2023/Batch1/GBP/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2023/Batch1/GBP/invoice-preview.json")

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
        discountPercentage = None,
        originalOrderDate = Some(LocalDate.of(2016, 11, 13))
      )
    )

    assertEquals(
      ratePlanCharges,
      ratePlanChargesCheck
    )

    // And now that we have a ZuoraRatePlanCharge, we can use it to find a matching rate plans.

    val ratePlans = ZuoraRatePlan.ratePlanChargeToMatchingRatePlan(subscription, ratePlanCharges.head).toSeq

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
              discountPercentage = None,
              originalOrderDate = Some(LocalDate.of(2016, 11, 13))
            )
          ),
          lastChangeType = None
        )
      )
    )

    val update = Membership2023Migration.zuoraUpdate_Monthlies(
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

    val account = Fixtures.accountFromJson("Migrations/Membership2023/Batch3/GBP/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Migrations/Membership2023/Batch3/GBP/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2023/Batch3/GBP/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2023/Batch3/GBP/invoice-preview.json")

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
        discountPercentage = None,
        originalOrderDate = Some(LocalDate.of(2017, 1, 20))
      )
    )

    assertEquals(
      ratePlanCharges,
      ratePlanChargesCheck
    )

    // And now that we have a ZuoraRatePlanCharge, we can use it to find a matching rate plans.

    val ratePlans = ZuoraRatePlan.ratePlanChargeToMatchingRatePlan(subscription, ratePlanCharges.head).toSeq

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
              discountPercentage = None,
              originalOrderDate = Some(LocalDate.of(2017, 1, 20))
            )
          ),
          lastChangeType = None
        )
      )
    )

    val update = Membership2023Migration.zuoraUpdate_Annuals(
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

    val account = Fixtures.accountFromJson("Migrations/Membership2023/Batch3/USD/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("Migrations/Membership2023/Batch3/USD/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("Migrations/Membership2023/Batch3/USD/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("Migrations/Membership2023/Batch3/USD/invoice-preview.json")

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
        discountPercentage = None,
        originalOrderDate = Some(LocalDate.of(2017, 1, 20))
      )
    )

    assertEquals(
      ratePlanCharges,
      ratePlanChargesCheck
    )

    // And now that we have a ZuoraRatePlanCharge, we can use it to find a matching rate plans.

    val ratePlans = ZuoraRatePlan.ratePlanChargeToMatchingRatePlan(subscription, ratePlanCharges.head).toSeq

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
              discountPercentage = None,
              originalOrderDate = Some(LocalDate.of(2017, 1, 20))
            )
          ),
          lastChangeType = None
        )
      )
    )

    val update = Membership2023Migration.zuoraUpdate_Annuals(
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
        discountPercentage = None,
        originalOrderDate = Some(LocalDate.of(2023, 4, 1))
      )
    )

    assertEquals(
      ratePlanCharges,
      ratePlanChargesCheck
    )

    // And now that we have a ZuoraRatePlanCharge, we can use it to find a matching rate plans.

    val ratePlans = ZuoraRatePlan.ratePlanChargeToMatchingRatePlan(subscription, ratePlanCharges.head).toSeq

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
              discountPercentage = None,
              originalOrderDate = Some(LocalDate.of(2023, 4, 1))
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

    val update = SupporterPlus2023V1V2Migration.zuoraUpdate(
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

  test("SupporterPlus2023V1V2 Amendment (monthly contribution)") {

    val account = Fixtures.accountFromJson("SupporterPlus2023V1V2/monthly-contribution/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("SupporterPlus2023V1V2/monthly-contribution/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("SupporterPlus2023V1V2/monthly-contribution/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("SupporterPlus2023V1V2/monthly-contribution/invoice-preview.json")

    // The effective date must be a billing date
    // Here we get the next billing date according to the invoice preview.
    val effectiveDate = LocalDate.of(2023, 8, 3) // montly: 2023-08-03

    // ZuoraInvoiceItem.items finds the invoice items corresponding to that billing date
    val invoiceItems = ZuoraInvoiceItem.items(invoicePreview, subscription, effectiveDate)

    val invoiceItemsCheck =
      List(
        ZuoraInvoiceItem("SUBSCRIPTION-NUMBER", LocalDate.of(2023, 8, 3), "C-04419773", "Supporter Plus"),
      )
    assertEquals(invoiceItems, invoiceItemsCheck)

    // Now that we have an invoice Item, which carries a chargeNumber, in this case "C-04417974", we can use it to
    // extract rate plan charges to get a collection of ZuoraRatePlanCharges

    val ratePlanCharges = ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItems.head).toSeq
    val ratePlanChargesCheck = List(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "8a12865b8219d9b401822106194e64e3",
        name = "Supporter Plus Monthly Charge",
        number = "C-04419773",
        currency = "GBP",
        price = Some(25.0),
        billingPeriod = Some("Month"),
        chargedThroughDate = Some(LocalDate.of(2023, 8, 3)),
        processedThroughDate = Some(LocalDate.of(2023, 7, 3)),
        specificBillingPeriod = None,
        endDateCondition = Some("Subscription_End"),
        upToPeriodsType = None,
        upToPeriods = None,
        billingDay = Some("ChargeTriggerDay"),
        triggerEvent = Some("CustomerAcceptance"),
        triggerDate = None,
        discountPercentage = None,
        originalOrderDate = Some(LocalDate.of(2023, 7, 3))
      )
    )

    assertEquals(
      ratePlanCharges,
      ratePlanChargesCheck
    )

    // And now that we have a ZuoraRatePlanCharge, we can use it to find a matching rate plans.

    val ratePlans = ZuoraRatePlan.ratePlanChargeToMatchingRatePlan(subscription, ratePlanCharges.head).toSeq

    assertEquals(
      ratePlans,
      List(
        ZuoraRatePlan(
          id = "8a12921d89018aaa01891bef52021b65",
          productName = "Supporter Plus",
          productRatePlanId = "8a12865b8219d9b401822106192b64dc",
          ratePlanName = "Supporter Plus Monthly",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "8a12865b8219d9b401822106194e64e3",
              name = "Supporter Plus Monthly Charge",
              number = "C-04419773",
              currency = "GBP",
              price = Some(25.0),
              billingPeriod = Some("Month"),
              chargedThroughDate = Some(LocalDate.of(2023, 8, 3)),
              processedThroughDate = Some(LocalDate.of(2023, 7, 3)),
              specificBillingPeriod = None,
              endDateCondition = Some("Subscription_End"),
              upToPeriodsType = None,
              upToPeriods = None,
              billingDay = Some("ChargeTriggerDay"),
              triggerEvent = Some("CustomerAcceptance"),
              triggerDate = None,
              discountPercentage = None,
              originalOrderDate = Some(LocalDate.of(2023, 7, 3))
            )
          ),
          lastChangeType = None
        )
      )
    )

    val cohortSpec =
      CohortSpec("SupporterPlus2023V1V2", "Campaign1", LocalDate.of(2023, 7, 14), LocalDate.of(2023, 8, 21))

    // The details of the item must match that of the estimation results in the corresponding EstimationHandlerTest

    val item =
      CohortItem(
        subscriptionName = "SUBSCRIPTION-NUMBER",
        processingStage = NotificationSendDateWrittenToSalesforce,
        startDate = Some(LocalDate.of(2023, 9, 3)),
        currency = Some("GBP"),
        oldPrice = Some(BigDecimal(25)),
        estimatedNewPrice = Some(BigDecimal(10)),
        billingPeriod = Some("Month")
      )

    val update = SupporterPlus2023V1V2Migration.zuoraUpdate(
      item,
      subscription,
      invoicePreview,
      effectiveDate: LocalDate
    )

    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              "8a128ed885fc6ded018602296ace3eb8",
              LocalDate.of(2023, 8, 3),
              chargeOverrides = List(
                ChargeOverride(
                  productRatePlanChargeId = "8a128d7085fc6dec01860234cd075270",
                  billingPeriod = "Month",
                  price = 15
                )
              )
            )
          ),
          remove = List(RemoveZuoraRatePlan("8a12921d89018aaa01891bef52021b65", LocalDate.of(2023, 8, 3))),
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
        discountPercentage = None,
        originalOrderDate = Some(LocalDate.of(2023, 7, 2))
      )
    )

    assertEquals(
      ratePlanCharges,
      ratePlanChargesCheck
    )

    // And now that we have a ZuoraRatePlanCharge, we can use it to find a matching rate plans.

    val ratePlans = ZuoraRatePlan.ratePlanChargeToMatchingRatePlan(subscription, ratePlanCharges.head).toSeq

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
              discountPercentage = None,
              originalOrderDate = Some(LocalDate.of(2023, 7, 2))
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

    val update = SupporterPlus2023V1V2Migration.zuoraUpdate(
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

  test("SupporterPlus2023V1V2 Amendment (annual contribution)") {

    val account = Fixtures.accountFromJson("SupporterPlus2023V1V2/annual-contribution/account.json")
    val catalogue = Fixtures.productCatalogueFromJson("SupporterPlus2023V1V2/annual-contribution/catalogue.json")
    val subscription = Fixtures.subscriptionFromJson("SupporterPlus2023V1V2/annual-contribution/subscription.json")
    val invoicePreview = Fixtures.invoiceListFromJson("SupporterPlus2023V1V2/annual-contribution/invoice-preview.json")

    // The effective date must be a billing date
    // Here we get the next billing date according to the invoice preview.
    val effectiveDate = LocalDate.of(2024, 6, 28) // annual: 2024-07-02

    // ZuoraInvoiceItem.items finds the invoice items corresponding to that billing date
    val invoiceItems = ZuoraInvoiceItem.items(invoicePreview, subscription, effectiveDate)

    val invoiceItemsCheck =
      List(
        ZuoraInvoiceItem("SUBSCRIPTION-NUMBER", LocalDate.of(2024, 6, 28), "C-04411538", "Supporter Plus"),
      )
    assertEquals(invoiceItems, invoiceItemsCheck)

    val ratePlanCharges = ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItems.head).toSeq
    val ratePlanChargesCheck = List(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "8a12865b8219d9b40182210618c664c1",
        name = "Supporter Plus Annual Charge",
        number = "C-04411538",
        currency = "GBP",
        price = Some(120.0),
        billingPeriod = Some("Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 6, 28)),
        processedThroughDate = Some(LocalDate.of(2023, 6, 28)),
        specificBillingPeriod = None,
        endDateCondition = Some("Subscription_End"),
        upToPeriodsType = None,
        upToPeriods = None,
        billingDay = Some("ChargeTriggerDay"),
        triggerEvent = Some("CustomerAcceptance"),
        triggerDate = None,
        discountPercentage = None,
        originalOrderDate = Some(LocalDate.of(2023, 6, 28))
      )
    )

    assertEquals(
      ratePlanCharges,
      ratePlanChargesCheck
    )

    // And now that we have a ZuoraRatePlanCharge, we can use it to find a matching rate plans.

    val ratePlans = ZuoraRatePlan.ratePlanChargeToMatchingRatePlan(subscription, ratePlanCharges.head).toSeq

    assertEquals(
      ratePlans,
      List(
        ZuoraRatePlan(
          id = "8a12843288f6ded10188ff5fbef67bb3",
          productName = "Supporter Plus",
          productRatePlanId = "8a12865b8219d9b40182210618a464ba",
          ratePlanName = "Supporter Plus Annual",
          ratePlanCharges = List(
            ZuoraRatePlanCharge(
              productRatePlanChargeId = "8a12865b8219d9b40182210618c664c1",
              name = "Supporter Plus Annual Charge",
              number = "C-04411538",
              currency = "GBP",
              price = Some(120.0),
              billingPeriod = Some("Annual"),
              chargedThroughDate = Some(LocalDate.of(2024, 6, 28)),
              processedThroughDate = Some(LocalDate.of(2023, 6, 28)),
              specificBillingPeriod = None,
              endDateCondition = Some("Subscription_End"),
              upToPeriodsType = None,
              upToPeriods = None,
              billingDay = Some("ChargeTriggerDay"),
              triggerEvent = Some("CustomerAcceptance"),
              triggerDate = None,
              discountPercentage = None,
              originalOrderDate = Some(LocalDate.of(2023, 6, 28))
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
        startDate = Some(LocalDate.of(2024, 6, 28)),
        currency = Some("GBP"),
        oldPrice = Some(BigDecimal(120)), // Here we simulate the subscription having a £25 contribution
        estimatedNewPrice = Some(BigDecimal(95)), // correct rate plan price for annual GBP
        billingPeriod = Some("Annual")
      )

    val update = SupporterPlus2023V1V2Migration.zuoraUpdate(
      item,
      subscription,
      invoicePreview,
      effectiveDate: LocalDate
    )

    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              "8a128ed885fc6ded01860228f77e3d5a",
              LocalDate.of(2024, 6, 28),
              chargeOverrides = List(
                ChargeOverride(
                  productRatePlanChargeId = "8a12892d85fc6df4018602451322287f",
                  billingPeriod = "Annual",
                  price = 25
                )
              )
            )
          ),
          remove = List(RemoveZuoraRatePlan("8a12843288f6ded10188ff5fbef67bb3", LocalDate.of(2024, 6, 28))),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }
}
