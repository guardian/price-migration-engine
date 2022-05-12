package pricemigrationengine.services

import pricemigrationengine.model.{
  SalesforceClientFailure,
  SalesforceContact,
  SalesforcePriceRise,
  SalesforceSubscription
}
import zio.{IO, ZIO}

case class SalesforcePriceRiseCreationResponse(id: String)

trait SalesforceClient {
  def getSubscriptionByName(subscrptionName: String): IO[SalesforceClientFailure, SalesforceSubscription]
  def getContact(contactId: String): IO[SalesforceClientFailure, SalesforceContact]
  def createPriceRise(
      priceRise: SalesforcePriceRise
  ): IO[SalesforceClientFailure, SalesforcePriceRiseCreationResponse]
  def updatePriceRise(priceRiseId: String, priceRise: SalesforcePriceRise): IO[SalesforceClientFailure, Unit]
}

object SalesforceClient {

  def getSubscriptionByName(
      subscrptionName: String
  ): ZIO[SalesforceClient, SalesforceClientFailure, SalesforceSubscription] =
    ZIO.environmentWithZIO(_.get.getSubscriptionByName(subscrptionName))

  def getContact(
      contactId: String
  ): ZIO[SalesforceClient, SalesforceClientFailure, SalesforceContact] =
    ZIO.environmentWithZIO(_.get.getContact(contactId))

  def createPriceRise(
      priceRise: SalesforcePriceRise
  ): ZIO[SalesforceClient, SalesforceClientFailure, SalesforcePriceRiseCreationResponse] =
    ZIO.environmentWithZIO(_.get.createPriceRise(priceRise))

  def updatePriceRise(
      priceRiseId: String,
      priceRise: SalesforcePriceRise
  ): ZIO[SalesforceClient, SalesforceClientFailure, Unit] =
    ZIO.environmentWithZIO(_.get.updatePriceRise(priceRiseId, priceRise))
}
