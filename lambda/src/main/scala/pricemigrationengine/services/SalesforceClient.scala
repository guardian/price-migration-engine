package pricemigrationengine.services

import pricemigrationengine.model.{CohortUpdateFailure, EstimationResult, SalesforceClientFailure, SalesforcePriceRise, SalesforcePriceRiseCreationResult, SalesforceSubscription}
import zio.{IO, ZIO}

object SalesforceClient {
  trait Service {
    def getSubscriptionByName(subscrptionName: String): IO[SalesforceClientFailure, SalesforceSubscription]
    def createPriceRise(priceRise: SalesforcePriceRise): IO[SalesforceClientFailure, SalesforcePriceRiseCreationResult]
  }

  def getSubscriptionByName(
    subscrptionName: String
  ): ZIO[SalesforceClient, SalesforceClientFailure, SalesforceSubscription] =
    ZIO.accessM(_.get.getSubscriptionByName(subscrptionName))

  def createPriceRise(
    priceRise: SalesforcePriceRise
  ): ZIO[SalesforceClient, SalesforceClientFailure, SalesforcePriceRiseCreationResult] =
    ZIO.accessM(_.get.createPriceRise(priceRise))
}