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
          Date_Letter_Sent__c = Some(LocalDate.of(2020, 1, 2))
        )
      ),
      """{
        |  "Name": "name",
        |  "Buyer__c": "buyer",
        |  "Current_Price_Today__c": "1.23",
        |  "Guardian_Weekly_New_Price__c": "1.99",
        |  "Price_Rise_Date__c": "2020-01-01",
        |  "SF_Subscription__c": "subscriptionId",
        |  "Date_Letter_Sent__c": "2020-01-02"
        |}""".stripMargin
    )
  }
  test("SalesforceClientLive should serialise SalesforcePriceRise with fields missing") {
    assertEquals(
      SalesforceClientLive.serialisePriceRise(
        SalesforcePriceRise(
          Date_Letter_Sent__c = Some(LocalDate.of(2020, 1, 2))
        )
      ),
      """{
        |  "Date_Letter_Sent__c": "2020-01-02"
        |}""".stripMargin
    )
  }
}
