package pricemigrationengine.services

import java.time.LocalDate

import pricemigrationengine.model._
import scalaj.http.{Http, HttpOptions, HttpRequest}
import upickle.default._
import zio.{ZIO, ZLayer}

import scala.concurrent.duration._

object ZuoraLive {
  val impl: ZLayer[Configuration with Logging, ConfigurationFailure, Zuora] = ZLayer
    .fromServicesM[
      Configuration.Service,
      Logging.Service,
      Configuration with Logging,
      ConfigurationFailure,
      Zuora.Service
    ] { (configuration, logging) =>
      configuration.config map { config =>
        new Zuora.Service {

          private val readTimeout = HttpOptions.readTimeout(Duration(30, SECONDS).toMillis.toInt)

          private case class AccessToken(access_token: String)
          private implicit val rwAccessToken: ReadWriter[AccessToken] = macroRW

          private case class InvoicePreviewRequest(accountId: String, targetDate: LocalDate)
          private implicit val rwInvoicePreviewRequest: ReadWriter[InvoicePreviewRequest] = macroRW

          private lazy val fetchedAccessToken: ZIO[Any, ZuoraFetchFailure, String] =
            handleRequest[AccessToken](
              Http(s"${config.zuora.apiHost}/oauth/token")
                .postForm(
                  Seq(
                    "grant_type" -> "client_credentials",
                    "client_id" -> config.zuora.clientId,
                    "client_secret" -> config.zuora.clientSecret
                  )
                )
            ).map(_.access_token)
              .tap(_ => logging.info("Fetched Zuora access token"))

          private def get[A: Reader](path: String): ZIO[Any, ZuoraFetchFailure, A] =
            fetchedAccessToken flatMap { accessToken =>
              handleRequest[A](
                Http(s"${config.zuora.apiHost}/v1/$path")
                  .header("Authorization", s"Bearer $accessToken")
              )
            }

          private def post[A: Reader](path: String, body: String): ZIO[Any, ZuoraFetchFailure, A] =
            fetchedAccessToken flatMap { accessToken =>
              handleRequest[A](
                Http(s"${config.zuora.apiHost}/v1/$path")
                  .header("Authorization", s"Bearer $accessToken")
                  .header("Content-Type", "application/json")
                  .postData(body)
              )
            }

          private def handleRequest[A: Reader](request: HttpRequest): ZIO[Any, ZuoraFetchFailure, A] = {
            val response = request
              .option(readTimeout)
              .asString
            val body = response.body
            if (response.code == 200)
              ZIO
                .effect(read[A](body))
                .orElse(ZIO.fail(ZuoraFetchFailure(body)))
            else
              ZIO.fail(ZuoraFetchFailure(body))
          }

          def fetchSubscription(subscriptionNumber: String): ZIO[Any, ZuoraFetchFailure, ZuoraSubscription] =
            get[ZuoraSubscription](s"subscriptions/$subscriptionNumber")
              .mapError(e => ZuoraFetchFailure(s"Subscription $subscriptionNumber: ${e.reason}"))
              .tap(_ => logging.info(s"Fetched subscription $subscriptionNumber"))

          def fetchInvoicePreview(accountId: String): ZIO[Any, ZuoraFetchFailure, ZuoraInvoiceList] =
            post[ZuoraInvoiceList](
              path = "operations/billing-preview",
              body = write(InvoicePreviewRequest(accountId, targetDate = config.yearInFuture))
            ).mapError(e => ZuoraFetchFailure(s"Invoice preview for account $accountId: ${e.reason}"))
              .tap(_ => logging.info(s"Fetched invoice preview for account $accountId"))

          val fetchProductCatalogue: ZIO[Any, ZuoraFetchFailure, ZuoraProductCatalogue] =
            get[ZuoraProductCatalogue]("catalog/products")
              .mapError(e => ZuoraFetchFailure(s"Product catalogue: ${e.reason}"))
              .tap(_ => logging.info(s"Fetched product catalogue"))
        }
      }
    }
}
