package pricemigrationengine.migrations
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._
import pricemigrationengine.services.Zuora

import java.time.LocalDate
import ujson._
import upickle.default._
import zio.ZIO

case class ProductMigration2025N4NotificationData(
    brandTitle: String,
    formstackUrl: String
)

object ProductMigration2025N4Migration {

  val maxLeadTime = 1000
  val minLeadTime = 0

  def decideFormstackUrl(salesforcePriceRiseId: String): String = {
    s"https://guardiannewsandmedia.formstack.com/forms/print_migration_25?subscription_reference=${salesforcePriceRiseId}"
  }

  def getNotificationData(
      cohortSpec: CohortSpec,
      item: CohortItem
  ): Option[ProductMigration2025N4NotificationData] = {
    MigrationType(cohortSpec) match {
      case ProductMigration2025N4 => {
        for {
          brandTitle <- item.ex_2025N4_label
          salesforcePriceRiseId <- item.salesforcePriceRiseId
          formstackUrl = decideFormstackUrl(salesforcePriceRiseId)
        } yield ProductMigration2025N4NotificationData(
          brandTitle,
          formstackUrl
        )
      }
      case _ =>
        Some(
          // For Tom reading this... Same as usual, I can't return a None
          // and the case class cannot have its fields defined as Options :)
          // #EmptyString ☺️
          ProductMigration2025N4NotificationData(
            "",
            ""
          )
        )
    }
  }

  def brazeName(cohortItem: CohortItem): Option[String] = {
    /*
      Canvas1: Newspaper+
      Canvas ID: af95cae0-0d3a-46c4-90da-193764ecc87d
      Canvas name:  SV_NP_DigitalMigrationNewspaperPlus_2025

      Canvas2: Newspaper only
      Canvas ID: dac04631-af52-46a3-94e1-082c2b38908d
      Canvas name: SV_NP_DigitalMigrationNewspaperOnly_2025

      Canvas3: Digi Subs
      Canvas ID: 7c8445fe-e36a-4e50-b3d4-5090b1c3e314
      Canvas name:
          (old) SV_NP_DigitalMigrationDigitalSubs_2025
          (new) SV_NP_DigitalMigrationNewspaperOnlyNoOptOut_2025
     */
    for {
      canvas <- cohortItem.ex_2025N4_canvas
    } yield {
      canvas match {
        case "canvas1" => "SV_NP_DigitalMigrationNewspaperPlus_2025"
        case "canvas2" => "SV_NP_DigitalMigrationNewspaperOnly_2025"
        case "canvas3" => "SV_NP_DigitalMigrationNewspaperOnlyNoOptOut_2025"
        case _         => throw new Exception("unexpected ProductMigration2025N4 cohort item canvas name")
      }
    }
  }

  // -----------------------------------------------------

  def priceData(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
  ): Either[DataExtractionFailure, PriceData] = {
    val priceDataOpt: Option[PriceData] = for {
      ratePlan <- SI2025RateplanFromSubAndInvoices.determineRatePlan(subscription, invoiceList)
      currency <- SI2025Extractions.determineCurrency(ratePlan)
      oldPrice = SI2025Extractions.determineOldPrice(ratePlan)
      billingPeriod <- SI2025Extractions.determineBillingPeriod(ratePlan)
      newPrice = oldPrice
    } yield PriceData(currency, oldPrice, newPrice, BillingPeriod.toString(billingPeriod))
    priceDataOpt match {
      case Some(pricedata) => Right(pricedata)
      case None =>
        Left(
          DataExtractionFailure(
            s"[9ac10338] Could not determine PriceData for subscription ${subscription.subscriptionNumber}"
          )
        )
    }
  }

  def amendmentOrderPayload(
      cohortItem: CohortItem,
      orderDate: LocalDate,
      accountNumber: String,
      subscriptionNumber: String,
      effectDate: LocalDate,
      zuora_subscription: ZuoraSubscription,
      oldPrice: BigDecimal,
      commsPrice: BigDecimal,
      invoiceList: ZuoraInvoiceList,
  ): Either[Failure, Value] = ???
}
