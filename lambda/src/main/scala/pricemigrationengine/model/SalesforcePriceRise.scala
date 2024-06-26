package pricemigrationengine.model

import java.time.LocalDate

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
    Cancellation_Reason__c: Option[String]
)

// [1] The processing state of the cohort item at time of salesforce notification
//     and "Cancellation", if the item is about to or has been cancelled.

// Note that Cancellation_Reason__c should remain withing 255 chars. This is a limitation
// imposed by Salesforce which came during the initial
// integration: https://github.com/guardian/salesforce/pull/976
