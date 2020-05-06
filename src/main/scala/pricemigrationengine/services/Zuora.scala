package pricemigrationengine.services

import pricemigrationengine.model.{ZuoraFetchFailure, ZuoraInvoiceList, ZuoraSubscription}
import zio.ZIO

object Zuora {

  trait Service {

    def fetchSubscription(subscriptionNumber: String): ZIO[Any, ZuoraFetchFailure, ZuoraSubscription]

    def fetchInvoicePreview(accountId: String): ZIO[Any, ZuoraFetchFailure, ZuoraInvoiceList]
  }

  def fetchSubscription(subscriptionNumber: String): ZIO[Zuora, ZuoraFetchFailure, ZuoraSubscription] =
    ZIO.accessM(_.get.fetchSubscription(subscriptionNumber))

  def fetchInvoicePreview(accountId: String): ZIO[Zuora, ZuoraFetchFailure, ZuoraInvoiceList] =
    ZIO.accessM(_.get.fetchInvoicePreview(accountId))
}
