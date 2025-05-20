package pricemigrationengine.model

import pricemigrationengine.migrations.{GW2024Migration, SupporterPlus2024Migration}
import pricemigrationengine.model.ZuoraProductCatalogue.productPricingMap

import java.time.LocalDate
import scala.math.BigDecimal.RoundingMode

case class AmendmentData(startDate: LocalDate, priceData: PriceData)

object AmendmentData {

  case class RatePlanChargePair(
      chargeFromSubscription: ZuoraRatePlanCharge,
      chargeFromProduct: ZuoraProductRatePlanCharge
  )

  def nextServiceStartDate(
      invoiceList: ZuoraInvoiceList,
      subscription: ZuoraSubscription,
      onOrAfter: LocalDate
  ): Either[Failure, LocalDate] =
    ZuoraInvoiceItem
      .itemsForSubscription(invoiceList, subscription)
      .map(_.serviceStartDate)
      .sortBy(_.toEpochDay)
      .dropWhile(_.isBefore(onOrAfter))
      .headOption
      .toRight(DataExtractionFailure(s"Cannot determine next billing date on or after $onOrAfter from $invoiceList"))

  def hasNotPriceAndDiscount(ratePlanCharge: ZuoraRatePlanCharge): Boolean =
    ratePlanCharge.price.isDefined ^ ratePlanCharge.discountPercentage.exists(_ > 0)

  def ratePlanCharge(
      subscription: ZuoraSubscription,
      invoiceItem: ZuoraInvoiceItem
  ): Either[Failure, ZuoraRatePlanCharge] =
    ZuoraRatePlanCharge
      .matchingRatePlanCharge(subscription, invoiceItem)
      .filterOrElse(
        hasNotPriceAndDiscount,
        DataExtractionFailure(s"Rate plan charge '${invoiceItem.chargeNumber}' has price and discount")
      )

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
  ): Either[DataExtractionFailure, Seq[RatePlanChargePair]] = {
    /*
     * distinct because where a sub has a discount rate plan,
     * the same discount will appear against each product rate plan charge in the invoice preview.
     */
    val pairs = ratePlanCharges.distinctBy(_.productRatePlanChargeId).map(rp => ratePlanChargePair(catalogue, rp))
    val failures = pairs.collect { case Left(failure) => failure }
    if (failures.isEmpty) Right(pairs.collect { case Right(pricing) => pricing })
    else
      Left(
        DataExtractionFailure(
          s"[AmendmentData] Failed to find matching product rate plan charges for rate plan charges: ${failures.mkString(", ")}"
        )
      )
  }

  /** Total charge amount, including taxes and discounts, for the service period starting on the given service start
    * date.
    */
  def totalChargeAmount(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      serviceStartDate: LocalDate
  ): Either[DataExtractionFailure, BigDecimal] = {
    /*
     * As charge amounts on Zuora invoice previews don't include tax,
     * we have to find the price of the rate plan charges on the subscription
     * that correspond with items in the invoice list.
     */
    val amounts = for {
      invoiceItem <- ZuoraInvoiceItem.items(invoiceList, subscription, serviceStartDate)
      ratePlanCharge <- ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toSeq
    } yield individualChargeAmount(ratePlanCharge)

    val discounts = amounts.collect { case Left(percentageDiscount) => percentageDiscount }

    if (discounts.length > 1) Left(DataExtractionFailure(s"Multiple discounts applied: ${discounts.mkString(", ")}"))
    else {
      val newPrice = applyDiscountAndThenSum(
        discountPercentage = discounts.headOption,
        beforeDiscount = amounts.collect { case Right(amount) => amount }
      )
      Right(newPrice)
    }
  }

  /** Either a left discount percentage or a right absolute amount.
    */
  def individualChargeAmount(ratePlanCharge: ZuoraRatePlanCharge): Either[Double, BigDecimal] =
    ratePlanCharge.price match {
      case None             => ratePlanCharge.discountPercentage.toLeft(0)
      case Some(p) if p > 0 => Right(p)
      case Some(_)          => Right(0)
    }

  private def applyDiscountAndThenSum(discountPercentage: Option[Double], beforeDiscount: Seq[BigDecimal]): BigDecimal =
    beforeDiscount.map(applyDiscount(discountPercentage)).sum

  private def applyDiscount(discountPercentage: Option[Double])(beforeDiscount: BigDecimal) =
    roundDown(discountPercentage.fold(beforeDiscount)(percentage => (100 - percentage) / 100 * beforeDiscount))

  def roundDown(d: BigDecimal): BigDecimal = d.setScale(2, RoundingMode.DOWN)

  def priceData(
      account: ZuoraAccount,
      subscription: ZuoraSubscription,
      cohortSpec: CohortSpec,
  ): Either[Failure, PriceData] = {

    MigrationType(cohortSpec) match {
      case GW2024            => GW2024Migration.priceData(subscription, account)
      case SupporterPlus2024 => SupporterPlus2024Migration.priceData(subscription)
    }
  }
}
