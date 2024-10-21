package pricemigrationengine.services

import pricemigrationengine.model._
import scalaj.http.{Http, HttpOptions, HttpRequest, HttpResponse}
import upickle.default.{ReadWriter, Reader, macroRW, read, write}
import zio.Schedule.{exponential, recurs}
import zio._

import java.time.LocalDate

object ZuoraLive {

  private val apiVersion = "v1"

  private val timeout = 30.seconds.toMillis.toInt
  private val connTimeout = HttpOptions.connTimeout(timeout)
  private val readTimeout = HttpOptions.readTimeout(timeout)

  private case class AccessToken(access_token: String)
  private implicit val rwAccessToken: ReadWriter[AccessToken] = macroRW

  private case class InvoicePreviewRequest(
      accountId: String,
      targetDate: LocalDate,
      assumeRenewal: String,
      chargeTypeToExclude: String
  )
  private implicit val rwInvoicePreviewRequest: ReadWriter[InvoicePreviewRequest] = macroRW

  private case class SubscriptionUpdateResponse(subscriptionId: ZuoraSubscriptionId)
  private implicit val rwSubscriptionUpdateResponse: ReadWriter[SubscriptionUpdateResponse] = macroRW

  /*
   * The access token is generated outside the ZIO framework so that it's only fetched once.
   * There has to be a better way to do this, but don't know what it is at the moment.
   * Possibly memoize the value at a higher level.
   * Will return to it.
   */
  private def fetchedAccessToken(config: ZuoraConfig) = {
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
    if (response.code == 200) {
      Right(read[AccessToken](body).access_token)
        .orElse(Left(ZuoraFetchFailure(failureMessage(request, response))))
    } else
      Left(ZuoraFetchFailure(failureMessage(request, response)))
  }

  private def failureMessage(request: HttpRequest, response: HttpResponse[String]) = {
    s"Request for ${request.method} ${request.url} returned status ${response.code} with body:${response.body}"
  }

  private def failureMessage(request: HttpRequest, t: Throwable) =
    s"Request for ${request.method} ${request.url} returned error ${t.toString}"

