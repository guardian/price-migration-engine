package pricemigrationengine.model

import java.time.LocalDate

/*
  Example of a SalesforcePriceRise [22nd Sept 2025] (sanitized)

  {
    "attributes": {
        "type": "Price_Rise__c",
        "url": "/services/data/v64.0/sobjects/Price_Rise__c/260c802f9bdf"
    },
    "Id": "260c802f9bdf",
    "OwnerId": "6c88ca6ae12d",
    "IsDeleted": false,
    "Name": "00784b8c",
    "CreatedDate": "2019-02-27T14:51:50.000+0000",
    "CreatedById": "6c88ca6ae12d",
    "LastModifiedDate": "2025-08-04T11:09:17.000+0000",
    "LastModifiedById": "03d339ceb533",
    "SystemModstamp": "2025-08-04T11:09:17.000+0000",
    "LastViewedDate": null,
    "LastReferencedDate": null,
    "Buyer__c": "c13e8789316b",
    "Current_Price_Today__c": 30,
    "Date_Letter_Sent__c": "2019-02-04",
    "File_Name__c": "ad76eb4ea4a3",
    "Guardian_Weekly_New_Price__c": 37.5,
    "Price_Rise_Date__c": "2019-04-07",
    "SF_Subscription__c": "715e519d26a4",
    "Increase__c": 25,
    "Amended_Zuora_Subscription_Id__c": null,
    "Cancellation_Reason__c": null,
    "Migration_Name__c": null,
    "Migration_Status__c": null,
    "Status__c": null,
    "Customer_Opt_Out__c": true
}
 */

case class SalesforcePriceRise(
    Name: Option[String] = None,
    Buyer__c: Option[String] = None,
    Current_Price_Today__c: Option[BigDecimal] = None,
    Guardian_Weekly_New_Price__c: Option[BigDecimal] = None,
    Price_Rise_Date__c: Option[LocalDate] = None,
    SF_Subscription__c: Option[String] = None,
    Date_Letter_Sent__c: Option[LocalDate] = None,
    Amended_Zuora_Subscription_Id__c: Option[ZuoraSubscriptionId] = None,
    Migration_Name__c: Option[String],
    Migration_Status__c: Option[String], // [1]
    Cancellation_Reason__c: Option[String],
    Customer_Opt_Out__c: Option[Boolean] = None // [2]
)

// [1] The processing state of the cohort item at time of salesforce notification
//     and "Cancellation", if the item is about to or has been cancelled.

// Note that Cancellation_Reason__c should remain withing 255 chars. This is a limitation
// imposed by Salesforce which came during the initial
// integration: https://github.com/guardian/salesforce/pull/976

// [2] The Customer_Opt_Out__c attribute was added in September 2025 as part of the
// product migration: ProductMigration2025N4. Users who have read the communication
// and want to opt out will interact with a formstack page and trigger Salesforce
// to set that attribute to `true`.
