package pricemigrationengine.model

import pricemigrationengine.model.OptionReader // not sure why this import is needed as should be visible implicitly
import upickle.default._

case class SoldToContact(
    country: String
)

object SoldToContact {
  implicit val rwSubscription: ReadWriter[SoldToContact] = macroRW
}
