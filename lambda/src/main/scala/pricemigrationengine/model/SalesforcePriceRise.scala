package pricemigrationengine.model

import java.time.LocalDate

case class SalesforcePriceRise(
  Name: Option[String] = None,
  Buyer__c: Option[String] = None,
  Current_Price_Today__c: Option[BigDecimal] = None,
  Guardian_Weekly_New_Price__c: Option[BigDecimal] = None,
  Price_Rise_Date__c: Option[LocalDate] = None,
  SF_Subscription__c: Option[String] = None,
  Date_Letter_Sent__c: Option[LocalDate] = None
)