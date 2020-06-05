package pricemigrationengine.model

case class SalesforceContact(
  Id: String,
  IdentityID__c: Option[String],
  Email: Option[String],
  FirstName: Option[String],
  LastName: Option[String],
  MailingAddress: SalesforceAddress
)