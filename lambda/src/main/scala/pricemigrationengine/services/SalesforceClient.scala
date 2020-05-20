package pricemigrationengine.services

import pricemigrationengine.model.{SalesforceClientError, SalesforceSubscription}
import zio.IO

object SalesforceClient {
  trait Service {
    def getSubscriptionByName(subscrptionName: String): IO[SalesforceClientError, SalesforceSubscription]
  }
}