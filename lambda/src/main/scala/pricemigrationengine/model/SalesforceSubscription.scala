package pricemigrationengine.model

case class SalesforceSubscription(
    Id: String,
    Name: String,
    Buyer__c: String,
    Status__c: String,
    Product_Type__c: Option[String] // This ought to always have a value but there appear to be some nulls in the data.
)
