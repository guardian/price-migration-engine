package pricemigrationengine.model.membershipworkflow

case class EmailPayloadContactAttributes(
  FirstName: String,
  LastName: String,
  AddressLine1: String,
  Town: String,
  Postcode: String,
  County: String,
  Country: String,
  NewPrice: String,
  StartDate: String,
  BillingPeriod: String
)

case class EmailPayload(Address: String, ContactAttributes: EmailPayloadContactAttributes)

case class EmailMessage(To: EmailPayload, DataExtensionName: String, SfContactId: String, IdentityUserId: Option[String])