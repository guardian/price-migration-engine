package pricemigrationengine.model

import java.time.LocalDate

case class SalesforcePriceRise(
  Buyer__c: String,
  Current_Price_Today__c: BigDecimal,
  Guardian_Weekly_New_Price__c: BigDecimal,
  Price_Rise_Date__c: LocalDate,
  SF_Subscription__c: String
)