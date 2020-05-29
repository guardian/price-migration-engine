package pricemigrationengine.services

import pricemigrationengine.model._
import zio.ZIO

object Zuora {

  trait Service {

    def fetchSubscription(subscriptionNumber: String): ZIO[Any, ZuoraFetchFailure, ZuoraSubscription]

    def fetchInvoicePreview(accountId: String): ZIO[Any, ZuoraFetchFailure, ZuoraInvoiceList]

    val fetchProductCatalogue: ZIO[Any, ZuoraFetchFailure, ZuoraProductCatalogue]

    def updateSubscription(
        subscription: ZuoraSubscription,
        update: ZuoraSubscriptionUpdate
    ): ZIO[Any, ZuoraUpdateFailure, ZuoraSubscriptionId]
  }

  def fetchSubscription(subscriptionNumber: String): ZIO[Zuora, Failure, ZuoraSubscription] =
    ZIO.accessM(_.get.fetchSubscription(subscriptionNumber))

  def fetchInvoicePreview(accountId: String): ZIO[Zuora, ZuoraFetchFailure, ZuoraInvoiceList] =
    ZIO.accessM(_.get.fetchInvoicePreview(accountId))

  val fetchProductCatalogue: ZIO[Zuora, ZuoraFetchFailure, ZuoraProductCatalogue] =
    ZIO.accessM(_.get.fetchProductCatalogue)

  def updateSubscription(
      subscription: ZuoraSubscription,
      update: ZuoraSubscriptionUpdate
  ): ZIO[Zuora, ZuoraUpdateFailure, ZuoraSubscriptionId] =
    ZIO.accessM(_.get.updateSubscription(subscription, update))
}
