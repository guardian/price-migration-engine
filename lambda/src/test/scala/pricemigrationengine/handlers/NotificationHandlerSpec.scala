package pricemigrationengine.handlers

import pricemigrationengine.model.{CohortSpec, NotificationHandlerFailure, SalesforceAddress, SalesforceContact}
import zio.test._

import java.time.LocalDate

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

  override def spec: Spec[Any, NotificationHandlerFailure] = suite("targetAddress")(
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
      val cohortSpec = CohortSpec("cohortName", "BrazeCampaignName", LocalDate.of(2000, 1, 1), LocalDate.of(2023, 1, 1))
      val salesForceAddress = NotificationHandler.targetAddress(cohortSpec, contact).toOption.get
      assertTrue(salesForceAddress == mailingAddress)
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
      val cohortSpec = CohortSpec("cohortName", "BrazeCampaignName", LocalDate.of(2000, 1, 1), LocalDate.of(2023, 1, 1))
      val salesForceAddress = NotificationHandler.targetAddress(cohortSpec, contact).toOption.get
      assertTrue(salesForceAddress == mailingAddress)
    }
  )
}
