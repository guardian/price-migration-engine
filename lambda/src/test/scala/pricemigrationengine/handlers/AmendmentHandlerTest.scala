package pricemigrationengine.handlers

import pricemigrationengine.model.{ZuoraRatePlanCharge, ZuoraSubscriptionUpdate, _}

import java.time.LocalDate
import pricemigrationengine.Fixtures

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
        "2c92a0f94c547592014c69f5b1204f80",
        "Supporter Membership - Monthly",
        "C-00400194",
        "GBP",
        Some(5.0),
        Some("Month"),
        Some(LocalDate.of(2023, 4, 13)),
        Some(LocalDate.of(2023, 3, 13)),
        None,
        Some("Subscription_End"),
        None,
        None,
        Some("ChargeTriggerDay"),
        Some("ContractEffective"),
        None,
        None
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
}
