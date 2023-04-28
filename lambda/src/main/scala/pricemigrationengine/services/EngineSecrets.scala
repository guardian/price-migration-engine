package pricemigrationengine.services

import pricemigrationengine.model._
import zio.{Layer, ZIO, ZLayer}
import software.amazon.awssdk.regions
import software.amazon.awssdk.services.secretsmanager._
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import upickle.default._

import java.lang.System.getenv

case class EngineSecrets(
    zuoraApiHost: String,
    zuoraClientId: String,
    zuoraClientSecret: String,
    salesforceClientId: String,
    salesforceClientSecret: String,
    salesforceUserName: String,
    salesforcePassword: String,
    salesforceToken: String,
    salesforceAuthUrl: String
)

object EngineSecrets {

  implicit val reader: Reader[EngineSecrets] = macroRW

  private lazy val region: regions.Region = regions.Region.EU_WEST_1

  private lazy val secretsClient = SecretsManagerClient.create()

  private lazy val secretId: String = s"price-migration-engine-lambda-${getenv("stage")}"

  def getSecretString: ZIO[Any, ConfigFailure, String] = {
    ZIO
      .attempt(
        secretsClient.getSecretValue(GetSecretValueRequest.builder().secretId(secretId).build()).secretString()
      )
      .mapError { ex =>
        ConfigFailure(s"Failure to retrieve secrets string: ${ex.getMessage}")
      }
  }

  def getSecrets: ZIO[Any, ConfigFailure, EngineSecrets] = {
    for {
      secretJsonString <- getSecretString
      secrets <- ZIO
        .attempt(read[EngineSecrets](secretJsonString))
        .mapError { ex =>
          ConfigFailure(s"Failure to parse secrets string: ${ex.getMessage}")
        }
    } yield secrets
  }
}
