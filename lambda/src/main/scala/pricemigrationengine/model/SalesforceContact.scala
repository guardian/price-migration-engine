package pricemigrationengine.model

case class SalesforceContact(
  Id: String,
  IdentityID__c: Option[String],
  Email: Option[String],
  Salutation: Option[String],
  FirstName: Option[String],
  LastName: Option[String],
  OtherAddress: SalesforceAddress
)