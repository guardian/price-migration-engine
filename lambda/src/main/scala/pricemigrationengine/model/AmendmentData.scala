package pricemigrationengine.model

import pricemigrationengine.migrations.{
  DigiSubs2025Migration,
  GuardianWeekly2025Migration,
  Membership2025Migration,
  Newspaper2025P1Migration,
  Newspaper2025P3Migration,
  ProductMigration2025N4Migration
}

import java.time.LocalDate
import scala.math.BigDecimal.RoundingMode

object AmendmentData {

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
      invoiceList: ZuoraInvoiceList,
  ): Either[Failure, PriceData] = {
    MigrationType(cohortSpec) match {
      case Test1                  => Left(ConfigFailure("Branch not supported"))
      case GuardianWeekly2025     => GuardianWeekly2025Migration.priceData(subscription, invoiceList, account)
      case Newspaper2025P1        => Newspaper2025P1Migration.priceData(subscription, invoiceList, account)
      case Newspaper2025P3        => Newspaper2025P3Migration.priceData(subscription, invoiceList, account)
      case ProductMigration2025N4 => ProductMigration2025N4Migration.priceData(subscription, invoiceList)
      case Membership2025         => Membership2025Migration.priceData(subscription, invoiceList)
      case DigiSubs2025           => DigiSubs2025Migration.priceData(subscription, invoiceList)
    }
  }
}
