package pricemigrationengine.handlers

import pricemigrationengine.migrations.{
  Newspaper2025P1NotificationData,
  Newspaper2025P3NotificationData,
  ProductMigration2025N4NotificationData,
  SP2026EmailExtraAttributes
}
import pricemigrationengine.model.CohortTableFilter.SalesforcePriceRiseCreationComplete
import pricemigrationengine.model._
import pricemigrationengine.model.membershipworkflow.{
  BrazeMessage,
  BrazePayload,
  BrazePayloadContactAttributes,
  BrazePayloadSubscriberAttributes
}
import java.time.{Instant, LocalDate}

class NotificationHandlerHelperTest extends munit.FunSuite {
  test("basic scala used in assertNonTrivialValue") {
    assertEquals("".isEmpty, true)
    assertEquals("thing".isEmpty, false)
  }
  test("basic scala used in messageIsWellFormed") {
    assertEquals(List(true, false).forall(identity), false)
    assertEquals(List(true, true).forall(identity), true)
  }
  test("assertNonTrivialValue") {
    assertEquals(NotificationHandlerHelper.isNonTrivialValue(None), false)
    assertEquals(NotificationHandlerHelper.isNonTrivialValue(Some("")), false)
    assertEquals(NotificationHandlerHelper.isNonTrivialValue(Some("thing")), true)
  }
  test("SalesforceAddress") {
    val address1 = SalesforceAddress(
      street = Some("street"),
      city = Some("city"),
      state = Some("state"),
      postalCode = Some("postalCode"),
      country = Some("country")
    )
    val contact = SalesforceContact(
      Id = "Id",
      IdentityID__c = None,
      Email = None,
      Salutation = None,
      FirstName = None,
      LastName = None,
      OtherAddress = Some(address1),
      MailingAddress = None
    )
    // In this case OtherAddress if fine, going to be selected
    assertEquals(NotificationHandlerHelper.decideSalesforceAddress(contact), address1)
  }
  test("SalesforceAddress") {
    val address1 = SalesforceAddress(
      street = Some("street"),
      city = None,
      state = Some("state"),
      postalCode = Some("postalCode"),
      country = Some("country")
    )
    val address2 = SalesforceAddress(
      street = Some("street2"),
      city = None,
      state = Some("state2"),
      postalCode = Some("postalCode2"),
      country = Some("country2")
    )
    val contact = SalesforceContact(
      Id = "Id",
      IdentityID__c = None,
      Email = None,
      Salutation = None,
      FirstName = None,
      LastName = None,
      OtherAddress = Some(address1),
      MailingAddress = Some(address2)
    )
    // In this case OtherAddress is missing city, so MailingAddress, which is defined,
    // is going to be selected, even if itself is also missing city
    assertEquals(NotificationHandlerHelper.decideSalesforceAddress(contact), address2)
  }
  test("SalesforceAddress") {
    val address1 = SalesforceAddress(
      street = Some("street"),
      city = None,
      state = Some("state"),
      postalCode = Some("postalCode"),
      country = Some("country")
    )
    val contact = SalesforceContact(
      Id = "Id",
      IdentityID__c = None,
      Email = None,
      Salutation = None,
      FirstName = None,
      LastName = None,
      OtherAddress = Some(address1),
      MailingAddress = None
    )
    // Here we default to SalesforceAddress.addressWithEmptyStrings
    // because OtherAddress is not selectable and MailingAddress is missing
    assertEquals(NotificationHandlerHelper.decideSalesforceAddress(contact), SalesforceAddress.addressWithEmptyStrings)
  }
  test("SalesforceAddress") {
    val contact = SalesforceContact(
      Id = "Id",
      IdentityID__c = None,
      Email = None,
      Salutation = None,
      FirstName = None,
      LastName = None,
      OtherAddress = None,
      MailingAddress = None
    )
    // Here we default to SalesforceAddress.addressWithEmptyStrings
    assertEquals(NotificationHandlerHelper.decideSalesforceAddress(contact), SalesforceAddress.addressWithEmptyStrings)
  }
  test("buildBrazeMessage") {
    val address1 = SalesforceAddress(
      street = Some("Kings Place, 90 York Way"),
      city = Some("London"),
      state = None,
      postalCode = Some("N1 9GU"),
      country = Some("United Kingdom")
    )
    val contact = SalesforceContact(
      Id = "SfContactId",
      IdentityID__c = Some("IdentityID__c"),
      Email = Some("luke@resistance.org"),
      Salutation = Some("Ms"),
      FirstName = Some("Luke"),
      LastName = Some("Skywalker"),
      OtherAddress = Some(address1),
      MailingAddress = None
    )
    val cohortItem = CohortItem(
      subscriptionName = "subscriptionName",
      processingStage = SalesforcePriceRiseCreationComplete,
      currency = Some("GBP"),
      oldPrice = None,
      estimatedNewPrice = None,
      commsPrice = None,
      newPrice = None,
      billingPeriod = None,
      amendmentEffectiveDate = None,
      salesforcePriceRiseId = None,
      newSubscriptionId = None,

      migrationExtraAttributes = None,

      //
      cancellationReason = None,

      // timestamps
      whenEstimationDone = None,
      whenAmendmentDone = None,
      whenNotificationSent = None,
      whenNotificationSentWrittenToSalesforce = None,
      whenAmendmentWrittenToSalesforce = None,
      whenSfShowEstimate = None,

      // ProductMigration2025N4
      ex_2025N4_label = None,
      ex_2025N4_group = None,
      ex_2025N4_canvas = None,
      ex_2025N4_rateplan_current = None,
      ex_2025N4_rateplan_target = None,
      delayN4AmendmentUntil = None,

      // Membership2025
      ex_membership2025_country = None,
    )
    val sfSubscription = SalesforceSubscription(
      Id = "sfSubscriptionId",
      Name = "Name",
      Buyer__c = "Buyer__c",
      Status__c = "Status__c",
      Product_Type__c = None
    )
    // Here we default to SalesforceAddress.addressWithEmptyStrings
    assertEquals(
      NotificationHandlerHelper.buildBrazeMessage(
        contact,
        "Luke",
        "Skywalker",
        "Kings Place, 90 York Way",
        address1,
        "N1 9GU",
        "United Kingdom",
        "£12.50",
        "2026-10-12",
        "month",
        cohortItem,
        sfSubscription,
        Newspaper2025P1NotificationData(""),
        Newspaper2025P3NotificationData(""),
        ProductMigration2025N4NotificationData("", ""),
        "£",
        SP2026EmailExtraAttributes("", "", ""),
        "the Guardian and the Observer",
        "brazeName"
      ),
      BrazeMessage(
        To = BrazePayload(
          Address = Some("luke@resistance.org"),
          ContactAttributes = BrazePayloadContactAttributes(
            SubscriberAttributes = BrazePayloadSubscriberAttributes(
              title = Some("Ms"),
              first_name = "Luke",
              last_name = "Skywalker",
              billing_address_1 = "Kings Place, 90 York Way",
              billing_address_2 = None,
              billing_city = Some("London"),
              billing_postal_code = "N1 9GU",
              billing_state = None,
              billing_country = "United Kingdom",
              payment_amount = "£12.50",
              next_payment_date = "12 October 2026",
              payment_frequency = "month",
              subscription_id = "subscriptionName",
              product_type = "",

              // -----------------------------------------------
              // Newspaper2025P1 (extension)
              // (Comment Group: 571dac68)
              newspaper2025_brand_title = Some(""),
              // -----------------------------------------------

              // -----------------------------------------------
              // Newspaper2025P3 (extension)
              newspaper2025_phase3_brand_title = Some(""),
              // -----------------------------------------------

              // -----------------------------------------------
              // ProductMigration2025N4 (extension)
              newspaper2025_phase4_brand_title = Some(""),
              newspaper2025_phase4_formstack_url = Some(""),
              // -----------------------------------------------

              // -----------------------------------------------
              // SupporterPlus2026 (extension)
              sp2026_contribution_amount = Some("£"),
              sp2026_current_combined_amount = Some("£"),
              sp2026_new_combined_amount = Some("£"),
              // -----------------------------------------------

              // -----------------------------------------------
              // Newspaper2026 (extension)
              newspaper2026_brand_title = Some("the Guardian and the Observer"),
            )
          )
        ),
        DataExtensionName = "brazeName",
        SfContactId = "SfContactId",
        IdentityUserId = Some("IdentityID__c")
      )
    )
  }
}
