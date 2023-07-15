package pricemigrationengine.model
import pricemigrationengine.model.CohortSpec
import pricemigrationengine.model.ZuoraProductCatalogue.{homeDeliveryRatePlans, productPricingMap}
import scala.math.BigDecimal.RoundingMode
import java.time.LocalDate

object SupporterRevenue2023V1V2 {

  val newPriceMapMonthlies: Map[Currency, BigDecimal] = Map(
    "GBP" -> BigDecimal(10),
    "AUD" -> BigDecimal(17),
    "CAD" -> BigDecimal(13),
    "EUR" -> BigDecimal(10),
    "USD" -> BigDecimal(13),
    "NZD" -> BigDecimal(17),
  )

  val newPriceMapAnnuals: Map[Currency, BigDecimal] = Map(
    "GBP" -> BigDecimal(95),
    "AUD" -> BigDecimal(160),
    "CAD" -> BigDecimal(120),
    "EUR" -> BigDecimal(95),
    "USD" -> BigDecimal(120),
    "NZD" -> BigDecimal(160),
  )

  def subscriptionRatePlan(subscription: ZuoraSubscription): Either[AmendmentDataFailure, ZuoraRatePlan] = {
    subscription.ratePlans.headOption match {
      case None =>
        Left(AmendmentDataFailure(s"Subscription ${subscription.subscriptionNumber} doesn't have any rate plan"))
      case Some(ratePlan) => Right(ratePlan)
    }
  }

  def subscriptionRatePlanCharge(
      subscription: ZuoraSubscription,
      ratePlan: ZuoraRatePlan
  ): Either[AmendmentDataFailure, ZuoraRatePlanCharge] = {
    ratePlan.ratePlanCharges.headOption match {
      case None => {
        // Although not enforced by the signature of the function, for this error message to make sense we expect that
        // the rate plan belongs to the currency
        Left(
          AmendmentDataFailure(s"Subscription ${subscription.subscriptionNumber} has a rate plan, but with no charge")
        )
      }
      case Some(ratePlanCharge) => Right(ratePlanCharge)
    }
  }

  def getOldPrice(
      subscription: ZuoraSubscription,
      ratePlanCharge: ZuoraRatePlanCharge
  ): Either[AmendmentDataFailure, BigDecimal] = {
    ratePlanCharge.price match {
      case None => {
        // Although not enforced by the signature of the function, for this error message to make sense we expect that
        // the rate plan charge belongs to the currency
        Left(
          AmendmentDataFailure(
            s"Subscription ${subscription.subscriptionNumber} has a rate plan charge, but with no currency"
          )
        )
      }
      case Some(price) => Right(price)
    }
  }

  def currencyToNewPriceMonthlies(currency: String): Either[AmendmentDataFailure, BigDecimal] = {
    newPriceMapMonthlies.get(currency) match {
      case None => Left(AmendmentDataFailure(s"Could not determine a new monthly price for currency: ${currency}"))
      case Some(price) => Right(price)
    }
  }

  def currencyToNewPriceAnnuals(currency: String): Either[AmendmentDataFailure, BigDecimal] = {
    newPriceMapAnnuals.get(currency) match {
      case None => Left(AmendmentDataFailure(s"Could not determine a new annual price for currency: ${currency}"))
      case Some(price) => Right(price)
    }
  }

  def currencyToNewPrice(bilingP: String, currency: String): Either[AmendmentDataFailure, BigDecimal] = {
    if (bilingP == "Month") {
      currencyToNewPriceMonthlies(currency: String)
    } else {
      currencyToNewPriceAnnuals(currency: String)
    }
  }

  case class RatePlanChargePair(
      chargeFromSubscription: ZuoraRatePlanCharge,
      chargeFromProduct: ZuoraProductRatePlanCharge
  )

  def hasNotPriceAndDiscount(ratePlanCharge: ZuoraRatePlanCharge): Boolean =
    ratePlanCharge.price.isDefined ^ ratePlanCharge.discountPercentage.exists(_ > 0)

