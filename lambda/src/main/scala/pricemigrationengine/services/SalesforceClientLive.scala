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

  private val salesforceApiPathPrefixToVersion = "services/data/v60.0"

  private def requestAsMessage(request: HttpRequest) =
    s"${request.method} ${request.url}"

  private def failureMessage(request: HttpRequest, response: HttpResponse[String]) =
    requestAsMessage(request) + s" returned status ${response.code} with body:${response.body}"

  private def sendRequest(request: HttpRequest) =
    for {
      response <- ZIO
        .attempt(request.option(connTimeout).option(readTimeout).asString)
        .mapError(ex => SalesforceClientFailure(s"Request for ${requestAsMessage(request)} failed: $ex"))
      valid200Response <-
        if ((response.code / 100) == 2) { ZIO.succeed(response) }
        else { ZIO.fail(SalesforceClientFailure(failureMessage(request, response))) }
    } yield valid200Response

  private def sendRequestAndParseResponse[A](request: HttpRequest)(implicit reader: Reader[A]) =
    for {
      valid200Response <- sendRequest(request)
      parsedResponse <- ZIO
        .attempt(read[A](valid200Response.body))
        .mapError(ex => SalesforceClientFailure(s"${requestAsMessage(request)} failed to deserialise: $ex"))
    } yield parsedResponse

  val impl: ZLayer[SalesforceConfig with Logging, SalesforceClientFailure, SalesforceClient] =
    ZLayer.fromZIO {

      def auth(config: SalesforceConfig, logging: Logging) = {
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
        )
      } <* logging.info(s"Authenticated with salesforce using user:${config.userName} and client: ${config.clientId}")

      for {
        config <- ZIO.service[SalesforceConfig]
        logging <- ZIO.service[Logging]
        auth <- auth(config, logging)
      } yield new services.SalesforceClient {

        override def getSubscriptionByName(
            subscriptionName: String
        ): IO[SalesforceClientFailure, SalesforceSubscription] =
          sendRequestAndParseResponse[SalesforceSubscription](
            Http(
              s"${auth.instance_url}/${salesforceApiPathPrefixToVersion}/sobjects/SF_Subscription__c/Name/$subscriptionName"
            )
              .header("Authorization", s"Bearer ${auth.access_token}")
              .method("GET")
          ).tap(subscription => logging.info(s"Successfully loaded: ${subscription.Name}"))

        override def getContact(contactId: String): IO[SalesforceClientFailure, SalesforceContact] =
          sendRequestAndParseResponse[SalesforceContact](
            Http(s"${auth.instance_url}/${salesforceApiPathPrefixToVersion}/sobjects/Contact/$contactId")
              .header("Authorization", s"Bearer ${auth.access_token}")
              .method("GET")
          ).tap(contact => logging.info(s"Successfully loaded contact: ${contact.Id}"))

        override def createPriceRise(
            priceRise: SalesforcePriceRise
        ): IO[SalesforceClientFailure, SalesforcePriceRiseCreationResponse] =
          sendRequestAndParseResponse[SalesforcePriceRiseCreationResponse](
            Http(s"${auth.instance_url}/${salesforceApiPathPrefixToVersion}/sobjects/Price_Rise__c/")
              .postData(serialisePriceRise(priceRise))
              .header("Authorization", s"Bearer ${auth.access_token}")
              .header("Content-Type", "application/json")
          ).tap(priceRiseId => logging.info(s"Successfully created Price_Rise__c object: ${priceRiseId.id}"))

        override def updatePriceRise(
            priceRiseId: String,
            priceRise: SalesforcePriceRise
        ): IO[SalesforceClientFailure, Unit] = {
          sendRequest(
            Http(s"${auth.instance_url}/${salesforceApiPathPrefixToVersion}/sobjects/Price_Rise__c/$priceRiseId")
              .postData(serialisePriceRise(priceRise))
              .method("PATCH")
              .header("Authorization", s"Bearer ${auth.access_token}")
              .header("Content-Type", "application/json")
          ).unit
        } <* logging.info(s"Successfully updated Price_Rise__c object, priceRiseId: ${priceRiseId}")

        override def getPriceRise(priceRiseId: String): IO[SalesforceClientFailure, SalesforcePriceRise] =
          sendRequestAndParseResponse[SalesforcePriceRise](
            Http(s"${auth.instance_url}/${salesforceApiPathPrefixToVersion}/sobjects/Price_Rise__c/${priceRiseId}")
              .header("Authorization", s"Bearer ${auth.access_token}")
              .method("GET")
          ).tap(priceRise =>
            logging.info(
              s"Successfully retrieved Salesforce price rise object, priceRiseId: ${priceRiseId}, priceRise: ${priceRise}"
            )
          )

      }
    }

  private[pricemigrationengine] def serialisePriceRise(priceRise: SalesforcePriceRise) =
    write(priceRise, indent = 2)
}
