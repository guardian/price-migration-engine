package pricemigrationengine.services

import pricemigrationengine.model._
import upickle.default.{ReadWriter, Reader, macroRW, read, write}
import zio.Schedule.{exponential, recurs}
import zio._

import java.time.LocalDate
import ujson._
import sttp.client4._
import sttp.client4.httpclient.zio.HttpClientZioBackend
import sttp.model.Uri

import scala.concurrent.duration.Duration

object ZuoraLive {

  private val apiVersion = "v1"

  // Full import to avoid conflicting with the zio import
  private val readTimeoutSttpClient4: scala.concurrent.duration.Duration =
    scala.concurrent.duration.Duration(
      120,
      scala.concurrent.duration.SECONDS
    )

  private case class AccessToken(access_token: String)
  private implicit val rwAccessToken: ReadWriter[AccessToken] = macroRW

  private case class InvoicePreviewRequest(
      accountId: String,
      targetDate: LocalDate,
      assumeRenewal: String,
      chargeTypeToExclude: String
  )
  private implicit val rwInvoicePreviewRequest: ReadWriter[InvoicePreviewRequest] = macroRW

  private def performRequestSttpClient4(
      request: Request[String]
  ): ZIO[Any, ZuoraFetchFailure, Response[String]] = {
    ZIO.scoped {
      for {
        backend <- HttpClientZioBackend
          .scoped()
          .mapError(ex =>
            ZuoraFetchFailure(
              s"Failed to create STTP backend: $ex"
            )
          )
        response <- backend
          .send(request)
          .mapError(ex =>
            ZuoraFetchFailure(
              s"Request for ${request.method} ${request.uri} failed: $ex"
            )
          )

        _ <-
          if (response.code.isSuccess)
            ZIO.unit
          else
            ZIO.fail(
              ZuoraFetchFailure(
                s"""
                   |(error: 5b325714)
                   |Zuora request failed
                   |Method: ${request.method}
                   |URI: ${request.uri}
                   |Status: ${response.code}
                   |Body: ${response.body}
                   |""".stripMargin
              )
            )
      } yield response
    }
  }

  private def fetchedAccessToken(config: ZuoraConfig): ZIO[Any, ZuoraFetchFailure, String] = {
    val request =
      basicRequest
        .post(makeURI(s"${config.apiHost}/oauth/token"))
        .body(
          Map(
            "grant_type" -> "client_credentials",
            "client_id" -> config.clientId,
            "client_secret" -> config.clientSecret
          )
        )
        .header("Content-Type", "application/x-www-form-urlencoded")
        .response(asStringAlways)
        .readTimeout(readTimeoutSttpClient4)

    performRequestSttpClient4(request).flatMap { response =>
      if (response.code.isSuccess) {
        ZIO
          .attempt(read[AccessToken](response.body).access_token)
          .mapError(_ => ZuoraFetchFailure(s"Failed to parse access token: ${response.body}"))
      } else {
        ZIO.fail(ZuoraFetchFailure(s"""
          |(error: 8b79c2ed)
          |Zuora request failed
          |Method: ${request.method}
          |URI: ${request.uri}
          |Status: ${response.code}
          |Body: ${response.body}
          |""".stripMargin))
      }
    }
  }

  private def performRequestAndParseAnswer[A](
      request: Request[String]
  )(implicit reader: Reader[A]): ZIO[Any, ZuoraFetchFailure, A] = {
    for {
      successfulResponse <- performRequestSttpClient4(request)
      body = successfulResponse.body
      _ <- ZIO.logInfo(s"[e2b0236b] successful response body: ${body}")
      parsedResponse <- ZIO
        .attempt(read[A](body))
        .mapError(ex => ZuoraFetchFailure(s"[c6691aea] failed to deserialise: ${body}, error: ${ex}"))
    } yield parsedResponse
  }

  private def makeURI(url: String): Uri = {
    // Note the use of unsafeParse here. The interpolated string is the correct url
    // but `.patch` requires a URI and `Uri(string)` performs escaping. To avoid that
    // we use the `unsafeParse` variant
    Uri.unsafeParse(url)
  }

