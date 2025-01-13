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
  object ApplyAmendmentOrder extends Effect[(ZuoraSubscription, ZuoraAmendmentOrderPayload), ZuoraOrderFailure, Unit]
  object RenewSubscription extends Effect[String, ZuoraRenewalFailure, Unit]

  val compose: URLayer[Proxy, Zuora] = ZLayer.fromZIO(ZIO.service[Proxy].map { proxy =>
    new Zuora {

      override def fetchSubscription(subscriptionNumber: String): ZIO[Any, ZuoraFetchFailure, ZuoraSubscription] =
        proxy(FetchSubscription, subscriptionNumber)

      override def fetchAccount(
          accountNumber: String,
          subscriptionNumber: String
      ): ZIO[Any, ZuoraFetchFailure, ZuoraAccount] =
        proxy(FetchAccount, accountNumber, subscriptionNumber)

      override def fetchInvoicePreview(
          accountId: String,
          targetDate: LocalDate
      ): ZIO[Any, ZuoraFetchFailure, ZuoraInvoiceList] = proxy(FetchInvoicePreview, accountId, targetDate)

      override val fetchProductCatalogue: ZIO[Any, ZuoraFetchFailure, ZuoraProductCatalogue] =
        proxy(FetchProductCatalogue)

      override def updateSubscription(
          subscription: ZuoraSubscription,
          update: ZuoraSubscriptionUpdate
      ): ZIO[Any, ZuoraUpdateFailure, ZuoraSubscriptionId] =
        proxy(UpdateSubscription, subscription, update)

      override def applyAmendmentOrder(
          subscription: ZuoraSubscription,
          payload: ZuoraAmendmentOrderPayload
      ): ZIO[Any, ZuoraOrderFailure, Unit] = proxy(ApplyAmendmentOrder, subscription, payload)

      override def renewSubscription(
          subscriptionNumber: String,
          payload: ZuoraRenewOrderPayload
      ): ZIO[Any, ZuoraRenewalFailure, Unit] =
        proxy(RenewSubscription, subscriptionNumber)
    }
  })
}
