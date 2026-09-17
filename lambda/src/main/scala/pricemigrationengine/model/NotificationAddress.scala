package pricemigrationengine.model

case class NotificationAddress(
    streetInformation: Option[String],
    city: Option[String],
    state: Option[String],
    postalCode: Option[String],
    country: Option[String]
)
