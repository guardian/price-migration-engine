package pricemigrationengine.model

case class SalesforceContact(
    Id: String,
    IdentityID__c: Option[String],
    Email: Option[String],
    Salutation: Option[String],
    FirstName: Option[String],
    LastName: Option[String],
    OtherAddress: Option[SalesforceAddress], // Billing address
    MailingAddress: Option[SalesforceAddress] // Fallback address if no billing address available
)
