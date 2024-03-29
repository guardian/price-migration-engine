package pricemigrationengine.service

import java.time.LocalDate

import pricemigrationengine.model.SalesforcePriceRise
import pricemigrationengine.services.SalesforceClientLive

class SalesforceClientLiveTest extends munit.FunSuite {
  test("SalesforceClientLive should serialise SalesforcePriceRise with all fields") {
    assertEquals(
      SalesforceClientLive.serialisePriceRise(
        SalesforcePriceRise(
          Name = Some("name"),
          Buyer__c = Some("buyer"),
          Current_Price_Today__c = Some(1.23),
          Guardian_Weekly_New_Price__c = Some(1.99),
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
        |  "Current_Price_Today__c": "1.23",
        |  "Guardian_Weekly_New_Price__c": "1.99",
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
}
