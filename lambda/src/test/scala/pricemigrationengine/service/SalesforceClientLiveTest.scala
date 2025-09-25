package pricemigrationengine.service

import java.time.LocalDate
import pricemigrationengine.model.{SalesforcePriceRise, ZuoraSubscriptionId}
import pricemigrationengine.services.{SalesforceClientLive, SalesforcePriceRiseCreationResponse}
import upickle.default._

class SalesforceClientLiveTest extends munit.FunSuite {
  test("SalesforceClientLive should serialise SalesforcePriceRise with all fields") {
    assertEquals(
      SalesforceClientLive.serialisePriceRise(
        SalesforcePriceRise(
          Name = Some("name"),
          Buyer__c = Some("buyer"),
          Current_Price_Today__c = Some(BigDecimal(1.23)),
          Guardian_Weekly_New_Price__c = Some(BigDecimal(1.99)),
          Price_Rise_Date__c = Some(LocalDate.of(2020, 1, 1)),
          SF_Subscription__c = Some("subscriptionId"),
          Date_Letter_Sent__c = Some(LocalDate.of(2020, 1, 2)),
          Migration_Name__c = Some("cohortName"),
          Migration_Status__c = Some("EstimationComplete"),
          Cancellation_Reason__c = None
        )
      ),
      """{
        |  "Name": "name",
        |  "Buyer__c": "buyer",
        |  "Current_Price_Today__c": 1.23,
        |  "Guardian_Weekly_New_Price__c": 1.99,
        |  "Price_Rise_Date__c": "2020-01-01",
        |  "SF_Subscription__c": "subscriptionId",
        |  "Date_Letter_Sent__c": "2020-01-02",
        |  "Migration_Name__c": "cohortName",
        |  "Migration_Status__c": "EstimationComplete",
        |  "Cancellation_Reason__c": null
        |}""".stripMargin
    )
  }
  test("SalesforceClientLive should serialise SalesforcePriceRise with fields missing") {
    assertEquals(
      SalesforceClientLive.serialisePriceRise(
        SalesforcePriceRise(
          Date_Letter_Sent__c = Some(LocalDate.of(2020, 1, 2)),
          Migration_Name__c = Some("cohortName"),
          Migration_Status__c = Some("EstimationComplete"),
          Cancellation_Reason__c = Some("Error 1")
        )
      ),
      """{
        |  "Date_Letter_Sent__c": "2020-01-02",
        |  "Migration_Name__c": "cohortName",
        |  "Migration_Status__c": "EstimationComplete",
        |  "Cancellation_Reason__c": "Error 1"
        |}""".stripMargin
    )
  }
  test("SalesforceClientLive should deserialise correctly the JSON object from Salesforce") {
    implicit val bigDecimalRW: ReadWriter[BigDecimal] =
      readwriter[ujson.Value].bimap[BigDecimal](
        bd => ujson.Num(bd.toDouble), // write
        js => js.num // read as number
      )
    implicit val localDateRW: ReadWriter[LocalDate] =
      readwriter[String].bimap[LocalDate](_.toString, LocalDate.parse)
    implicit val salesforcePriceRiseRW: ReadWriter[SalesforcePriceRise] = macroRW

    val rawJSON = """{
    |  "Migration_Name__c": "cohortName",
    |  "Migration_Status__c": "EstimationComplete",
    |  "Cancellation_Reason__c": "Error 1",
    |  "Current_Price_Today__c":311.88,
    |  "Guardian_Weekly_New_Price__c": 335.88
    |}""".stripMargin

    val priceRiseObject = read[SalesforcePriceRise](rawJSON)
    assertEquals(
      priceRiseObject.Guardian_Weekly_New_Price__c,
      Some(BigDecimal(335.88))
    )
  }
}
