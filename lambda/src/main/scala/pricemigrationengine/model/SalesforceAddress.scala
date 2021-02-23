package pricemigrationengine.model

case class SalesforceAddress(
    street: Option[String],
    city: Option[String],
    state: Option[String],
    postalCode: Option[String],
    country: Option[String]
)