  val impl: ZLayer[ZuoraConfig with Logging, ConfigFailure, Zuora] =
    ZLayer.fromZIO(
      for {
        logging <- ZIO.service[Logging]
        config <- ZIO.service[ZuoraConfig]
        accessToken <- ZIO
          .fromEither(fetchedAccessToken(config))
          .mapError(failure => ConfigFailure(failure.reason))
          .tap(token => logging.info(s"Fetched Zuora access token: $token"))
      } yield new Zuora {

        private def retry[E, A](effect: => ZIO[Any, E, A]) =
          effect.retry(exponential(1.second) && recurs(5))

        private def get[A: Reader](path: String, params: Map[String, String] = Map.empty) = {
          for {
            a <- retry(
              handleRequest[A](
                Http(s"${config.apiHost}/$apiVersion/$path")
                  .params(params)
                  .header("Authorization", s"Bearer $accessToken")
              ).mapError(e => ZuoraFetchFailure(e.reason))
            )
          } yield a
        }

        private def post[A: Reader](path: String, body: String) =
          for {
            a <- handleRequest[A](
              Http(s"${config.apiHost}/$apiVersion/$path")
                .header("Authorization", s"Bearer $accessToken")
                .header("Content-Type", "application/json")
                .postData(body)
            ).mapError(e => ZuoraUpdateFailure(e.reason))
          } yield a

        private def put[A: Reader](path: String, body: String) =
          for {
            a <- handleRequest[A](
              Http(s"${config.apiHost}/$apiVersion/$path")
                .header("Authorization", s"Bearer $accessToken")
                .header("Content-Type", "application/json")
                .put(body)
            ).mapError(e => ZuoraUpdateFailure(e.reason))
          } yield a

        private def handleRequest[A: Reader](request: HttpRequest) =
          for {
            response <-
              ZIO
                .attempt(request.option(connTimeout).option(readTimeout).asString)
                .mapError(e => ZuoraFailure(failureMessage(request, e)))
                .filterOrElseWith(_.code == 200)(response => ZIO.fail(ZuoraFailure(failureMessage(request, response))))
            a <-
              ZIO
                .attempt(read[A](response.body))
                .orElseFail(ZuoraFailure(failureMessage(request, response)))
          } yield a

        override def fetchSubscription(subscriptionNumber: String): ZIO[Any, ZuoraFetchFailure, ZuoraSubscription] =
          get[ZuoraSubscription](s"subscriptions/$subscriptionNumber")
            .mapError(e => ZuoraFetchFailure(s"Subscription $subscriptionNumber: ${e.reason}"))
            .tapBoth(
              e => logging.error(s"Failed to fetch subscription $subscriptionNumber: $e"),
              _ => logging.info(s"Fetched subscription $subscriptionNumber")
            )

        override def fetchAccount(
            accountNumber: String,
            subscriptionNumber: String
        ): ZIO[Any, ZuoraFetchFailure, ZuoraAccount] =
          get[ZuoraAccount](s"accounts/$accountNumber")
            .mapError(e =>
              ZuoraFetchFailure(s"Account ${accountNumber} for subscription $subscriptionNumber: ${e.reason}")
            )
            .tapBoth(
              e => logging.error(s"Failed to fetch account ${accountNumber} for subscription $subscriptionNumber: $e"),
              _ => logging.info(s"Fetched account $accountNumber for subscription $subscriptionNumber")
            )

        // See https://www.zuora.com/developer/api-reference/#operation/POST_BillingPreviewRun
        override def fetchInvoicePreview(
            accountId: String,
            targetDate: LocalDate
        ): ZIO[Any, ZuoraFetchFailure, ZuoraInvoiceList] = {
          retry(
            post[ZuoraInvoiceList](
              path = "operations/billing-preview",
              body = write(
                InvoicePreviewRequest(
                  accountId,
                  targetDate,
                  assumeRenewal = "Autorenew",
                  chargeTypeToExclude = "OneTime"
                )
              )
            ).mapError(e => ZuoraFetchFailure(s"Invoice preview for account $accountId: ${e.reason}"))
          )
            .tapBoth(
              e => logging.error(s"Failed to fetch invoice preview for account $accountId: $e"),
              _ => logging.info(s"Fetched invoice preview for account $accountId")
            )
        }

        override val fetchProductCatalogue: ZIO[Any, ZuoraFetchFailure, ZuoraProductCatalogue] = {
          def fetchPage(idx: Int): ZIO[Any, ZuoraFetchFailure, ZuoraProductCatalogue] =
            get[ZuoraProductCatalogue](path = "catalog/products", params = Map("page" -> idx.toString))
              .mapError(e => ZuoraFetchFailure(s"Product catalogue: ${e.reason}"))
              .tapBoth(
                e => logging.error(s"Failed to fetch product catalogue page $idx: $e"),
                _ => logging.info(s"Fetched product catalogue page $idx")
              )

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
              catalogue <-
                if (hasNextPage(curr)) {
                  fetchCatalogue(soFar, pageIdx + 1)
                } else ZIO.succeed(soFar)
            } yield catalogue

          fetchCatalogue(ZuoraProductCatalogue.empty, pageIdx = 1)
        }

        override def updateSubscription(
            subscription: ZuoraSubscription,
            update: ZuoraSubscriptionUpdate
        ): ZIO[Any, ZuoraUpdateFailure, ZuoraSubscriptionId] = {
          put[SubscriptionUpdateResponse](
            path = s"subscriptions/${subscription.subscriptionNumber}",
            body = write(update)
          ).mapBoth(
            e => ZuoraUpdateFailure(s"Subscription ${subscription.subscriptionNumber} and update $update: ${e.reason}"),
            response => response.subscriptionId
          )
        }

        override def renewSubscription(subscriptionNumber: String): ZIO[Any, ZuoraRenewalFailure, Unit] = {

          val triggerDates = List(
            ZuoraRenewOrderPayloadOrderActionTriggerDate(
              "ContractEffective",
              LocalDate.of(2024, 10, 21)
            ), // TODO: set correct date
            ZuoraRenewOrderPayloadOrderActionTriggerDate(
              "ServiceActivation",
              LocalDate.of(2024, 10, 21)
            ), // TODO: set correct date
            ZuoraRenewOrderPayloadOrderActionTriggerDate(
              "CustomerAcceptance",
              LocalDate.of(2024, 10, 21)
            ), // TODO: set correct date
          )

          val orderActions = List(
            ZuoraRenewOrderPayloadOrderAction(
              `type` = "RenewSubscription",
              triggerDates = triggerDates
            )
          )

          val subscriptions = List(
            ZuoraRenewOrderPayloadSubscription(
              subscriptionNumber = subscriptionNumber,
              orderActions = orderActions
            )
          )

          val processingOptions = ZuoraRenewOrderPayloadProcessingOptions(runBilling = false, collectPayment = false)

          val payload = ZuoraRenewOrderPayload(
            orderDate = LocalDate.of(2024, 10, 21), // TODO: set correct date
            existingAccountNumber = "A01269270", // TODO: set correct account number
            subscriptions = subscriptions,
            processingOptions = processingOptions
          )

          retry(
            post[Unit](
              path = s"v1/orders/",
              body = write(payload)
            ).mapBoth(
              e =>
                ZuoraRenewalFailure(
                  s"Failed to renew subscription number ${subscriptionNumber} with payload: ${payload}"
                ),
              response => ()
            )
          ) <* logging.info(s"renewed subscription ${subscriptionNumber}")
        }
      }
    )
}
