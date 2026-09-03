package pricemigrationengine.migrations
import pricemigrationengine.model.{BillingPeriod, ZuoraRatePlan, _}

import java.time.LocalDate
import ujson._
import upickle.default._

import java.time.format.DateTimeFormatter

object Print2026C1NPAnnualsUKMigration {

  val earliestAmendmentEffectiveDate = ???
  val notificationLeadTime = 35

  // ------------------------------------------------
  // Primary Functions:
  //
  // The primary functions are the main functions that
  // are implemented by the *Migration module.
  //
  // - priceData is used in the Estimation handler
  // - amendmentOrderPayload is used in the Amendment handler
  // ------------------------------------------------

  def logValue[T](label: String)(value: T): T = {
    println(s"$label: $value")
    value
  }

  def priceData(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      account: ZuoraAccount
  ): Either[DataExtractionFailure, PriceData] = ???

  def amendmentOrderPayload(
      cohortItem: CohortItem,
      orderDate: LocalDate,
      accountNumber: String,
      subscriptionNumber: String,
      effectDate: LocalDate,
      zuora_subscription: ZuoraSubscription,
      commsPrice: BigDecimal,
      invoiceList: ZuoraInvoiceList,
  ): Either[Failure, Value] = ???
}
