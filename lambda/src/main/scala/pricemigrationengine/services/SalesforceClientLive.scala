package pricemigrationengine.services

import java.time.LocalDate
import pricemigrationengine.model.{
  SalesforceAddress,
  SalesforceClientFailure,
  SalesforceConfig,
  SalesforceContact,
  SalesforcePriceRise,
  SalesforceSubscription
}
import scalaj.http.{Http, HttpOptions, HttpRequest, HttpResponse}
import upickle.default._
import zio.{IO, ZIO, ZLayer}
import pricemigrationengine.model.OptionReader
import pricemigrationengine.model.OptionWriter
import pricemigrationengine.services
import sttp.client4._
import sttp.client4.Backend
import sttp.client4.httpclient.zio.HttpClientZioBackend
import sttp.client4.httpclient.zio._
import sttp.model.Uri

import scala.concurrent.duration._
import zio.Task

object SalesforceClientLive {

  private case class SalesforceAuthDetails(access_token: String, instance_url: String)

  implicit private val localDateRW: ReadWriter[LocalDate] =
    readwriter[String].bimap[LocalDate](_.toString, LocalDate.parse)
  implicit private val salesforceAuthDetailsRW: ReadWriter[SalesforceAuthDetails] = macroRW
  implicit private val salesforceSubscriptionRW: ReadWriter[SalesforceSubscription] = macroRW
  implicit private val salesforcePriceRiseRW: ReadWriter[SalesforcePriceRise] = macroRW
  implicit private val salesforcePriceIdRiseRW: ReadWriter[SalesforcePriceRiseCreationResponse] = macroRW
  implicit private val salesforceAddressRW: ReadWriter[SalesforceAddress] = macroRW
  implicit private val salesforceContactRW: ReadWriter[SalesforceContact] = macroRW

  implicit private val bigDecimalRW: ReadWriter[BigDecimal] =
    readwriter[ujson.Value].bimap[BigDecimal](
      bd => ujson.Num(bd.toDouble), // write
      js => js.num // read as number
    )

  private val requestTimeout: Duration = 30.seconds
  private val scalajTimeoutMs = 30.seconds.toMillis.toInt
  private val connTimeout = HttpOptions.connTimeout(scalajTimeoutMs)
  private val readTimeout = HttpOptions.readTimeout(scalajTimeoutMs)

  private val salesforceApiPathPrefixToVersion = "services/data/v60.0"

  // old utilities

  private def requestAsMessage(request: HttpRequest) =
    s"${request.method} ${request.url}"

  private def failureMessage(request: HttpRequest, response: HttpResponse[String]) =
    requestAsMessage(request) + s" returned status ${response.code} with body:${response.body}"

  private def sendRequest_old(request: HttpRequest) =
    for {
      response <- ZIO
        .attempt(request.option(connTimeout).option(readTimeout).asString)
        .mapError(ex => SalesforceClientFailure(s"Request for ${requestAsMessage(request)} failed: $ex"))
      valid200Response <-
        if ((response.code / 100) == 2) { ZIO.succeed(response) }
        else { ZIO.fail(SalesforceClientFailure(failureMessage(request, response))) }
    } yield valid200Response

  private def sendRequestAndParseResponse_old[A](request: HttpRequest)(implicit reader: Reader[A]) =
    for {
      valid200Response <- sendRequest_old(request)
      body = valid200Response.body
      _ <- ZIO.logInfo(s"[5b58d83c] Salesforce GET body: ${body}")
      parsedResponse <- ZIO
        .attempt(read[A](body))
        .mapError(ex => SalesforceClientFailure(s"${requestAsMessage(request)} failed to deserialise: $ex"))
    } yield parsedResponse

  // new utilities

