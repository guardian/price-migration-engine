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
import pricemigrationengine.model.OptionReader //This import is required do not remove
import pricemigrationengine.model.OptionWriter //This import is required do not remove

import scala.concurrent.duration.{Duration, SECONDS}

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

  private val timeout = Duration(30, SECONDS).toMillis.toInt
  private val connTimeout = HttpOptions.connTimeout(timeout)
  private val readTimeout = HttpOptions.readTimeout(timeout)

  val impl: ZLayer[SalesforceConfiguration with Logging, SalesforceClientFailure, SalesforceClient] =
    ZLayer.fromFunctionM { dependencies: SalesforceConfiguration with Logging =>
      val logging = dependencies.get[Logging.Service]
      val salesforceConfig = dependencies.get[SalesforceConfiguration.Service]

      def requestAsMessage(request: HttpRequest) = {
        s"${request.method} ${request.url}"
      }

      def failureMessage(request: HttpRequest, response: HttpResponse[String]) = {
        requestAsMessage(request) + s" returned status ${response.code} with body:${response.body}"
      }

      def sendRequestAndParseResponse[A](request: HttpRequest)(implicit
          reader: Reader[A]
      ): ZIO[Any, SalesforceClientFailure, A] =
        for {
          valid200Response <- sendRequest(request)
          parsedResponse <-
            IO
              .effect(read[A](valid200Response.body))
              .mapError(ex => SalesforceClientFailure(s"${requestAsMessage(request)} failed to deserialise: $ex"))
        } yield parsedResponse

      def sendRequest(request: HttpRequest): ZIO[Any, SalesforceClientFailure, HttpResponse[String]] =
        for {
          response <-
            IO.effect(request.option(connTimeout).option(readTimeout).asString)
              .mapError(ex => SalesforceClientFailure(s"Request for ${requestAsMessage(request)} failed: $ex"))
          valid200Response <-
            if ((response.code / 100) == 2) {
              IO.succeed(response)
            } else {
              IO.fail(SalesforceClientFailure(failureMessage(request, response)))
            }
        } yield valid200Response

      def auth(config: SalesforceConfig): IO[SalesforceClientFailure, SalesforceAuthDetails] = {
        sendRequestAndParseResponse[SalesforceAuthDetails](
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
        ).tap { _ =>
          logging.info(s"Authenticated with salesforce using user:${config.userName} and client: ${config.clientId}")
        }
      }

      for {
        config <-
          salesforceConfig.config
            .mapError { error => SalesforceClientFailure(s"Failed to load salesforce config: ${error.reason}") }
        auth <- auth(config)
      } yield new SalesforceClient.Service {
        override def getSubscriptionByName(
            subscriptionName: String
        ): IO[SalesforceClientFailure, SalesforceSubscription] =
          sendRequestAndParseResponse[SalesforceSubscription](
            Http(s"${auth.instance_url}/services/data/v43.0/sobjects/SF_Subscription__c/Name/${subscriptionName}")
              .header("Authorization", s"Bearer ${auth.access_token}")
              .method("GET")
          ).tap(subscription => logging.info(s"Successfully loaded: ${subscription.Name}"))

        override def getContact(
            contactId: String
        ): IO[SalesforceClientFailure, SalesforceContact] =
          sendRequestAndParseResponse[SalesforceContact](
            Http(s"${auth.instance_url}/services/data/v43.0/sobjects/Contact/${contactId}")
              .header("Authorization", s"Bearer ${auth.access_token}")
              .method("GET")
          ).tap(contact => logging.info(s"Successfully loaded contact: ${contact.Id}"))

        override def createPriceRise(
            priceRise: SalesforcePriceRise
        ): IO[SalesforceClientFailure, SalesforcePriceRiseCreationResponse] =
          sendRequestAndParseResponse[SalesforcePriceRiseCreationResponse](
            Http(s"${auth.instance_url}/services/data/v43.0/sobjects/Price_Rise__c/")
              .postData(serialisePriceRise(priceRise))
              .header("Authorization", s"Bearer ${auth.access_token}")
              .header("Content-Type", "application/json")
          ).tap(priceRiseId => logging.info(s"Successfully created Price_Rise__c object: ${priceRiseId.id}"))

        override def updatePriceRise(
            priceRiseId: String,
            priceRise: SalesforcePriceRise
        ): IO[SalesforceClientFailure, Unit] =
          sendRequest(
            Http(s"${auth.instance_url}/services/data/v43.0/sobjects/Price_Rise__c/$priceRiseId")
              .postData(serialisePriceRise(priceRise))
              .method("PATCH")
              .header("Authorization", s"Bearer ${auth.access_token}")
              .header("Content-Type", "application/json")
          ).unit
            .tap(_ => logging.info(s"Successfully updated Price_Rise__c object: ${priceRiseId}"))

      }
    }

  def serialisePriceRise(priceRise: SalesforcePriceRise) = {
    write(priceRise, indent = 2)
  }
}
