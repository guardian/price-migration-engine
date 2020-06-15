package pricemigrationengine.model.membershipworkflow

import pricemigrationengine.model.OptionWriter //required
import pricemigrationengine.model.OptionReader //required
import upickle.default.{ReadWriter, macroRW}

case class EmailPayloadContactAttributes(
  FirstName: String,
  LastName: String,
  AddressLine1: String,
  Town: Option[String],
  Postcode: String,
  County: Option[String],
  Country: String,
  NewPrice: String,
  StartDate: String,
  BillingPeriod: String
)

object EmailPayloadContactAttributes {
  implicit val rw: ReadWriter[EmailPayloadContactAttributes] = macroRW
}

case class EmailPayload(Address: String, ContactAttributes: EmailPayloadContactAttributes)

object EmailPayload {
  implicit val rw: ReadWriter[EmailPayload] = macroRW
}

case class EmailMessage(To: EmailPayload, DataExtensionName: String, SfContactId: String, IdentityUserId: Option[String])

object EmailMessage {
  implicit val rw: ReadWriter[EmailMessage] = macroRW
}