package pricemigrationengine.services

import pricemigrationengine.model._
import zio.{Layer, ZIO, ZLayer}
import zio._
import software.amazon.awssdk.regions
import software.amazon.awssdk.services.secretsmanager._
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import upickle.default._

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

  private def getSecretId: ZIO[Any, ConfigFailure, String] =
    for {
      stageOpt <- System.env("stage").mapError { ex => ConfigFailure(s"Failure to retrieve stage: ${ex.getMessage}") }
      stage <- ZIO.fromOption(stageOpt).mapError { _ => ConfigFailure(s"Failure to retrieve stage") }
    } yield s"price-migration-engine-lambda-${stage}"

  private def getSecretString: ZIO[Any, ConfigFailure, String] = for {
    secretId <- getSecretId
    secret <- ZIO
      .attempt(
        secretsClient.getSecretValue(GetSecretValueRequest.builder().secretId(secretId).build()).secretString()
      )
      .mapError { ex =>
        ConfigFailure(s"Failure to retrieve secrets string: ${ex.getMessage}")
      }
  } yield secret

  def getSecrets: ZIO[Any, ConfigFailure, EngineSecrets] = for {
    secretJsonString <- getSecretString
    secrets <- ZIO
      .attempt(read[EngineSecrets](secretJsonString))
      .mapError { ex =>
        ConfigFailure(s"Failure to parse secrets string: ${ex.getMessage}")
      }
  } yield secrets
}
