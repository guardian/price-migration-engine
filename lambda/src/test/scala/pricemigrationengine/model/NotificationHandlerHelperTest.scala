package pricemigrationengine.handlers

import pricemigrationengine.migrations.{Newspaper2025P3NotificationData, SP2026EmailExtraAttributes}
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
  test("buildBrazeMessage") {
    val notificationAddress = NotificationAddress(
      streetInformation = Some("Kings Place, 90 York Way"),
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
      OtherAddress = Some(
        SalesforceAddress(
          street = Some("Kings Place, 90 York Way"),
          city = Some("London"),
          state = None,
          postalCode = Some("N1 9GU"),
          country = Some("United Kingdom")
        )
      ),
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
        contact = contact,
        firstName = "Luke",
        lastName = "Skywalker",
        street = "Kings Place, 90 York Way",
        notificationAddress = notificationAddress,
        postalCode = "N1 9GU",
        country = "United Kingdom",
        commsPriceWithCurrencySymbol = "£12.50",
        amendmentEffectiveDate = "2026-10-12",
        paymentFrequency = "month",
        cohortItem = cohortItem,
        sfSubscription = sfSubscription,
        newspaper2025P3NotificationData = Newspaper2025P3NotificationData(""),
        currencySymbol = "£",
        supporterPlus2026ExtraData = SP2026EmailExtraAttributes("", "", ""),
        newspaper2026_brand_title = "the Guardian and the Observer",
        brazeName = "brazeName"
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
              // Newspaper2025P3 (extension)
              newspaper2025_phase3_brand_title = Some(""),
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
  test("zuoraAccountSoldToContactToStreetInformation (1)") {
    val zuoraAccountSoldToContact = ZuoraAccountSoldToContact(
      address1 = Some("address1"),
      address2 = Some("address2"),
      city = Some("city"),
      zipCode = Some("zipCode"),
      state = None,
      country = "United Kingdom"
    )
    assertEquals(
      NotificationHandlerHelper.zuoraAccountSoldToContactToStreetInformation(
        zuoraAccountSoldToContact
      ),
      Some("address1 / address2")
    )
  }
  test("zuoraAccountSoldToContactToStreetInformation (2)") {
    val zuoraAccountSoldToContact = ZuoraAccountSoldToContact(
      address1 = None,
      address2 = Some("address2"),
      city = Some("city"),
      zipCode = Some("zipCode"),
      state = None,
      country = "United Kingdom"
    )
    assertEquals(
      NotificationHandlerHelper.zuoraAccountSoldToContactToStreetInformation(
        zuoraAccountSoldToContact
      ),
      Some("address2")
    )
  }
  test("zuoraAccountSoldToContactToStreetInformation (3)") {
    val zuoraAccountSoldToContact = ZuoraAccountSoldToContact(
      address1 = Some("address1"),
      address2 = None,
      city = Some("city"),
      zipCode = Some("zipCode"),
      state = None,
      country = "United Kingdom"
    )
    assertEquals(
      NotificationHandlerHelper.zuoraAccountSoldToContactToStreetInformation(
        zuoraAccountSoldToContact
      ),
      Some("address1")
    )
  }
  test("firstDefined (1)") {
    val a: Option[String] = Some("a")
    val b: Option[String] = Some("b")
    val c: Option[String] = Some("c")
    assertEquals(
      NotificationHandlerHelper.firstDefined(a, b, c),
      Some("a")
    )
  }
  test("firstDefined (2)") {
    val a: Option[String] = None
    val b: Option[String] = None
    val c: Option[String] = Some("c")
    assertEquals(
      NotificationHandlerHelper.firstDefined(a, b, c),
      Some("c")
    )
  }
  test("buildNotificationAddress (1)") {

    // In this case zuoraAccountSoldToContact is sufficient, so it's going to be taken
    // And cherry on the cake, both address1 and address2 are defined

    val zuoraAccountSoldToContact = ZuoraAccountSoldToContact(
      address1 = Some("address1"),
      address2 = Some("address2"),
      city = Some("city"),
      zipCode = None,
      state = None,
      country = "United Kindgom"
    )

    val salesforceContact = SalesforceContact(
      Id = "SfContactId",
      IdentityID__c = Some("IdentityID__c"),
      Email = Some("luke@resistance.org"),
      Salutation = Some("Ms"),
      FirstName = Some("Luke"),
      LastName = Some("Skywalker"),
      OtherAddress = Some(
        SalesforceAddress(
          street = Some("1600 Pennsylvania Avenue NW"),
          city = Some("Washington"),
          state = Some("DC"),
          postalCode = Some("20500"),
          country = Some("United States")
        )
      ),
      MailingAddress = Some(
        SalesforceAddress(
          street = Some("Kings Place, 90 York Way"),
          city = None, // missing information
          state = None,
          postalCode = Some("N1 9GU"),
          country = Some("United Kingdom")
        )
      )
    )
    assertEquals(
      NotificationHandlerHelper.buildNotificationAddress(zuoraAccountSoldToContact, salesforceContact),
      NotificationAddress(
        streetInformation = Some("address1 / address2"),
        city = Some("city"),
        state = None,
        postalCode = None,
        country = Some("United Kindgom")
      )
    )
  }
  test("buildNotificationAddress (2)") {

    // In this case zuoraAccountSoldToContact is sufficient, so it's going to be taken
    // Same as before but only address2 is defined

    val zuoraAccountSoldToContact = ZuoraAccountSoldToContact(
      address1 = None,
      address2 = Some("address2"),
      city = Some("city"),
      zipCode = None,
      state = None,
      country = "United Kindgom"
    )

    val salesforceContact = SalesforceContact(
      Id = "SfContactId",
      IdentityID__c = Some("IdentityID__c"),
      Email = Some("luke@resistance.org"),
      Salutation = Some("Ms"),
      FirstName = Some("Luke"),
      LastName = Some("Skywalker"),
      OtherAddress = Some(
        SalesforceAddress(
          street = Some("1600 Pennsylvania Avenue NW"),
          city = Some("Washington"),
          state = Some("DC"),
          postalCode = Some("20500"),
          country = Some("United States")
        )
      ),
      MailingAddress = Some(
        SalesforceAddress(
          street = Some("Kings Place, 90 York Way"),
          city = None, // missing information
          state = None,
          postalCode = Some("N1 9GU"),
          country = Some("United Kingdom")
        )
      )
    )
    assertEquals(
      NotificationHandlerHelper.buildNotificationAddress(zuoraAccountSoldToContact, salesforceContact),
      NotificationAddress(
        streetInformation = Some("address2"),
        city = Some("city"),
        state = None,
        postalCode = None,
        country = Some("United Kindgom")
      )
    )
  }
  test("buildNotificationAddress (3)") {

    // Here zuoraAccountSoldToContact is going to break, so we fall
    // back to salesforceContact' MailingAddress

    val zuoraAccountSoldToContact = ZuoraAccountSoldToContact(
      address1 = None,
      address2 = None,
      city = Some("city"),
      zipCode = None,
      state = None,
      country = "United Kindgom"
    )

    val salesforceContact = SalesforceContact(
      Id = "SfContactId",
      IdentityID__c = Some("IdentityID__c"),
      Email = Some("luke@resistance.org"),
      Salutation = Some("Ms"),
      FirstName = Some("Luke"),
      LastName = Some("Skywalker"),
      OtherAddress = Some(
        SalesforceAddress(
          street = Some("1600 Pennsylvania Avenue NW"),
          city = Some("Washington"),
          state = Some("DC"),
          postalCode = Some("20500"),
          country = Some("United States")
        )
      ),
      MailingAddress = Some(
        SalesforceAddress(
          street = Some("Kings Place, 90 York Way"),
          city = Some("London"),
          state = None,
          postalCode = Some("N1 9GU"),
          country = Some("United Kingdom")
        )
      )
    )
    assertEquals(
      NotificationHandlerHelper.buildNotificationAddress(zuoraAccountSoldToContact, salesforceContact),
      NotificationAddress(
        streetInformation = Some("Kings Place, 90 York Way"),
        city = Some("London"),
        state = None,
        postalCode = Some("N1 9GU"),
        country = Some("United Kingdom")
      )
    )
  }
  test("buildNotificationAddress (4)") {

    // Here zuoraAccountSoldToContact is going to break, and
    // salesforceContact' MailingAddress is also going to break, so we fall back to
    // salesforceContact' OtherAddress

    val zuoraAccountSoldToContact = ZuoraAccountSoldToContact(
      address1 = None, // missing information
      address2 = None, // missing information
      city = Some("city"),
      zipCode = None,
      state = None,
      country = "United Kindgom"
    )

    val salesforceContact = SalesforceContact(
      Id = "SfContactId",
      IdentityID__c = Some("IdentityID__c"),
      Email = Some("luke@resistance.org"),
      Salutation = Some("Ms"),
      FirstName = Some("Luke"),
      LastName = Some("Skywalker"),
      OtherAddress = Some(
        SalesforceAddress(
          street = Some("1600 Pennsylvania Avenue NW"),
          city = Some("Washington"),
          state = Some("DC"),
          postalCode = Some("20500"),
          country = Some("United States")
        )
      ),
      MailingAddress = Some(
        SalesforceAddress(
          street = Some("Kings Place, 90 York Way"),
          city = None, // missing information
          state = None,
          postalCode = Some("N1 9GU"),
          country = Some("United Kingdom")
        )
      )
    )
    assertEquals(
      NotificationHandlerHelper.buildNotificationAddress(zuoraAccountSoldToContact, salesforceContact),
      NotificationAddress(
        streetInformation = Some("1600 Pennsylvania Avenue NW"),
        city = Some("Washington"),
        state = Some("DC"),
        postalCode = Some("20500"),
        country = Some("United States")
      )
    )
  }
  test("buildNotificationAddress (5)") {

    // Here everything breaks, so we return the empty NotificationAddress

    val zuoraAccountSoldToContact = ZuoraAccountSoldToContact(
      address1 = None, // missing information
      address2 = None, // missing information
      city = Some("city"),
      zipCode = None,
      state = None,
      country = "United Kindgom"
    )

    val salesforceContact = SalesforceContact(
      Id = "SfContactId",
      IdentityID__c = Some("IdentityID__c"),
      Email = Some("luke@resistance.org"),
      Salutation = Some("Ms"),
      FirstName = Some("Luke"),
      LastName = Some("Skywalker"),
      OtherAddress = Some(
        SalesforceAddress(
          street = None, // missing information,
          city = None, // missing information
          state = Some("DC"),
          postalCode = Some("20500"),
          country = Some("United States")
        )
      ),
      MailingAddress = Some(
        SalesforceAddress(
          street = None, // missing information,
          city = Some("Washington"),
          state = None,
          postalCode = Some("N1 9GU"),
          country = Some("United Kingdom")
        )
      )
    )
    assertEquals(
      NotificationHandlerHelper.buildNotificationAddress(zuoraAccountSoldToContact, salesforceContact),
      NotificationAddress(
        streetInformation = None,
        city = None,
        state = None,
        postalCode = None,
        country = None
      )
    )
  }
}
