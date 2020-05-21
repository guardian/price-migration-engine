package pricemigrationengine.services

import pricemigrationengine.model.{CohortUpdateFailure, EstimationResult, SalesforceClientFailure, SalesforceSubscription}
import zio.{IO, ZIO}

object SalesforceClient {
  trait Service {
    def getSubscriptionByName(subscrptionName: String): IO[SalesforceClientFailure, SalesforceSubscription]
  }

  def getSubscriptionByName(
    subscrptionName: String
  ): ZIO[SalesforceClient, SalesforceClientFailure, SalesforceSubscription] =
    ZIO.accessM(_.get.getSubscriptionByName(subscrptionName))
}