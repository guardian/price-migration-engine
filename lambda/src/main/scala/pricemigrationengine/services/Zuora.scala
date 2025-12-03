package pricemigrationengine.services

import java.time.LocalDate

import pricemigrationengine.model._
import zio.ZIO
import ujson._

trait Zuora {

  def fetchSubscription(subscriptionNumber: String): ZIO[Any, ZuoraFetchFailure, ZuoraSubscription]

  def fetchAccount(accountNumber: String, subscriptionNumber: String): ZIO[Any, ZuoraFetchFailure, ZuoraAccount]

  def fetchInvoicePreview(accountId: String, targetDate: LocalDate): ZIO[Any, ZuoraFetchFailure, ZuoraInvoiceList]

  val fetchProductCatalogue: ZIO[Any, ZuoraFetchFailure, ZuoraProductCatalogue]

  /*
    This function takes a Zuora Orders API payload and submit it for asynchronous processing
    Note that the `subscriptionNumber` and `operationDescriptionForLogging` are both
    only used for logging. Notably `operationDescriptionForLogging` was introduced
    simply to specify a difference between renewals and product changes.
   */
  def applyOrderAsynchronously(
      subscriptionNumber: String,
      payload: Value,
      operationDescriptionForLogging: String
  ): ZIO[Any, ZuoraAsynchronousOrderRequestFailure, Unit]
}

object Zuora {

  def fetchSubscription(subscriptionNumber: String): ZIO[Zuora, Failure, ZuoraSubscription] =
    ZIO.environmentWithZIO(_.get.fetchSubscription(subscriptionNumber))

  def fetchAccount(accountNumber: String, subscriptionNumber: String): ZIO[Zuora, ZuoraFetchFailure, ZuoraAccount] =
    ZIO.environmentWithZIO(_.get.fetchAccount(accountNumber, subscriptionNumber))

  def fetchInvoicePreview(accountId: String, targetDate: LocalDate): ZIO[Zuora, ZuoraFetchFailure, ZuoraInvoiceList] =
    ZIO.environmentWithZIO(_.get.fetchInvoicePreview(accountId, targetDate))

  val fetchProductCatalogue: ZIO[Zuora, ZuoraFetchFailure, ZuoraProductCatalogue] =
    ZIO.environmentWithZIO(_.get.fetchProductCatalogue)

  def applyOrderAsynchronously(
      subscriptionNumber: String,
      payload: Value,
      operationDescriptionForLogging: String
  ): ZIO[Zuora, ZuoraAsynchronousOrderRequestFailure, Unit] =
    ZIO.environmentWithZIO(_.get.applyOrderAsynchronously(subscriptionNumber, payload, operationDescriptionForLogging))
}
