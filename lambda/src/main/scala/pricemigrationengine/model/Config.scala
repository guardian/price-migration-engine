package pricemigrationengine.model

import java.time.LocalDate

case class Config(
    // earliest date that price migration can take place
    earliestStartDate: LocalDate,
)

case class ZuoraConfig(apiHost: String, clientId: String, clientSecret: String, yearInFuture: LocalDate = LocalDate.now.plusYears(1))

case class DynamoDBConfig(endpoint: Option[DynamoDBEndpointConfig])

case class DynamoDBEndpointConfig(serviceEndpoint: String, signingRegion: String)

case class CohortTableConfig(stage: String, batchSize: Int = 100)