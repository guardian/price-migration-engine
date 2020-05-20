package pricemigrationengine.services

import pricemigrationengine.model.{ConfigurationFailure, SalesforceClientFailure, SalesforceConfig, ZuoraFetchFailure, ZuoraProductCatalogue}
import scalaj.http.{Http, HttpRequest, HttpResponse}
import upickle.default._
import zio.{IO, ZLayer}

object SalesforceClientLive {
  private case class SalesforceAuthDetails(access_token: String, instance_url: String)

  val impl: ZLayer[SalesforceConfiguration with Logging, SalesforceClientFailure, SalesforceClient] =
    ZLayer.fromFunctionM { dependencies: SalesforceConfiguration with Logging =>
    val logging  = dependencies.get[Logging.Service]
    val salesforceConfig  = dependencies.get[SalesforceConfiguration.Service]

    implicit val rw: ReadWriter[SalesforceAuthDetails] = macroRW

    def requestAsMessage(request: HttpRequest) = {
      s"Request for ${request.method} ${request.url}"
    }

    def failureMessage(request: HttpRequest, response: HttpResponse[String]) = {
      requestAsMessage(request) + " returned status ${response.code} with body:${response.body}"
    }

    def auth(config: SalesforceConfig): IO[SalesforceClientFailure, SalesforceAuthDetails] = {
      val request = Http(s"${config.authUrl}/services/oauth2/token")
        .postForm(
          Seq(
            "grant_type" -> "password",
            "client_id" -> config.clientId,
            "client_secret" -> config.clientSecret,
            "username" -> config.userName,
            "password" -> s"${config.password}${config.token}",
          )
        )
      for {
        response <- IO.effect(request.asString)
            .mapError(ex => SalesforceClientFailure(s"${requestAsMessage(request)} failed: $ex"))
        valid200Response <- if(response.code == 200) {
          IO.succeed(response)
        } else {
          IO.fail(SalesforceClientFailure(failureMessage(request, response)))
        }

        parsedResponse <- IO
          .effect(read[SalesforceAuthDetails](valid200Response.body))
          .mapError(ex => SalesforceClientFailure(s"Failed to deserialise salesforce auth response: $ex"))
          .tap { _ =>
            logging.info(s"Authenticated with salesforce using user:${config.userName} and client: ${config.clientId}")
          }
      } yield parsedResponse
    }

    for {
      config <-
        salesforceConfig
          .config
          .mapError { error => SalesforceClientFailure(s"Failed to load salesforce config: ${error.reason}") }
      _ <- auth(config)
    } yield new SalesforceClient.Service {

    }
  }
}
