package pricemigrationengine.model

import java.time.LocalDate

import scala.math.BigDecimal.RoundingMode

case class AmendmentData(startDate: LocalDate, priceData: PriceData)

case class PriceData(currency: String, oldPrice: BigDecimal, newPrice: BigDecimal, billingPeriod: String)

object AmendmentData {

  def apply(
      pricing: ZuoraPricingData,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      earliestStartDate: LocalDate
  ): Either[AmendmentDataFailure, AmendmentData] =
    for {
      startDate <- nextServiceStartDate(invoiceList, onOrAfter = earliestStartDate)
      price <- priceData(pricing, subscription, invoiceList, startDate)
    } yield AmendmentData(startDate, priceData = price)

  def nextServiceStartDate(
      invoiceList: ZuoraInvoiceList,
      onOrAfter: LocalDate
  ): Either[AmendmentDataFailure, LocalDate] =
    invoiceList.invoiceItems
      .map(_.serviceStartDate)
      .sortBy(_.toEpochDay)
      .dropWhile(_.isBefore(onOrAfter))
      .headOption
      .toRight(AmendmentDataFailure(s"Cannot determine next billing date on or after $onOrAfter from $invoiceList"))

  /**
    * General algorithm:
    * <ol>
    * <li>For a given date, gather chargeNumber and chargeAmount fields from invoice preview.</li>
    * <li>For each chargeNumber, match it with ratePlanCharge number on sub and get corresponding productRatePlanChargeId.</li>
    * <li>For each productRatePlanChargeId, match it with id in catalogue and get pricing currency, price and discount percentage.</li>
    * <li>Get combined chargeAmount field for old price, and combined pricing price for new price, and currency.</li>
    * </ol>
    */
  def priceData(
      pricingData: ZuoraPricingData,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      startDate: LocalDate
  ): Either[AmendmentDataFailure, PriceData] = {

    def matchingPricing(productRatePlanChargeId: ZuoraProductRatePlanChargeId): Option[ZuoraPricing] =
      pricingData.get(productRatePlanChargeId)

    /*
     * Our matching operations can fail so this gathers up the results
     * and just returns the failing results if there are any,
     * otherwise returns all the successful results.
     */
    def eitherFailingOrPassingResults[K, V](ks: Seq[K], f: K => Option[V]): Either[Seq[K], Seq[V]] = {
      val (fails, successes) = ks.foldLeft((Seq.empty[K], Seq.empty[V])) {
        case ((accK, accV), k) =>
          f(k) match {
            case None    => (accK :+ k, accV)
            case Some(v) => (accK, accV :+ v)
          }
      }
      Either.cond(fails.isEmpty, successes, fails)
    }

    def hasNoMoreThanOneDiscount(pricings: Seq[ZuoraPricing]) =
      pricings.count(_.discountPercentage > 0) <= 1

    def hasNotPriceAndDiscount(pricings: Seq[ZuoraPricing]) =
      pricings.forall(pricing => pricing.price.isDefined ^ pricing.discountPercentage > 0)

    val invoiceItems = invoiceList.invoiceItems.filter(_.serviceStartDate == startDate)

    for {
      ratePlanCharges <- eitherFailingOrPassingResults(
        invoiceItems,
        ZuoraRatePlanCharge.matchingRatePlanCharge(subscription)
      ).left.map(
        invoiceItems =>
          AmendmentDataFailure(
            s"Failed to find matching rate plan charge for invoice items: ${invoiceItems.map(_.chargeNumber).mkString(", ")}"
        )
      )
      pricings <- eitherFailingOrPassingResults(
        /*
         * distinct because where a sub has a discount rate plan,
         * the same discount will appear against each product rate plan charge in the invoice preview.
         */
        ratePlanCharges.map(_.productRatePlanChargeId).distinct,
        matchingPricing
      ).left
        .map(
          ids => AmendmentDataFailure(s"Failed to find matching pricing for rate plan charges: ${ids.mkString}")
        )
        .filterOrElse(
          hasNoMoreThanOneDiscount,
          AmendmentDataFailure("Has multiple discount rate plan charges")
        )
        .filterOrElse(
          hasNotPriceAndDiscount,
          AmendmentDataFailure("Some rate plan charges have both a price and a discount")
        )
      firstPricing <- pricings.headOption
        .map(p => Right(p))
        .getOrElse(Left(AmendmentDataFailure(s"No invoice items for date: $startDate")))
      oldPrice <- totalChargeAmount(subscription, invoiceList, startDate)
    } yield {
      PriceData(
        currency = firstPricing.currency,
        oldPrice,
        newPrice = combinePrices(pricings),
        // head is safe here because if there were no rate plan charges, would already have shortcircuited
        billingPeriod = ratePlanCharges.head.billingPeriod.getOrElse("Unknown")
      )
    }
  }

  /**
    * Total charge amount, including taxes, for the service period starting on the given service start date.
    */
  def totalChargeAmount(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      serviceStartDate: LocalDate
  ): Either[AmendmentDataFailure, BigDecimal] = {
    /*
     * As charge amounts on Zuora invoice previews don't include tax,
     * we have to find the price of the rate plan charges on the subscription
     * that correspond with items in the invoice list.
     */
    val amounts = for {
      invoiceItem <- ZuoraInvoiceItem.items(invoiceList, serviceStartDate).distinctBy(_.chargeNumber)
      ratePlanCharge <- ZuoraRatePlanCharge.matchingRatePlanCharge(subscription)(invoiceItem).toSeq
    } yield individualChargeAmount(ratePlanCharge)

    val discounts = amounts.collect { case Left(percentageDiscount) => percentageDiscount }

    if (discounts.length > 1) Left(AmendmentDataFailure(s"Multiple discounts applied: ${discounts.mkString(", ")}"))
    else
      Right {
        applyDiscount(
          discountPercentage = discounts.headOption,
          beforeDiscount = amounts.collect { case Right(amount) => amount }.sum
        )
      }
  }

  /**
    * Either a left discount percentage or a right absolute amount.
    */
  def individualChargeAmount(ratePlanCharge: ZuoraRatePlanCharge): Either[Double, BigDecimal] =
    ratePlanCharge.price match {
      case None             => ratePlanCharge.discountPercentage.toLeft(0)
      case Some(p) if p > 0 => Right(p)
      case Some(_)          => Right(0)
    }

  def combinePrices(pricings: Seq[ZuoraPricing]): BigDecimal = {
    val discountPercentage = pricings.map(_.discountPercentage).find(_ > 0)
    val beforeDiscount = pricings.flatMap(_.price).sum
    applyDiscount(discountPercentage, beforeDiscount)
  }

  private def applyDiscount(discountPercentage: Option[Double], beforeDiscount: BigDecimal) =
    roundDown(discountPercentage.fold(beforeDiscount)(percentage => (100 - percentage) / 100 * beforeDiscount))

  def roundDown(d: BigDecimal): BigDecimal = d.setScale(2, RoundingMode.DOWN)
}
