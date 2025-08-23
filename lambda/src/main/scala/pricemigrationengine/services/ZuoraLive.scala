package pricemigrationengine.services

import pricemigrationengine.model._
import scalaj.http.{Http, HttpOptions, HttpRequest, HttpResponse}
import upickle.default.{ReadWriter, Reader, macroRW, read, write}
import zio.Schedule.{exponential, recurs}
import zio._

import java.time.LocalDate
import ujson._

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
    s"[a16466b3] Request for ${request.method} ${request.url} returned status ${response.code} with body:${response.body}"
  }

  private def failureMessage(request: HttpRequest, t: Throwable) =
    s"[08ee1348] Request for ${request.method} ${request.url} returned error ${t.toString}"

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
              e => logging.error(s"[4b4b9e39] Failed to fetch subscription $subscriptionNumber: $e"),
              _ => logging.info(s"[4f1645c4] Fetched subscription $subscriptionNumber")
            )

        override def fetchAccount(
            accountNumber: String,
            subscriptionNumber: String
        ): ZIO[Any, ZuoraFetchFailure, ZuoraAccount] =
          get[ZuoraAccount](s"accounts/$accountNumber")
            .mapError(e =>
              ZuoraFetchFailure(
                s"[2b254d19] Account ${accountNumber} for subscription $subscriptionNumber: ${e.reason}"
              )
            )
            .tapBoth(
              e =>
                logging.error(
                  s"[8a07429d] Failed to fetch account ${accountNumber} for subscription $subscriptionNumber: $e"
                ),
              _ => logging.info(s"[7951c941] Fetched account $accountNumber for subscription $subscriptionNumber")
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
            ).mapError(e => ZuoraFetchFailure(s"[9445e7fd] Invoice preview for account $accountId: ${e.reason}"))
          )
            .tapBoth(
              e => logging.error(s"[26de9125] Failed to fetch invoice preview for account $accountId: $e"),
              _ => logging.info(s"[45d846a0] Fetched invoice preview for account $accountId")
            )
        }

        override val fetchProductCatalogue: ZIO[Any, ZuoraFetchFailure, ZuoraProductCatalogue] = {
          def fetchPage(idx: Int): ZIO[Any, ZuoraFetchFailure, ZuoraProductCatalogue] =
            get[ZuoraProductCatalogue](path = "catalog/products", params = Map("page" -> idx.toString))
              .mapError(e => ZuoraFetchFailure(s"Product catalogue: ${e.reason}"))
              .tapBoth(
                e => logging.error(s"[fdf4fe69] Failed to fetch product catalogue page $idx: $e"),
                _ => logging.info(s"[50dbdee6] Fetched product catalogue page $idx")
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

        override def applyAmendmentOrder_typed_deprecated(
            subscription: ZuoraSubscription,
            payload: ZuoraAmendmentOrderPayload
        ): ZIO[Any, ZuoraOrderFailure, Unit] = {

          // The existence of type_flush is explained in comment cada56ad.
          // This is, for all intent and purpose a hack due to the way upickle deals with sealed traits
          // and in a future change we will get rid of it, either by changing JSON library or coming up
          // with the correct writers.

          def type_flush(str: String): String = {
            str
              .replace(""""$type":"ZuoraAmendmentOrderPayloadOrderActionAdd",""", "")
              .replace(""""$type":"ZuoraAmendmentOrderPayloadOrderActionRemove",""", "")
          }

          val body = type_flush(write(payload))

          post[ZuoraAmendmentOrderResponse](
            path = s"orders",
            body = type_flush(write(payload))
          ).foldZIO(
            failure = e =>
              ZIO.fail(
                ZuoraOrderFailure(
                  s"[f8569839] subscription number: ${subscription.subscriptionNumber}, payload: ${payload}, reason: ${e.reason}"
                )
              ),
            success = response =>
              if (response.success) {
                ZIO.succeed(())
              } else {
                ZIO.fail(
                  ZuoraOrderFailure(
                    s"[bb6f22ef] subscription number: ${subscription.subscriptionNumber}, payload: ${payload}, serialised payload: ${body}, with answer ${response}"
                  )
                )
              }
          )
        }

        private def submitAsynchronousOrderRequest(
            subscriptionNumber: String,
            payload: Value
        ): ZIO[Any, ZuoraAsynchronousOrderRequestFailure, AsyncJobSubmissionTicket] = {
          val payload_stringified = payload.toString()
          post[AsyncJobSubmissionTicket](
            path = "async/orders",
            body = payload_stringified
          ).foldZIO(
            failure = e =>
              ZIO.fail(
                ZuoraAsynchronousOrderRequestFailure(
                  s"[c8158a4f] subscription number: ${subscriptionNumber}, payload: ${payload}, reason: ${e.reason}"
                )
              ),
            success = response => ZIO.succeed(response)
          )
        }

        private def getJobReport(jobId: String): ZIO[Any, ZuoraGetJobStatusFailure, AsyncJobReport] = {
          get[AsyncJobReport](s"async-jobs/${jobId}")
            .mapError(e =>
              ZuoraGetJobStatusFailure(
                s"[deb14905] Could not retrieve job report for jobId: ${jobId}, reason: ${e.reason}"
              )
            )
        }

        override def applyOrderAsynchronously(
            subscriptionNumber: String,
            payload: Value,
            operationDescriptionForLogging: String
        ): ZIO[Any, ZuoraAsynchronousOrderRequestFailure, Unit] = {
          // Note: This function was introduced in August 2025, to support the print migrations.
          // Some subscriptions with a large number of amendments would timeout during a
          // synchronous renewal or price amendment request, but extensive tests ran
          // with asynchronous requests showed a 100% success rate within less than 20 seconds.
          //
          // The retry schedule was originally meant to be
          // `.retry(Schedule.spaced(2.second) && Schedule.duration(5.minutes))`
          // but that actually didn't work, the effect was exiting after 5 minutes
          // despite the order executing within seconds 🤔. I then set it to
          // `.retry(Schedule.spaced(2.second))`.
          // If for any reason Zuora doesn't succeed that job, the lambda is going to
          // be terminated by AWS.
          for {
            _ <- ZIO.logInfo(
              s"[18943ad2] submitting asynchronous order for subscription ${subscriptionNumber}, operation: ${operationDescriptionForLogging}, payload: ${payload}"
            )
            submissionTicket <- submitAsynchronousOrderRequest(
              subscriptionNumber,
              payload
            ).mapError(e =>
              ZuoraAsynchronousOrderRequestFailure(
                s"[847e2075] error while submitting asynchronous order for subscription ${subscriptionNumber}, operation: ${operationDescriptionForLogging}, reason: ${e.reason}"
              )
            )
            _ <-
              if (submissionTicket.success) { ZIO.unit }
              else {
                ZIO.fail(
                  ZuoraAsynchronousOrderRequestFailure(
                    s"[65d1d2fa] Zuora has not accepted an asynchronous order for ${subscriptionNumber}, operation: ${operationDescriptionForLogging}, payload: ${payload}"
                  )
                )
              }
            _ <- ZIO.logInfo(
              s"[fe478094] submitted asynchronous order for subscription ${subscriptionNumber}, operation: ${operationDescriptionForLogging}, submission ticket: ${submissionTicket}"
            )
            _ <- (for {
              jobReport <- getJobReport(submissionTicket.jobId)
              _ <- ZIO.logInfo(
                s"[4b4e379c] jobReport: ${jobReport}"
              )
              _ <-
                if (AsyncJobReport.isReady(jobReport)) { ZIO.unit }
                else { ZIO.fail(()) }
            } yield ())
              .retry(Schedule.spaced(2.second))
              .mapError(e =>
                ZuoraAsynchronousOrderRequestFailure(
                  s"[462b80c6] error while evaluating asynchronous job report 🤔, jobId: ${submissionTicket.jobId}, error: ${e}"
                )
              )
            _ <- ZIO.logInfo(
              s"[62d66c48] completed asynchronous order for subscription ${subscriptionNumber}, operation: ${operationDescriptionForLogging}"
            )
          } yield ZIO.succeed(())
        }
      }
    )
}