  val impl: ZLayer[ZuoraConfig with Logging, ConfigFailure, Zuora] =
    ZLayer.fromZIO(
      for {
        logging <- ZIO.service[Logging]
        config <- ZIO.service[ZuoraConfig]
        accessToken <- fetchedAccessToken(config)
          .mapError(failure => ConfigFailure(failure.reason))
          .tap(token => logging.info(s"Fetched Zuora access token: $token"))
      } yield new Zuora {

        private def retry[E, A](effect: => ZIO[Any, E, A]) =
          effect.retry(exponential(1.second) && recurs(5))

        private def get[A: Reader](
            path: String,
            params: Map[String, String] = Map.empty
        ): ZIO[Any, ZuoraFetchFailure, A] = {
          val request =
            basicRequest
              .get(makeURI(s"${config.apiHost}/$apiVersion/$path").addParams(params))
              .header("Authorization", s"Bearer $accessToken")
              .response(asStringAlways)

          retry(
            performRequestAndParseAnswer[A](request)
              .mapError(e => ZuoraFetchFailure(e.reason))
          )
        }

        private def post[A: Reader](path: String, body: String) = {
          val request =
            basicRequest
              .post(makeURI(s"${config.apiHost}/$apiVersion/$path"))
              .body(body)
              .header("Authorization", s"Bearer $accessToken")
              .contentType("application/json")
              .response(asStringAlways)
              .readTimeout(readTimeoutSttpClient4)

          performRequestAndParseAnswer[A](request)
            .mapError(e => ZuoraUpdateFailure(e.reason))
        }

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

        private def jobMonitor(jobId: String): ZIO[Any, Any, Either[String, Unit]] = {
          // This function takes a jobId, from an async processing request and either
          // 1. Returns Right(Unit) is the job has completed normally, this should result in a ZIO succeed of the calling code.
          // 2. Returns Left[String] if (and as soon as ) we encounter a Failure from Zuora, this
          //    should result in a ZIO fail of the calling code.
          // 3. A Failure if the processing could not complete (either successfully in Zuora or as a failure in Zuora),
          //    essentially if the .retry failed, this should result in a ZIO fail of the calling code.
          (for {
            jobReport <- getJobReport(jobId)
            _ <- logging.info(
              s"[4b4e379c] jobReport: ${jobReport}"
            )
            result <- {
              if (AsyncJobReport.isCompletedCompleted(jobReport)) { ZIO.succeed(Right(())) }
              else if (AsyncJobReport.isCompleted(jobReport) && !AsyncJobReport.isCompletedCompleted(jobReport)) {
                ZIO.succeed(
                  Left(
                    "[8a50192a] The process has completed but not with a completed result. Should investigate. (Possibly and order in Pending state)."
                  )
                )
              } else if (AsyncJobReport.hasFailed(jobReport)) {
                ZIO.succeed(Left(jobReport.errors.getOrElse("(empty errors string from the job report)")))
              } else {
                // Although we are raising a ZIO.fail here,
                // This case only means that we are waiting for results
                ZIO.fail(())
              }
            }

          } yield result)
            .retry(
              // Query every 2 seconds for a total of 150 times (eg: 5 minutes)
              Schedule.spaced(2.second) && Schedule.recurs(150)
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
            _ <- logging.info(
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
            _ <- logging.info(
              s"[fe478094] submitted asynchronous order for subscription ${subscriptionNumber}, operation: ${operationDescriptionForLogging}, submission ticket: ${submissionTicket}"
            )
            monitorResult <- jobMonitor(submissionTicket.jobId)
              .mapError(e =>
                // Look like we have timed out during the monitoring and the .retry gave up
                ZuoraAsynchronousOrderRequestFailure(
                  s"[462b80c6] error while evaluating asynchronous job report 🤔, jobId: ${submissionTicket.jobId}, error: ${e}"
                )
              )
            _ <- ZIO
              .fromEither(monitorResult)
              .mapError(e =>
                // Looks like Zuora properly failed the request.
                ZuoraAsynchronousOrderRequestFailure(
                  s"[5eed7eb0] We got a Left from the monitor 🤔, jobId: ${submissionTicket.jobId}, error: ${e}"
                )
              )
            _ <- logging.info(
              s"[62d66c48] completed asynchronous order for subscription ${subscriptionNumber}, operation: ${operationDescriptionForLogging}"
            )
          } yield ZIO.succeed(())
        }
      }
    )
}
