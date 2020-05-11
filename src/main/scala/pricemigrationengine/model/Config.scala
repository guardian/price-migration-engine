package pricemigrationengine.model

import java.time.LocalDate

case class Config(
    zuora: ZuoraConfig,
    yearInFuture: LocalDate = LocalDate.now.plusYears(1)
)

case class ZuoraConfig(apiHost: String, clientId: String, clientSecret: String)
