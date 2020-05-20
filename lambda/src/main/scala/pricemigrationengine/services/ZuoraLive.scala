package pricemigrationengine.services

import java.time.LocalDate

import pricemigrationengine.model._
import scalaj.http.{Http, HttpOptions, HttpRequest, HttpResponse}
import upickle.default._
import zio.{ZIO, ZLayer}

import scala.concurrent.duration._

object ZuoraLive {
  val impl: ZLayer[ZuoraConfiguration with Logging, ConfigurationFailure, Zuora] = ZLayer
    .fromServicesM[
      ZuoraConfiguration.Service,
      Logging.Service,
      ZuoraConfiguration with Logging,
      ConfigurationFailure,
      Zuora.Service
    ] { (configuration, logging) =>
      configuration.config map { config =>
        new Zuora.Service {

          private val apiVersion = "v1"

          private val readTimeout = HttpOptions.readTimeout(Duration(30, SECONDS).toMillis.toInt)

          private case class AccessToken(access_token: String)
          private implicit val rwAccessToken: ReadWriter[AccessToken] = macroRW

          private case class InvoicePreviewRequest(accountId: String, targetDate: LocalDate)
          private implicit val rwInvoicePreviewRequest: ReadWriter[InvoicePreviewRequest] = macroRW

          /*
           * The access token is generated outside the ZIO framework so that it's only fetched once.
           * There has to be a better way to do this, but don't know what it is at the moment.
           * Possibly memoize the value at a higher level.
           * Will return to it.
           */
          private lazy val fetchedAccessToken: Either[ZuoraFetchFailure, String] = {
            val request = Http(s"${config.apiHost}/oauth/token")
              .postForm(
                Seq(
                  "grant_type" -> "client_credentials",
                  "client_id" -> config.clientId,
                  "client_secret" -> config.clientSecret
                )
              )
            val response = request.asString
            val body = response.body
            if (response.code == 200)
              Right(read[AccessToken](body).access_token)
                .orElse(Left(ZuoraFetchFailure(failureMessage(request, response))))
            else
              Left(ZuoraFetchFailure(failureMessage(request, response)))
          }

          private def get[A: Reader](path: String, params: Map[String, String] = Map.empty): ZIO[Any, Failure, A] =
            for {
              accessToken <- ZIO.fromEither(fetchedAccessToken)
              a <- handleRequest[A](
                Http(s"${config.apiHost}/$apiVersion/$path")
                  .params(params)
                  .header("Authorization", s"Bearer $accessToken")
              )
            } yield a

          private def post[A: Reader](path: String, body: String): ZIO[Any, Failure, A] =
            for {
              accessToken <- ZIO.fromEither(fetchedAccessToken)
              a <- handleRequest[A](
                Http(s"${config.apiHost}/$apiVersion/$path")
                  .header("Authorization", s"Bearer $accessToken")
                  .header("Content-Type", "application/json")
                  .postData(body)
              )
            } yield a

          private def put[A: Reader](path: String, body: String): ZIO[Any, Failure, A] =
            for {
              accessToken <- ZIO.fromEither(fetchedAccessToken)
              a <- handleRequest[A](
                Http(s"${config.apiHost}/$apiVersion/$path")
                  .header("Authorization", s"Bearer $accessToken")
                  .header("Content-Type", "application/json")
                  .put(body)
              )
            } yield a

          private def handleRequest[A: Reader](request: HttpRequest): ZIO[Any, Failure, A] = {
            val response = request
              .option(readTimeout)
              .asString
            val body = response.body
            if (response.code == 200)
              ZIO
                .effect(read[A](body))
                .orElse(ZIO.fail(ZuoraFetchFailure(failureMessage(request, response))))
            else
              ZIO.fail(ZuoraFetchFailure(failureMessage(request, response)))
          }

          private def failureMessage(request: HttpRequest, response: HttpResponse[String]) = {
            s"Request for ${request.method} ${request.url} returned status ${response.code} with body:${response.body}"
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

          val fetchProductCatalogue: ZIO[Any, ZuoraFetchFailure, ZuoraProductCatalogue] = {

            def fetchPage(idx: Int): ZIO[Any, ZuoraFetchFailure, ZuoraProductCatalogue] =
              get[ZuoraProductCatalogue](path = "catalog/products", params = Map("page" -> idx.toString))
                .mapError(e => ZuoraFetchFailure(s"Product catalogue: ${e.reason}"))
                .tap(_ => logging.info(s"Fetched product catalogue page $idx"))

            def hasNextPage(catalogue: ZuoraProductCatalogue) = catalogue.nextPage.isDefined

            def combine(c1: ZuoraProductCatalogue, c2: ZuoraProductCatalogue) =
              ZuoraProductCatalogue(products = c1.products ++ c2.products)

            def fetchCatalogue(
                acc: ZuoraProductCatalogue,
                pageIdx: Int
            ): ZIO[Any, ZuoraFetchFailure, ZuoraProductCatalogue] =
              for {
                curr <- fetchPage(pageIdx)
                soFar = combine(acc, curr)
                catalogue <- if (hasNextPage(curr)) {
                  fetchCatalogue(soFar, pageIdx + 1)
                } else ZIO.succeed(soFar)
              } yield catalogue

            fetchCatalogue(ZuoraProductCatalogue.empty, pageIdx = 1)
          }

          def updateSubscription(
              subscription: ZuoraSubscription,
              update: ZuoraSubscriptionUpdate
          ): ZIO[Any, ZuoraUpdateFailure, ZuoraSubscription] =
            put[ZuoraSubscription](
              path = s"subscriptions/${subscription.subscriptionNumber}",
              body = write(update)
            ).mapError(
                e =>
                  ZuoraUpdateFailure(s"Subscription ${subscription.subscriptionNumber} and update $update: ${e.reason}")
              )
              .tap(_ => logging.info(s"Updated subscription ${subscription.subscriptionNumber} with: $update"))
        }
      }
    }
}
