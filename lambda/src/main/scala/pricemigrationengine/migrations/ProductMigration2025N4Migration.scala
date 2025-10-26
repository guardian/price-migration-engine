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

case class N4Charge(name: String, charge: BigDecimal, chargeId: String)
case class N4Target(
    productName: String,
    sourceRatePlanName: String,
    targetRatePlanName: String,
    targetRatePlanId: String,
    charges: List[N4Charge]
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

  // -----------------------------------------------------

  // Note that we have 15 entries instead of 16, because we are missing
  // Newspaper - National Delivery / Saturday, which didn't show up in the
  // pre migration checks.

  val n4TargetMapping: Map[String, N4Target] = Map(
    "2c92a0fd5e1dcf0d015e3cb39d0a7ddb" -> N4Target(
      productName = "Newspaper Delivery",
      sourceRatePlanName = "Saturday",
      targetRatePlanName = "Saturday+",
      targetRatePlanId = "2c92a0ff6205708e01622484bb2c4613",
      charges = List(
        N4Charge(name = "Saturday", charge = BigDecimal(11.52), chargeId = "2c92a0ff6205708e01622484bb68461d"),
        N4Charge(name = "Digital Pack", charge = BigDecimal(9.47), chargeId = "2c92a0ff6205708e01622484bb404615")
      )
    ),
    "2c92a0fd5614305c01561dc88f3275be" -> N4Target(
      productName = "Newspaper Delivery",
      sourceRatePlanName = "Weekend",
      targetRatePlanName = "Weekend+",
      targetRatePlanId = "2c92a0ff560d311b0156136b9f5c3968",
      charges = List(
        N4Charge(name = "Sunday", charge = BigDecimal(12.40), chargeId = "2c92a0ff560d311b0156136ba0523996"),
        N4Charge(name = "Saturday", charge = BigDecimal(12.40), chargeId = "2c92a0ff560d311b0156136ba11539ae"),
        N4Charge(name = "Digital Pack", charge = BigDecimal(10.19), chargeId = "2c92a0ff560d311b0156136b9fac3976")
      )
    ),
    "2c92a0fd560d13880156136b72e50f0c" -> N4Target(
      productName = "Newspaper Delivery",
      sourceRatePlanName = "Everyday",
      targetRatePlanName = "Everyday+",
      targetRatePlanId = "2c92a0fd560d132301560e43cf041a3c",
      charges = List(
        N4Charge(name = "Wednesday", charge = BigDecimal(9.65), chargeId = "2c92a0fc560d13390156136324931d21"),
        N4Charge(name = "Friday", charge = BigDecimal(9.65), chargeId = "2c92a0fd560d138801561364cad96af7"),
        N4Charge(name = "Thursday", charge = BigDecimal(9.65), chargeId = "2c92a0fe560d3104015613640f555223"),
        N4Charge(name = "Sunday", charge = BigDecimal(12.67), chargeId = "2c92a0fe560d31040156136626dd5d1b"),
        N4Charge(name = "Monday", charge = BigDecimal(9.65), chargeId = "2c92a0ff560d31190156134be59060f4"),
        N4Charge(name = "Tuesday", charge = BigDecimal(9.65), chargeId = "2c92a0ff560d311b015613623e050a63"),
        N4Charge(name = "Saturday", charge = BigDecimal(12.67), chargeId = "2c92a0ff560d311c0156136573e366f3"),
        N4Charge(name = "Digital Pack", charge = BigDecimal(10.40), chargeId = "2c92a0fd560d132901561367b2f17763")
      )
    ),
    "2c92a0ff560d311b0156136f2afe5315" ->
      N4Target(
        productName = "Newspaper Delivery",
        sourceRatePlanName = "Sixday",
        targetRatePlanName = "Sixday+",
        targetRatePlanId = "2c92a0ff560d311b0156136b697438a9",
        charges = List(
          N4Charge(name = "Wednesday", charge = BigDecimal(10.01), chargeId = "2c92a0ff560d311b0156136b698f38ac"),
          N4Charge(name = "Friday", charge = BigDecimal(10.01), chargeId = "2c92a0ff560d311b0156136b6a0838bc"),
          N4Charge(name = "Thursday", charge = BigDecimal(10.01), chargeId = "2c92a0ff560d311b0156136b6a4138c5"),
          N4Charge(name = "Monday", charge = BigDecimal(10.01), chargeId = "2c92a0ff560d311b0156136b6ac738d5"),
          N4Charge(name = "Tuesday", charge = BigDecimal(10.01), chargeId = "2c92a0ff560d311b0156136b6b0438dd"),
          N4Charge(name = "Saturday", charge = BigDecimal(13.14), chargeId = "2c92a0ff560d311b0156136b6b4b38e6"),
          N4Charge(name = "Digital Pack", charge = BigDecimal(10.80), chargeId = "2c92a0ff560d311b0156136b69d038b4")
        )
      ),
    "8a12999f8a268c57018a27ebe868150c" -> N4Target(
      productName = "Newspaper - National Delivery",
      sourceRatePlanName = "Weekend",
      targetRatePlanName = "Weekend+",
      targetRatePlanId = "8a1280be96d33dbf0196d487b55c1283",
      charges = List(
        N4Charge(name = "Digital Pack", charge = BigDecimal(10.19), chargeId = "8a12817596d33daf0196d48a3eec13ce"),
        N4Charge(name = "Sunday", charge = BigDecimal(12.40), chargeId = "8a1280be96d33dbf0196d487b5ae1285"),
        N4Charge(name = "Saturday", charge = BigDecimal(12.40), chargeId = "8a1280be96d33dbf0196d487b5f8128d")
      )
    ),
    "8a12999f8a268c57018a27ebfd721883" -> N4Target(
      productName = "Newspaper - National Delivery",
      sourceRatePlanName = "Sixday",
      targetRatePlanName = "Sixday+",
      targetRatePlanId = "8a12994696d3587b0196d484491e3beb",
      charges = List(
        N4Charge(name = "Digital Pack", charge = BigDecimal(10.80), chargeId = "8a12979796d358720196d4878ee0421f"),
        N4Charge(name = "Saturday", charge = BigDecimal(13.14), chargeId = "8a12994696d3587b0196d4844b5f3c15"),
        N4Charge(name = "Wednesday", charge = BigDecimal(10.01), chargeId = "8a12994696d3587b0196d48449893bed"),
        N4Charge(name = "Friday", charge = BigDecimal(10.01), chargeId = "8a12994696d3587b0196d48449f33bf5"),
        N4Charge(name = "Thursday", charge = BigDecimal(10.01), chargeId = "8a12994696d3587b0196d4844a473bfd"),
        N4Charge(name = "Monday", charge = BigDecimal(10.01), chargeId = "8a12994696d3587b0196d4844aa13c05"),
        N4Charge(name = "Tuesday", charge = BigDecimal(10.01), chargeId = "8a12994696d3587b0196d4844aff3c0d")
      )
    ),
    "8a12999f8a268c57018a27ebe31414a4" -> N4Target(
      productName = "Newspaper - National Delivery",
      sourceRatePlanName = "Everyday",
      targetRatePlanName = "Everyday+",
      targetRatePlanId = "8a1280be96d33dbf0196d48a632616f4",
      charges = List(
        N4Charge(name = "Digital Pack", charge = BigDecimal(10.40), chargeId = "8a12904196d3586d0196d48bff216382"),
        N4Charge(name = "Sunday", charge = BigDecimal(12.67), chargeId = "8a1280be96d33dbf0196d48a634b16f6"),
        N4Charge(name = "Wednesday", charge = BigDecimal(9.65), chargeId = "8a1280be96d33dbf0196d48a638d16fe"),
        N4Charge(name = "Friday", charge = BigDecimal(9.65), chargeId = "8a1280be96d33dbf0196d48a63cf1706"),
        N4Charge(name = "Thursday", charge = BigDecimal(9.65), chargeId = "8a1280be96d33dbf0196d48a6414170f"),
        N4Charge(name = "Monday", charge = BigDecimal(9.65), chargeId = "8a1280be96d33dbf0196d48a645c1718"),
        N4Charge(name = "Tuesday", charge = BigDecimal(9.65), chargeId = "8a1280be96d33dbf0196d48a64a21720"),
        N4Charge(name = "Saturday", charge = BigDecimal(12.67), chargeId = "8a1280be96d33dbf0196d48a64f21728")
      )
    ),
    "2c92a0ff56fe33f00157040f9a537f4b" -> N4Target(
      productName = "Newspaper Voucher",
      sourceRatePlanName = "Weekend",
      targetRatePlanName = "Weekend+",
      targetRatePlanId = "2c92a0fd56fe26b60157040cdd323f76",
      charges = List(
        N4Charge(name = "Saturday", charge = BigDecimal(9.92), chargeId = "2c92a0fd56fe26b601570432f4e33d17"),
        N4Charge(name = "Sunday", charge = BigDecimal(9.92), chargeId = "2c92a0ff56fe33f5015709b8fc4d5617"),
        N4Charge(name = "Digipack", charge = BigDecimal(8.15), chargeId = "2c92a0fe56fe33ff015709bb986636d8")
      )
    ),
    "2c92a0fd6205707201621f9f6d7e0116" -> N4Target(
      productName = "Newspaper Voucher",
      sourceRatePlanName = "Saturday",
      targetRatePlanName = "Saturday+",
      targetRatePlanId = "2c92a0fd6205707201621fa1350710e3",
      charges = List(
        N4Charge(name = "Saturday", charge = BigDecimal(8.78), chargeId = "2c92a0fd6205707201621fa1354710ed"),
        N4Charge(name = "Digipack", charge = BigDecimal(7.21), chargeId = "2c92a0fd6205707201621fa1351710e5")
      )
    ),
    "2c92a0fd56fe270b0157040e42e536ef" -> N4Target(
      productName = "Newspaper Voucher",
      sourceRatePlanName = "Sixday",
      targetRatePlanName = "Sixday+",
      targetRatePlanId = "2c92a0fc56fe26ba0157040c5ea17f6a",
      charges = List(
        N4Charge(name = "Thursday", charge = BigDecimal(8.39), chargeId = "2c92a0fc56fe26ba015709cf4bbd3d1c"),
        N4Charge(name = "Wednesday", charge = BigDecimal(8.39), chargeId = "2c92a0fd56fe26b6015709ced61a032e"),
        N4Charge(name = "Friday", charge = BigDecimal(8.39), chargeId = "2c92a0fd56fe26b6015709cfc1500a2e"),
        N4Charge(name = "Saturday", charge = BigDecimal(11.01), chargeId = "2c92a0fd56fe26b6015709d078df4a80"),
        N4Charge(name = "Monday", charge = BigDecimal(8.39), chargeId = "2c92a0fe56fe33ff015704325d87494c"),
        N4Charge(name = "Tuesday", charge = BigDecimal(8.39), chargeId = "2c92a0ff56fe33f5015709cdedbd246b"),
        N4Charge(name = "Digipack", charge = BigDecimal(9.03), chargeId = "2c92a0ff56fe33f3015709d10a436f52")
      )
    ),
    "2c92a0fd56fe270b0157040dd79b35da" -> N4Target(
      productName = "Newspaper Voucher",
      sourceRatePlanName = "Everyday",
      targetRatePlanName = "Everyday+",
      targetRatePlanId = "2c92a0ff56fe33f50157040bbdcf3ae4",
      charges = List(
        N4Charge(name = "Saturday", charge = BigDecimal(10.56), chargeId = "2c92a0fd56fe26b60157042fcd462666"),
        N4Charge(name = "Tuesday", charge = BigDecimal(8.04), chargeId = "2c92a0fd56fe26b6015709ca144a646a"),
        N4Charge(name = "Monday", charge = BigDecimal(8.04), chargeId = "2c92a0fd56fe270b015709c90c291c49"),
        N4Charge(name = "Thursday", charge = BigDecimal(8.04), chargeId = "2c92a0fd56fe270b015709cc16f92645"),
        N4Charge(name = "Wednesday", charge = BigDecimal(8.04), chargeId = "2c92a0ff56fe33f0015709cac4561bf3"),
        N4Charge(name = "Sunday", charge = BigDecimal(10.56), chargeId = "2c92a0ff56fe33f5015709c80af30495"),
        N4Charge(name = "Friday", charge = BigDecimal(8.04), chargeId = "2c92a0ff56fe33f5015709cce7ad1aea"),
        N4Charge(name = "Digipack", charge = BigDecimal(8.67), chargeId = "2c92a0fc56fe26ba01570418eddd26e1")
      )
    ),
    "2c92a00870ec598001710740cdd02fbd" -> N4Target(
      productName = "Newspaper Digital Voucher",
      sourceRatePlanName = "Saturday",
      targetRatePlanName = "Saturday+",
      targetRatePlanId = "2c92a00870ec598001710740ce702ff0",
      charges = List(
        N4Charge(name = "Saturday", charge = BigDecimal(8.78), chargeId = "2c92a00870ec598001710740cf1e2ffc"),
        N4Charge(name = "Digipack", charge = BigDecimal(7.21), chargeId = "2c92a00870ec598001710740cea02ff4")
      )
    ),
    "2c92a00870ec598001710740d24b3022" -> N4Target(
      productName = "Newspaper Digital Voucher",
      sourceRatePlanName = "Weekend",
      targetRatePlanName = "Weekend+",
      targetRatePlanId = "2c92a00870ec598001710740c6672ee7",
      charges = List(
        N4Charge(name = "Saturday", charge = BigDecimal(9.92), chargeId = "2c92a00870ec598001710740c6872ee9"),
        N4Charge(name = "Sunday", charge = BigDecimal(9.92), chargeId = "2c92a00870ec598001710740c7132efe"),
        N4Charge(name = "Digipack", charge = BigDecimal(8.15), chargeId = "2c92a00870ec598001710740c6ce2ef1")
      )
    ),
    "2c92a00870ec598001710740ca532f69" -> N4Target(
      productName = "Newspaper Digital Voucher",
      sourceRatePlanName = "Sixday",
      targetRatePlanName = "Sixday+",
      targetRatePlanId = "2c92a00870ec598001710740c4582ead",
      charges = List(
        N4Charge(name = "Thursday", charge = BigDecimal(8.39), chargeId = "2c92a00870ec598001710740c48e2eaf"),
        N4Charge(name = "Wednesday", charge = BigDecimal(8.39), chargeId = "2c92a00870ec598001710740c4dc2eb7"),
        N4Charge(name = "Friday", charge = BigDecimal(8.39), chargeId = "2c92a00870ec598001710740c5192ebf"),
        N4Charge(name = "Saturday", charge = BigDecimal(11.01), chargeId = "2c92a00870ec598001710740c55a2ec7"),
        N4Charge(name = "Monday", charge = BigDecimal(8.39), chargeId = "2c92a00870ec598001710740c5962ecf"),
        N4Charge(name = "Tuesday", charge = BigDecimal(8.39), chargeId = "2c92a00870ec598001710740c60f2edf"),
        N4Charge(name = "Digipack", charge = BigDecimal(9.03), chargeId = "2c92a00870ec598001710740c5cf2ed7")
      )
    ),
    "2c92a00870ec598001710740c78d2f13" -> N4Target(
      productName = "Newspaper Digital Voucher",
      sourceRatePlanName = "Everyday",
      targetRatePlanName = "Everyday+",
      targetRatePlanId = "2c92a00870ec598001710740d3d03035",
      charges = List(
        N4Charge(name = "Saturday", charge = BigDecimal(10.56), chargeId = "2c92a00870ec598001710740d4b8304f"),
        N4Charge(name = "Tuesday", charge = BigDecimal(8.04), chargeId = "2c92a00870ec598001710740d54f3069"),
        N4Charge(name = "Monday", charge = BigDecimal(8.04), chargeId = "2c92a00870ec598001710740d5fd3073"),
        N4Charge(name = "Thursday", charge = BigDecimal(8.04), chargeId = "2c92a00870ec598001710740d691307c"),
        N4Charge(name = "Wednesday", charge = BigDecimal(8.04), chargeId = "2c92a00870ec598001710740d7493084"),
        N4Charge(name = "Sunday", charge = BigDecimal(10.56), chargeId = "2c92a00870ec598001710740d7e2308d"),
        N4Charge(name = "Friday", charge = BigDecimal(8.04), chargeId = "2c92a00870ec598001710740d8873096"),
        N4Charge(name = "Digipack", charge = BigDecimal(8.67), chargeId = "2c92a00870ec598001710740d4143037")
      )
    )
  )

  def decideN4Target(sourceRatePlanId: String): Option[N4Target] = {
    n4TargetMapping.get(sourceRatePlanId)
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
