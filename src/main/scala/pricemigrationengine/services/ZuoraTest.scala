package pricemigrationengine.services

import java.time.LocalDate

import pricemigrationengine.model._
import zio.{ZIO, ZLayer}

object ZuoraTest {
  val impl: ZLayer[Any, Throwable, Zuora] = ZLayer.succeed(
    new Zuora.Service {

      def fetchSubscription(name: String): ZIO[Any, ZuoraFetchFailure, ZuoraSubscription] =
        ZIO.fail(ZuoraFetchFailure("failure!"))

      def fetchInvoicePreview(accountId: String): ZIO[Any, ZuoraFetchFailure, ZuoraInvoiceList] =
        ZIO.succeed(ZuoraInvoiceList(Seq(ZuoraInvoiceItem(LocalDate.now, 12.34, "C-1234"))))

      val fetchProductCatalogue: ZIO[Any, ZuoraFetchFailure, ZuoraProductCatalogue] =
        ZIO.succeed(ZuoraProductCatalogue(Set.empty[ZuoraProduct]))
    }
  )
}
