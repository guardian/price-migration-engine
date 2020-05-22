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
      startDate <- nextBillingDate(invoiceList, onOrAfter = earliestStartDate)
      price <- priceData(pricing, subscription, invoiceList, startDate)
    } yield AmendmentData(startDate, priceData = price)

  def nextBillingDate(invoiceList: ZuoraInvoiceList, onOrAfter: LocalDate): Either[AmendmentDataFailure, LocalDate] =
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

    def matchingRatePlanCharge(invoiceItem: ZuoraInvoiceItem): Option[ZuoraRatePlanCharge] = {
      val ratePlanCharges = subscription.ratePlans.flatMap(_.ratePlanCharges)
      ratePlanCharges.find(_.number == invoiceItem.chargeNumber)
    }

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
        matchingRatePlanCharge
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
    } yield {
      PriceData(
        currency = firstPricing.currency,
        // invoice items include discount rate plan charges so summing them should give correct amount even when discounted
        oldPrice = invoiceItems.map(_.chargeAmount).sum,
        newPrice = combinePrices(pricings),
        // head is safe here because if there were no rate plan charges, would already have shortcircuited
        billingPeriod = ratePlanCharges.head.billingPeriod.getOrElse("Unknown")
      )
    }
  }

  def totalChargeAmount(invoiceList: ZuoraInvoiceList, billingDate: LocalDate): BigDecimal =
    ZuoraInvoiceItem.items(invoiceList, billingDate).map(_.chargeAmount).sum

  def combinePrices(pricings: Seq[ZuoraPricing]): BigDecimal = {
    val discountPercentage = pricings.map(_.discountPercentage).find(_ > 0)
    val beforeDiscount = pricings.flatMap(_.price).sum
    roundDown(discountPercentage.fold(beforeDiscount)(_ / 100 * beforeDiscount))
  }

  def roundDown(d: BigDecimal): BigDecimal = d.setScale(2, RoundingMode.DOWN)
}
