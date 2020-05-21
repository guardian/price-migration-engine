package pricemigrationengine.services

import java.time.LocalDate

import pricemigrationengine.model.{SalesforceClientFailure, SalesforceConfig, SalesforcePriceRise, SalesforcePriceRiseCreationDetails, SalesforceSubscription}
import scalaj.http.{Http, HttpRequest, HttpResponse}
import upickle.default._
import zio.{IO, ZIO, ZLayer}

object SalesforceClientLive {
  private case class SalesforceAuthDetails(access_token: String, instance_url: String)

  val impl: ZLayer[SalesforceConfiguration with Logging, SalesforceClientFailure, SalesforceClient] =
    ZLayer.fromFunctionM { dependencies: SalesforceConfiguration with Logging =>
    val logging  = dependencies.get[Logging.Service]
    val salesforceConfig  = dependencies.get[SalesforceConfiguration.Service]

    implicit val localDateRW: ReadWriter[LocalDate] = readwriter[String].bimap[LocalDate](_.toString, LocalDate.parse)
    implicit val salesforceAuthDetailsRW: ReadWriter[SalesforceAuthDetails] = macroRW
    implicit val salesforceSubscriptionRW: ReadWriter[SalesforceSubscription] = macroRW
    implicit val salesforcePriceRiseRW: ReadWriter[SalesforcePriceRise] = macroRW
    implicit val salesforcePriceIdRiseRW: ReadWriter[SalesforcePriceRiseCreationResponse] = macroRW

    def requestAsMessage(request: HttpRequest) = {
      s"${request.method} ${request.url}"
    }

    def failureMessage(request: HttpRequest, response: HttpResponse[String]) = {
      requestAsMessage(request) + s" returned status ${response.code} with body:${response.body}"
    }

    def sendRequest[A](request: HttpRequest)(implicit reader: Reader[A]): ZIO[Any, SalesforceClientFailure, A] =
      for {
        response <- IO.effect(request.asString)
          .mapError(ex => SalesforceClientFailure(s"Request for ${requestAsMessage(request)} failed: $ex"))
        valid200Response <- if (response.code == 200) {
          IO.succeed(response)
        } else {
          IO.fail(SalesforceClientFailure(failureMessage(request, response)))
        }
        parsedResponse <- IO
          .effect(read[A](valid200Response.body))
          .mapError(ex => SalesforceClientFailure(s"${requestAsMessage(request)} failed to deserialise: $ex"))
      } yield parsedResponse

    def auth(config: SalesforceConfig): IO[SalesforceClientFailure, SalesforceAuthDetails] = {
      sendRequest[SalesforceAuthDetails](
        Http(s"${config.authUrl}/services/oauth2/token")
          .postForm(
            Seq(
              "grant_type" -> "password",
              "client_id" -> config.clientId,
              "client_secret" -> config.clientSecret,
              "username" -> config.userName,
              "password" -> s"${config.password}${config.token}",
            )
          )
      ).tap { _ =>
        logging.info(s"Authenticated with salesforce using user:${config.userName} and client: ${config.clientId}")
      }
    }

    for {
      config <- salesforceConfig
        .config
        .mapError { error => SalesforceClientFailure(s"Failed to load salesforce config: ${error.reason}") }
      auth <- auth(config)
    } yield new SalesforceClient.Service {
      override def getSubscriptionByName(
        subscriptionName: String
      ): IO[SalesforceClientFailure, SalesforceSubscription] =
        sendRequest[SalesforceSubscription](
          Http(s"${auth.instance_url}/services/data/v43.0/sobjects/SF_Subscription__c/Name/${subscriptionName}")
            .header("Authorization", s"Bearer ${auth.access_token}")
            .method("GET")
        ).tap( subscription =>
          logging.info(s"Successfully loaded: ${subscription}")
        )

      override def createPriceRise(priceRise: SalesforcePriceRise): IO[SalesforceClientFailure, SalesforcePriceRiseCreationResponse] =
        sendRequest[SalesforcePriceRiseCreationResponse](
          Http(s"${auth.instance_url}/services/data/v43.0/sobjects/Price_Rise__c/")
            .postData(write(priceRise))
            .header("Authorization", s"Bearer ${auth.access_token}")
        ).tap( priceRiseId =>
          logging.info(s"Successfully created Price_Rise__c object: ${priceRiseId.id}")
        )
    }
  }
}
