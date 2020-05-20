package pricemigrationengine

import pricemigrationengine.model.{ZuoraInvoiceList, ZuoraProductCatalogue, ZuoraSubscription}
import upickle.default._

import scala.io.Source

object Fixtures {

  private def instanceFromJson[A: Reader](resource: String): A = {
    val json = Source.fromResource(resource).mkString
    read[A](json)
  }

  def productCatalogueFromJson(resource: String): ZuoraProductCatalogue =
    instanceFromJson[ZuoraProductCatalogue](resource)

  def subscriptionFromJson(resource: String): ZuoraSubscription =
    instanceFromJson[ZuoraSubscription](resource)

  def invoiceListFromJson(resource: String): ZuoraInvoiceList =
    instanceFromJson[ZuoraInvoiceList](resource)
}
