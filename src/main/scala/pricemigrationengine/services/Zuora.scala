package pricemigrationengine.services

import pricemigrationengine.model.{ZuoraAccount, ZuoraFetchFailure, ZuoraSubscription}
import zio.ZIO

object Zuora {

  trait Service {
    def fetchSubscription(name: String): ZIO[Any, ZuoraFetchFailure, ZuoraSubscription]
    def fetchAccount(id: String): ZIO[Any, ZuoraFetchFailure, ZuoraAccount]
  }

  def fetchSubscription(name: String): ZIO[Zuora, ZuoraFetchFailure, ZuoraSubscription] =
    ZIO.accessM(_.get.fetchSubscription(name))

  def fetchAccount(id: String): ZIO[Zuora, ZuoraFetchFailure, ZuoraAccount] =
    ZIO.accessM(_.get.fetchAccount(id))
}
