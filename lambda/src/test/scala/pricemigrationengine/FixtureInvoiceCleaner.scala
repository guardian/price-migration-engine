package pricemigrationengine

import scala.io.Source

object FixtureInvoiceCleaner extends App {

  def cleaned(s: String): String =
    Map(
      "id" -> "id",
      ".*[aA]ccountId" -> "accId",
      ".*[aA]ccountNumber" -> "accNum",
      ".*[aA]ccountName" -> "accName",
      "subscriptionName" -> "subName",
      "subscriptionId" -> "subId",
      "subscriptionNumber" -> "subNum",
      "chargeId" -> "chargeId"
    ).foldLeft(s) {
      case (str, (fieldName, replacementVal)) =>
        str.replaceAll(s""""($fieldName)": ".+"""", s""""$$1": "$replacementVal"""")
    }

  println(cleaned(Source.fromResource(args(0)).mkString))
}
