package pricemigrationengine.handlers

import pricemigrationengine.model.{NotificationHandlerFailure, SalesforceAddress, SalesforceContact}
import zio.test._

object NotificationHandlerSpec extends ZIOSpecDefault {

  private val billingAddress = SalesforceAddress(
    street = Some("21 High Street"),
    city = Some("London"),
    state = None,
    postalCode = Some("N2 9GZ"),
    country = Some("United Kingdom")
  )

  private val mailingAddress = SalesforceAddress(
    street = Some("1 High Street"),
    city = Some("London"),
    state = None,
    postalCode = Some("N1 9GU"),
    country = Some("United Kingdom")
  )

  override def spec: Spec[Any, TestFailure[NotificationHandlerFailure], TestSuccess] = suite("targetAddress")(
    test("should use mailing address if billing address has no street") {
      val contact = SalesforceContact(
        Id = "id",
        IdentityID__c = None,
        Email = None,
        Salutation = None,
        FirstName = None,
        LastName = None,
        OtherAddress = Some(billingAddress.copy(street = None)),
        MailingAddress = Some(mailingAddress)
      )
      NotificationHandler.targetAddress(contact) map { address =>
        assertTrue(address == mailingAddress)
      }
    },
    test("should use mailing address if billing address has no city") {
      val contact = SalesforceContact(
        Id = "id",
        IdentityID__c = None,
        Email = None,
        Salutation = None,
        FirstName = None,
        LastName = None,
        OtherAddress = Some(billingAddress.copy(city = None)),
        MailingAddress = Some(mailingAddress)
      )
      NotificationHandler.targetAddress(contact) map { address =>
        assertTrue(address == mailingAddress)
      }
    }
  )
}
