package pricemigrationengine.service

import pricemigrationengine.model.membershipworkflow.{EmailMessage, EmailPayload, EmailPayloadContactAttributes}
import pricemigrationengine.services.EmailSenderLive

class EmailSenderLiveTest extends munit.FunSuite {
  test("EmailSenderLive should serialise message correctly") {
    assertEquals(
      EmailSenderLive.serialiseMessage(
        EmailMessage(
          EmailPayload(
            "test@test.com",
            EmailPayloadContactAttributes(
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
        |      "FirstName": "firstName",
        |      "LastName": "lastName",
        |      "AddressLine1": "address line 1",
        |      "Town": "town",
        |      "Postcode": "postcode",
        |      "County": "county",
        |      "Country": "country",
        |      "NewPrice": "1.23",
        |      "StartDate": "2020-01-01",
        |      "BillingPeriod": "Monthly"
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
        |      "FirstName": "firstName",
        |      "LastName": "lastName",
        |      "AddressLine1": "address line 1",
        |      "Town": null,
        |      "Postcode": "postcode",
        |      "County": null,
        |      "Country": "country",
        |      "NewPrice": "1.23",
        |      "StartDate": "2020-01-01",
        |      "BillingPeriod": "Monthly"
        |    }
        |  },
        |  "DataExtensionName": "data-extension",
        |  "SfContactId": "contactId",
        |  "IdentityUserId": null
        |}""".stripMargin
    )
  }
}
