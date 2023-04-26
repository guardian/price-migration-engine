package pricemigrationengine.services

import pricemigrationengine.model._
import zio.{Layer, ZIO, ZLayer}

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider
import software.amazon.awssdk.services.secretsmanager._
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

import play.api.libs.json.Json

import java.lang.System.getenv

case class PriceMigrationEngineSecrets(
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

object PriceMigrationEngineSecrets {
  implicit val secretsRead = Json.reads[PriceMigrationEngineSecrets]
}

object SecretManagerClient {

  lazy val region: regions.Region = regions.Region.EU_WEST_1

  lazy val secretsClient = SecretsManagerClient
    .builder()
    .build()

  lazy val secretId: String = "price-migration-engine-lambda-CODE"

  lazy val secretsJsonString =
    secretsClient.getSecretValue(GetSecretValueRequest.builder().secretId(secretId).build()).secretString()

  lazy val secrets: PriceMigrationEngineSecrets = Json.parse(secretsJsonString).as[PriceMigrationEngineSecrets]
}
