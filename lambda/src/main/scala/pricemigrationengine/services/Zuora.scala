package pricemigrationengine.services

import java.time.LocalDate

import pricemigrationengine.model._
import zio.ZIO

trait Zuora {

  def fetchSubscription(subscriptionNumber: String): ZIO[Any, ZuoraFetchFailure, ZuoraSubscription]

  def fetchAccount(accountNumber: String, subscriptionNumber: String): ZIO[Any, ZuoraFetchFailure, ZuoraAccount]

  def fetchInvoicePreview(accountId: String, targetDate: LocalDate): ZIO[Any, ZuoraFetchFailure, ZuoraInvoiceList]

  val fetchProductCatalogue: ZIO[Any, ZuoraFetchFailure, ZuoraProductCatalogue]

  def updateSubscription(
      subscription: ZuoraSubscription,
      update: ZuoraSubscriptionUpdate
  ): ZIO[Any, ZuoraUpdateFailure, ZuoraSubscriptionId]

  def applyAmendmentOrder(
      subscription: ZuoraSubscription,
      payload: ZuoraAmendmentOrderPayload
  ): ZIO[Any, ZuoraOrderFailure, Unit]

  def renewSubscription(
      subscriptionNumber: String,
      payload: ZuoraRenewOrderPayload
  ): ZIO[Any, ZuoraRenewalFailure, Unit]
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

  def updateSubscription(
      subscription: ZuoraSubscription,
      update: ZuoraSubscriptionUpdate
  ): ZIO[Zuora, ZuoraUpdateFailure, ZuoraSubscriptionId] =
    ZIO.environmentWithZIO(_.get.updateSubscription(subscription, update))

  def applyAmendmentOrder(
      subscription: ZuoraSubscription,
      payload: ZuoraAmendmentOrderPayload
  ): ZIO[Zuora, ZuoraOrderFailure, Unit] =
    ZIO.environmentWithZIO(_.get.applyAmendmentOrder(subscription, payload))

  def renewSubscription(
      subscriptionNumber: String,
      payload: ZuoraRenewOrderPayload
  ): ZIO[Zuora, ZuoraRenewalFailure, Unit] =
    ZIO.environmentWithZIO(_.get.renewSubscription(subscriptionNumber, payload))
}
