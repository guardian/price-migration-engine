package pricemigrationengine.services

import pricemigrationengine.model.{SalesforceFailure, SalesforceContact, SalesforcePriceRise, SalesforceSubscription}
import zio.{IO, ZIO}

case class SalesforcePriceRiseCreationResponse(id: String)

trait Salesforce {
  def getSubscriptionByName(subscrptionName: String): IO[SalesforceFailure, SalesforceSubscription]
  def getContact(contactId: String): IO[SalesforceFailure, SalesforceContact]
  def createPriceRise(
      priceRise: SalesforcePriceRise
  ): IO[SalesforceFailure, SalesforcePriceRiseCreationResponse]
  def updatePriceRise(priceRiseId: String, priceRise: SalesforcePriceRise): IO[SalesforceFailure, Unit]
  def getPriceRise(priceRiseId: String): IO[SalesforceFailure, SalesforcePriceRise]
}

object Salesforce {

  def getSubscriptionByName(
      subscrptionName: String
  ): ZIO[Salesforce, SalesforceFailure, SalesforceSubscription] =
    ZIO.environmentWithZIO(_.get.getSubscriptionByName(subscrptionName))

  def getContact(
      contactId: String
  ): ZIO[Salesforce, SalesforceFailure, SalesforceContact] =
    ZIO.environmentWithZIO(_.get.getContact(contactId))

  def createPriceRise(
      priceRise: SalesforcePriceRise
  ): ZIO[Salesforce, SalesforceFailure, SalesforcePriceRiseCreationResponse] =
    ZIO.environmentWithZIO(_.get.createPriceRise(priceRise))

  def updatePriceRise(
      priceRiseId: String,
      priceRise: SalesforcePriceRise
  ): ZIO[Salesforce, SalesforceFailure, Unit] =
    ZIO.environmentWithZIO(_.get.updatePriceRise(priceRiseId, priceRise))

  def getPriceRise(
      priceRiseId: String
  ): ZIO[Salesforce, SalesforceFailure, SalesforcePriceRise] =
    ZIO.environmentWithZIO(_.get.getPriceRise(priceRiseId))
}