  private def performRequestSttpClient4(
      request: Request[String]
  ): ZIO[Any, SalesforceClientFailure, Response[String]] = {
    ZIO.scoped {
      for {
        backend <- HttpClientZioBackend
          .scoped()
          .mapError(ex =>
            SalesforceClientFailure(
              s"Failed to create STTP backend: $ex"
            )
          )
        response <- backend
          .send(request)
          .mapError(ex =>
            SalesforceClientFailure(
              s"Request for ${request.method} ${request.uri} failed: $ex"
            )
          )

        _ <-
          if (response.code.isSuccess)
            ZIO.unit
          else
            ZIO.fail(
              SalesforceClientFailure(
                s"""
                   |(error: 4d8d6c12)
                   |Salesforce request failed
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

  private def performRequestAndParseAnswer[A](
      request: Request[String]
  )(implicit reader: Reader[A]): ZIO[Any, SalesforceClientFailure, A] = {
    for {
      successfulResponse <- performRequestSttpClient4(request)
      body = successfulResponse.body
      _ <- ZIO.logInfo(s"[66412c75] successful response body: ${body}")
      parsedResponse <- ZIO
        .attempt(read[A](body))
        .mapError(ex => SalesforceClientFailure(s"[de6f48da] failed to deserialise: ${body}, error: ${ex}"))
    } yield parsedResponse
  }

  private def makeURI(url: String): Uri = {
    // Note the use of unsafeParse here. The interpolated string is the correct url
    // but `.patch` requires a URI and `Uri(string)` performs escaping. To avoid that
    // we use the `unsafeParse` variant
    Uri.unsafeParse(url)
  }

  // Layer

  val impl: ZLayer[SalesforceConfig with Logging, SalesforceClientFailure, SalesforceClient] =
    ZLayer.fromZIO {

      def auth(config: SalesforceConfig, logging: Logging) = {
        sendRequestAndParseResponse_old[SalesforceAuthDetails](
          Http(s"${config.authUrl}/services/oauth2/token")
            .postForm(
              Seq(
                "grant_type" -> "password",
                "client_id" -> config.clientId,
                "client_secret" -> config.clientSecret,
                "username" -> config.userName,
                "password" -> s"${config.password}${config.token}"
              )
            )
        )
      } <* logging.info(
        s"[c6f8f9f7] Authenticated with salesforce using user:${config.userName} and client: ${config.clientId}"
      )

      for {
        config <- ZIO.service[SalesforceConfig]
        logging <- ZIO.service[Logging]
        auth <- auth(config, logging)
      } yield new services.SalesforceClient {

        override def getSubscriptionByName(
            subscriptionName: String
        ): IO[SalesforceClientFailure, SalesforceSubscription] =
          sendRequestAndParseResponse_old[SalesforceSubscription](
            Http(
              s"${auth.instance_url}/${salesforceApiPathPrefixToVersion}/sobjects/SF_Subscription__c/Name/$subscriptionName"
            )
              .header("Authorization", s"Bearer ${auth.access_token}")
              .method("GET")
          ).tap(subscription =>
            logging.info(s"[ce8f4177] Successfully loaded subscription ${subscription.Name} from Salesforce")
          )

        override def getContact(contactId: String): IO[SalesforceClientFailure, SalesforceContact] =
          sendRequestAndParseResponse_old[SalesforceContact](
            Http(s"${auth.instance_url}/${salesforceApiPathPrefixToVersion}/sobjects/Contact/$contactId")
              .header("Authorization", s"Bearer ${auth.access_token}")
              .method("GET")
          ).tap(contact => logging.info(s"[0309af88] Successfully loaded contact: ${contact.Id}"))

        override def createPriceRise(
            priceRise: SalesforcePriceRise
        ): IO[SalesforceClientFailure, SalesforcePriceRiseCreationResponse] =
          sendRequestAndParseResponse_old[SalesforcePriceRiseCreationResponse](
            Http(s"${auth.instance_url}/${salesforceApiPathPrefixToVersion}/sobjects/Price_Rise__c/")
              .postData(serialisePriceRise(priceRise))
              .header("Authorization", s"Bearer ${auth.access_token}")
              .header("Content-Type", "application/json")
          ).tap(priceRiseId => logging.info(s"[e3e340a7] Successfully created Price_Rise__c object: ${priceRiseId.id}"))

        override def updatePriceRise(
            priceRiseId: String,
            priceRise: SalesforcePriceRise
        ): IO[SalesforceClientFailure, Unit] = {

          val request =
            basicRequest
              .patch(
                makeURI(
                  s"${auth.instance_url}/${salesforceApiPathPrefixToVersion}/sobjects/Price_Rise__c/$priceRiseId"
                )
              )
              .body(serialisePriceRise(priceRise))
              .header("Authorization", s"Bearer ${auth.access_token}")
              .contentType("application/json")
              .response(asStringAlways)
              .readTimeout(requestTimeout)

          performRequestSttpClient4(request).unit
            .tapError(failure => logging.error(s"[bb7d65d1] Failed to update Price_Rise__c object: $failure"))
            .tap(_ => logging.info(s"[bb7d65d1] Successfully updated Price_Rise__c object, priceRiseId: $priceRiseId"))
        }

        override def getPriceRise(priceRiseId: String): IO[SalesforceClientFailure, SalesforcePriceRise] = {

          val request = basicRequest
            .get(
              makeURI(s"${auth.instance_url}/${salesforceApiPathPrefixToVersion}/sobjects/Price_Rise__c/${priceRiseId}")
            )
            .header("Authorization", s"Bearer ${auth.access_token}")
            .contentType("application/json")
            .response(asStringAlways)
            .readTimeout(requestTimeout)

          for {
            priceRise <- performRequestAndParseAnswer[SalesforcePriceRise](request).tap(priceRise =>
              logging.info(
                s"[774f676b] Successfully retrieved Salesforce price rise object, priceRiseId: ${priceRiseId}, priceRise: ${priceRise}"
              )
            )
          } yield priceRise
        }
      }
    }

  private[pricemigrationengine] def serialisePriceRise(priceRise: SalesforcePriceRise) =
    write(priceRise, indent = 2)
}
