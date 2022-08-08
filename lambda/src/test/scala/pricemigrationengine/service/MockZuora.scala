package pricemigrationengine.service

import pricemigrationengine.model._
import pricemigrationengine.services.Zuora
import zio._
import zio.mock._

import java.time.LocalDate

object MockZuora extends Mock[Zuora] {

  object FetchSubscription extends Effect[String, ZuoraFetchFailure, ZuoraSubscription]
  object FetchAccount extends Effect[(String, String), ZuoraFetchFailure, ZuoraAccount]
  object FetchInvoicePreview extends Effect[(String, LocalDate), ZuoraFetchFailure, ZuoraInvoiceList]
  object FetchProductCatalogue extends Effect[Unit, ZuoraFetchFailure, ZuoraProductCatalogue]
  object UpdateSubscription
      extends Effect[(ZuoraSubscription, ZuoraSubscriptionUpdate), ZuoraUpdateFailure, ZuoraSubscriptionId]

  val compose: URLayer[Proxy, Zuora] = ZLayer.fromZIO(ZIO.service[Proxy].map { proxy =>
    new Zuora {

      override def fetchSubscription(subscriptionNumber: String): ZIO[Any, ZuoraFetchFailure, ZuoraSubscription] =
        proxy(FetchSubscription, subscriptionNumber)

      override def fetchAccount(accountNumber: String, subscriptionNumber: String)
          : ZIO[Any, ZuoraFetchFailure, ZuoraAccount] =
        proxy(FetchAccount, accountNumber, subscriptionNumber)

      override def fetchInvoicePreview(accountId: String, targetDate: LocalDate)
          : ZIO[Any, ZuoraFetchFailure, ZuoraInvoiceList] = proxy(FetchInvoicePreview, accountId, targetDate)

      override val fetchProductCatalogue: ZIO[Any, ZuoraFetchFailure, ZuoraProductCatalogue] =
        proxy(FetchProductCatalogue)

      override def updateSubscription(subscription: ZuoraSubscription, update: ZuoraSubscriptionUpdate)
          : ZIO[Any, ZuoraUpdateFailure, ZuoraSubscriptionId] =
        proxy(UpdateSubscription, subscription, update)
    }
  })
}
