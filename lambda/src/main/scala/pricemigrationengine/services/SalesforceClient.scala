package pricemigrationengine.services

import pricemigrationengine.model.{SalesforceClientFailure, SalesforceSubscription}
import zio.IO

object SalesforceClient {
  trait Service {
    def getSubscriptionByName(subscrptionName: String): IO[SalesforceClientFailure, SalesforceSubscription]
  }
}