package pricemigrationengine.model

case class SalesforceAddress(
    street: Option[String],
    city: Option[String],
    state: Option[String],
    postalCode: Option[String],
    country: Option[String]
)

object SalesforceAddress {
  // This value exist as the default empty NotificationAddress
  // The reason for the Some(""), at opposition to None, is that the fields are used
  // in a for comprehension and None would short circuit the entire process, whereas Some("")
  // allows for empty values in cases the field wasn't required
  val addressWithEmptyStrings = SalesforceAddress(Some(""), Some(""), Some(""), Some(""), Some(""))
}
