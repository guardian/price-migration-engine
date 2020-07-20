package pricemigrationengine.service

import pricemigrationengine.model.membershipworkflow.{EmailMessage, EmailPayload, EmailPayloadContactAttributes, EmailPayloadSubscriberAttributes}
import pricemigrationengine.services.EmailSenderLive

class EmailSenderLiveTest extends munit.FunSuite {
  test("EmailSenderLive should serialise message correctly") {
    assertEquals(
      EmailSenderLive.serialiseMessage(
        EmailMessage(
          EmailPayload(
            Some("test@test.com"),
            EmailPayloadContactAttributes(
              EmailPayloadSubscriberAttributes(
                Some("title"),
                "firstName",
                "lastName",
                "address line 1",
                Some("address line 2"),
                Some("town"),
                "postcode",
                Some("county"),
                "country",
                "1.23",
                "2020-01-01",
                "Monthly",
                "Subscription-001"
              )
            )
          ),
          "data-extension",
          "contactId",
          Some("identity-user-id")
        )
      ),
      """{
        |  "To": {
        |    "Address": "test@test.com",
        |    "ContactAttributes": {
        |      "SubscriberAttributes": {
        |        "title": "title",
        |        "first_name": "firstName",
        |        "last_name": "lastName",
        |        "billing_address_1": "address line 1",
        |        "billing_address_2": "address line 2",
        |        "billing_city": "town",
        |        "billing_postal_code": "postcode",
        |        "billing_state": "county",
        |        "billing_country": "country",
        |        "payment_amount": "1.23",
        |        "next_payment_date": "2020-01-01",
        |        "payment_frequency": "Monthly",
        |        "subscription_id": "Subscription-001"
        |      }
        |    }
        |  },
        |  "DataExtensionName": "data-extension",
        |  "SfContactId": "contactId",
        |  "IdentityUserId": "identity-user-id"
        |}""".stripMargin
    )
  }

  test("EmailSenderLive should serialise message with missing optional values correctly") {
    assertEquals(
      EmailSenderLive.serialiseMessage(
        EmailMessage(
          EmailPayload(
            Some("test@test.com"),
            EmailPayloadContactAttributes(
              EmailPayloadSubscriberAttributes(
                None,
                "firstName",
                "lastName",
                "address line 1",
                None,
                None,
                "postcode",
                None,
                "country",
                "1.23",
                "2020-01-01",
                "Monthly",
                "Subscription-001"
              )
            )
          ),
          "data-extension",
          "contactId",
          None
        )
      ),
      """{
        |  "To": {
        |    "Address": "test@test.com",
        |    "ContactAttributes": {
        |      "SubscriberAttributes": {
        |        "title": null,
        |        "first_name": "firstName",
        |        "last_name": "lastName",
        |        "billing_address_1": "address line 1",
        |        "billing_address_2": null,
        |        "billing_city": null,
        |        "billing_postal_code": "postcode",
        |        "billing_state": null,
        |        "billing_country": "country",
        |        "payment_amount": "1.23",
        |        "next_payment_date": "2020-01-01",
        |        "payment_frequency": "Monthly",
        |        "subscription_id": "Subscription-001"
        |      }
        |    }
        |  },
        |  "DataExtensionName": "data-extension",
        |  "SfContactId": "contactId",
        |  "IdentityUserId": null
        |}""".stripMargin
    )
  }
}