  def ratePlanCharge(
      subscription: ZuoraSubscription,
      invoiceItem: ZuoraInvoiceItem
  ): Either[AmendmentDataFailure, ZuoraRatePlanCharge] =
    ZuoraRatePlanCharge
      .matchingRatePlanCharge(subscription, invoiceItem)
      .filterOrElse(
        hasNotPriceAndDiscount,
        AmendmentDataFailure(s"Rate plan charge '${invoiceItem.chargeNumber}' has price and discount")
      )

  def ratePlanChargesOrFail(
      subscription: ZuoraSubscription,
      invoiceItems: Seq[ZuoraInvoiceItem]
  ): Either[AmendmentDataFailure, Seq[ZuoraRatePlanCharge]] = {
    val ratePlanCharges = invoiceItems.map(item => ratePlanCharge(subscription, item))
    val failures = ratePlanCharges.collect { case Left(failure) => failure }

    if (failures.isEmpty) Right(ratePlanCharges.collect { case Right(charge) => charge })
    else Left(AmendmentDataFailure(failures.map(_.reason).mkString(", ")))
  }

  def ratePlanChargePair(
      catalogue: ZuoraProductCatalogue,
      ratePlanCharge: ZuoraRatePlanCharge
  ): Either[ZuoraProductRatePlanChargeId, RatePlanChargePair] = {
    productPricingMap(catalogue)
      .get(ratePlanCharge.productRatePlanChargeId)
      .toRight(ratePlanCharge.productRatePlanChargeId)
      .map(productRatePlanCharge => RatePlanChargePair(ratePlanCharge, productRatePlanCharge))
  }

  def ratePlanChargePairs(
      catalogue: ZuoraProductCatalogue,
      ratePlanCharges: Seq[ZuoraRatePlanCharge]
  ): Either[AmendmentDataFailure, Seq[RatePlanChargePair]] = {
    /*
     * distinct because where a sub has a discount rate plan,
     * the same discount will appear against each product rate plan charge in the invoice preview.
     */
    val pairs = ratePlanCharges.distinctBy(_.productRatePlanChargeId).map(rp => ratePlanChargePair(catalogue, rp))
    val failures = pairs.collect { case Left(failure) => failure }
    if (failures.isEmpty) Right(pairs.collect { case Right(pricing) => pricing })
    else
      Left(
        AmendmentDataFailure(
          s"Failed to find matching product rate plan charges for rate plan charges: ${failures.mkString(", ")}"
        )
      )
  }

  def billingPeriod(
      account: ZuoraAccount,
      catalogue: ZuoraProductCatalogue,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      nextServiceStartDate: LocalDate,
  ): Either[AmendmentDataFailure, String] = {
    val invoiceItems = ZuoraInvoiceItem.items(invoiceList, subscription, nextServiceStartDate)
    for {
      ratePlanCharges <- ratePlanChargesOrFail(subscription, invoiceItems)
      billingPeriod <- ratePlanCharges
        .map(rpc => rpc.billingPeriod)
        .filter(period => period.isDefined)
        .map(rpc => rpc.get)
        .headOption
        .toRight(AmendmentDataFailure("Unknown billing period"))
    } yield billingPeriod
  }

  def priceData(
      account: ZuoraAccount,
      catalogue: ZuoraProductCatalogue,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      nextServiceDate: LocalDate,
      cohortSpec: CohortSpec
  ): Either[AmendmentDataFailure, PriceData] = {
    for {
      ratePlan <- subscriptionRatePlan(subscription)
      ratePlanCharge <- subscriptionRatePlanCharge(subscription, ratePlan)
      currency = ratePlanCharge.currency
      oldPrice <- getOldPrice(subscription, ratePlanCharge)
      billingP <- billingPeriod(account, catalogue, subscription, invoiceList, nextServiceDate)
      newPrice <- currencyToNewPrice(billingP, currency: String)
    } yield PriceData(currency, oldPrice, newPrice, billingP)
  }
}
