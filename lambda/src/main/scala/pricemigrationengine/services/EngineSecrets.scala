package pricemigrationengine.services

import pricemigrationengine.model._
import zio.{Layer, ZIO, ZLayer}
import software.amazon.awssdk.regions
import software.amazon.awssdk.services.secretsmanager._
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import upickle.default._

import java.lang.System.getenv

sealed case class EngineSecrets(
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

  private lazy val secretsJsonString =
    secretsClient.getSecretValue(GetSecretValueRequest.builder().secretId(secretId).build()).secretString()

  private lazy val secrets: EngineSecrets = read[EngineSecrets](secretsJsonString)

  def getSecrets: ZIO[Any, ConfigFailure, EngineSecrets] = {
    ZIO.succeed(secrets)
  }
}
