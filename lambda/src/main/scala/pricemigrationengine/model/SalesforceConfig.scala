package pricemigrationengine.model

case class SalesforceConfig(
    authUrl: String,
    clientId: String,
    clientSecret: String,
    userName: String,
    password: String,
    token: String
)
