package pricemigrationengine.handlers

import pricemigrationengine.model._

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

}
