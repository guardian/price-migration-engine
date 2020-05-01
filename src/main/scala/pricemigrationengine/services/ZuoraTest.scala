package pricemigrationengine.services

import pricemigrationengine.model._
import zio.{ZIO, ZLayer}

object ZuoraTest {
  val impl: ZLayer[Any, Throwable, Zuora] = ZLayer.succeed(
    new Zuora.Service {

      def fetchSubscription(name: String): ZIO[Any, ZuoraFetchFailure, ZuoraSubscription] =
        ZIO.fail(ZuoraFetchFailure("failure!"))

      def fetchAccount(id: String): ZIO[Any, ZuoraFetchFailure, ZuoraAccount] =
        ZIO.succeed(ZuoraAccount(ZuoraAccountBillingAndPayment(0)))
    }
  )
}
