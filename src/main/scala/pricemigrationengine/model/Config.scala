package pricemigrationengine.model

case class Config(zuora: ZuoraConfig)

case class ZuoraConfig(baseUrl: String, clientId: String, clientSecret: String)
