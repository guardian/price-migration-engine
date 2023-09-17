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

  def renewSubscription(subscriptionNumber: String): ZIO[Any, ZuoraRenewalFailure, Unit]

  def fetchLastSubscriptionAmendment(
      subscriptionId: ZuoraSubscriptionId
  ): ZIO[Any, ZuoraFetchFailure, ZuoraSubscriptionAmendment]
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

  def renewSubscription(subscriptionNumber: String): ZIO[Zuora, ZuoraRenewalFailure, Unit] =
    ZIO.environmentWithZIO(_.get.renewSubscription(subscriptionNumber))

  // Note: the Zuora documentation
  // https://www.zuora.com/developer/api-references/older-api/operation/GET_AmendmentsBySubscriptionID/
  // specifies that a subscriptionId is to be provided, but it also works with a subscription number
  // (aka subscription name for a cohort item).
  def fetchLastSubscriptionAmendment(
      subscriptionId: ZuoraSubscriptionId
  ): ZIO[Zuora, Failure, ZuoraSubscriptionAmendment] =
    ZIO.environmentWithZIO(_.get.fetchLastSubscriptionAmendment(subscriptionId))
}
