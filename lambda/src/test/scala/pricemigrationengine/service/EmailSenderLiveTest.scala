package pricemigrationengine.service

import pricemigrationengine.model.membershipworkflow.{EmailMessage, EmailPayload, EmailPayloadContactAttributes, EmailPayloadSubscriberAttributes}
import pricemigrationengine.services.EmailSenderLive

class EmailSenderLiveTest extends munit.FunSuite {
  test("EmailSenderLive should serialise message correctly") {
    assertEquals(
      EmailSenderLive.serialiseMessage(
        EmailMessage(
          EmailPayload(
            "test@test.com",
            EmailPayloadContactAttributes(
              EmailPayloadSubscriberAttributes(
                "firstName",
                "lastName",
                "address line 1",
                Some("town"),
                "postcode",
                Some("county"),
                "country",
                "1.23",
                "2020-01-01",
                "Monthly"
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
        |        "first_name": "firstName",
        |        "last_name": "lastName",
        |        "billing_address_1": "address line 1",
        |        "billing_city": "town",
        |        "billing_postal_code": "postcode",
        |        "billing_state": "county",
        |        "billing_country": "country",
        |        "payment_amount": "1.23",
        |        "next_payment_date": "2020-01-01",
        |        "payment_frequency": "Monthly"
        |      }
        |    }
        |  },
        |  "DataExtensionName": "data-extension",
        |  "SfContactId": "contactId",
        |  "IdentityUserId": "identity-user-id"
        |}""".stripMargin
    )
  }

  test("EmailSenderLive should serialise message with missing optional valuescorrectly") {
    assertEquals(
      EmailSenderLive.serialiseMessage(
        EmailMessage(
          EmailPayload(
            "test@test.com",
            EmailPayloadContactAttributes(
              EmailPayloadSubscriberAttributes(
                "firstName",
                "lastName",
                "address line 1",
                None,
                "postcode",
                None,
                "country",
                "1.23",
                "2020-01-01",
                "Monthly"
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
        |        "first_name": "firstName",
        |        "last_name": "lastName",
        |        "billing_address_1": "address line 1",
        |        "billing_city": null,
        |        "billing_postal_code": "postcode",
        |        "billing_state": null,
        |        "billing_country": "country",
        |        "payment_amount": "1.23",
        |        "next_payment_date": "2020-01-01",
        |        "payment_frequency": "Monthly"
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
