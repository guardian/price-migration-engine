package pricemigrationengine.model.membershipworkflow

import pricemigrationengine.model.OptionWriter //required
import pricemigrationengine.model.OptionReader //required
import upickle.default.{ReadWriter, macroRW}

case class EmailPayloadSubscriberAttributes(
  title: Option[String],
  first_name: String,
  last_name: String,
  billing_address_1: String,
  billing_address_2: Option[String],
  billing_city: Option[String],
  billing_postal_code: String,
  billing_state: Option[String],
  billing_country: String,
  payment_amount: String,
  next_payment_date: String,
  payment_frequency: String,
  subscription_id: String
)

object EmailPayloadSubscriberAttributes {
  implicit val rw: ReadWriter[EmailPayloadSubscriberAttributes] = macroRW
}

case class EmailPayloadContactAttributes(SubscriberAttributes: EmailPayloadSubscriberAttributes)

object EmailPayloadContactAttributes {
  implicit val rw: ReadWriter[EmailPayloadContactAttributes] = macroRW
}

case class EmailPayload(Address: Option[String], ContactAttributes: EmailPayloadContactAttributes)

object EmailPayload {
  implicit val rw: ReadWriter[EmailPayload] = macroRW
}

case class EmailMessage(To: EmailPayload, DataExtensionName: String, SfContactId: String, IdentityUserId: Option[String])

object EmailMessage {
  implicit val rw: ReadWriter[EmailMessage] = macroRW
}