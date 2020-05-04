package pricemigrationengine.services

import pricemigrationengine.model.{ZuoraAccount, ZuoraFetchFailure, ZuoraSubscription}
import zio.ZIO

object Zuora {

  trait Service {
    def fetchSubscription(subscriptionNumber: String): ZIO[Any, ZuoraFetchFailure, ZuoraSubscription]
    def fetchAccount(accountNumber: String): ZIO[Any, ZuoraFetchFailure, ZuoraAccount]
  }

  def fetchSubscription(subscriptionNumber: String): ZIO[Zuora, ZuoraFetchFailure, ZuoraSubscription] =
    ZIO.accessM(_.get.fetchSubscription(subscriptionNumber))

  def fetchAccount(accountNumber: String): ZIO[Zuora, ZuoraFetchFailure, ZuoraAccount] =
    ZIO.accessM(_.get.fetchAccount(accountNumber))
}
