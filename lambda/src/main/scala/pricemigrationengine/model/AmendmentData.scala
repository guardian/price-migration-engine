package pricemigrationengine.model

import java.time.LocalDate

import scala.math.BigDecimal.RoundingMode

case class AmendmentData(startDate: LocalDate, priceData: PriceData)

case class PriceData(currency: Currency, oldPrice: BigDecimal, newPrice: BigDecimal, billingPeriod: String)

object AmendmentData {

  def apply(
      pricing: ZuoraPricingData,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      earliestStartDate: LocalDate
  ): Either[AmendmentDataFailure, AmendmentData] =
    for {
      startDate <- nextServiceStartDate(invoiceList, subscription, onOrAfter = earliestStartDate)
      price <- priceData(pricing, subscription, invoiceList, startDate)
    } yield AmendmentData(startDate, priceData = price)

  def nextServiceStartDate(
      invoiceList: ZuoraInvoiceList,
      subscription: ZuoraSubscription,
      onOrAfter: LocalDate
  ): Either[AmendmentDataFailure, LocalDate] =
    ZuoraInvoiceItem
      .itemsForSubscription(invoiceList, subscription)
      .map(_.serviceStartDate)
      .sortBy(_.toEpochDay)
      .dropWhile(_.isBefore(onOrAfter))
      .headOption
      .toRight(AmendmentDataFailure(s"Cannot determine next billing date on or after $onOrAfter from $invoiceList"))

  /*
   * New prices can only be calculated from a combination of the rate plan charge
   * and its corresponding product catalogue entries.
   * This is because discounts are only reliable in the rate plan charge
   * and new prices have to come from the product catalogue.
   */
  private case class RatePlanChargeAndPricing(ratePlanCharge: ZuoraRatePlanCharge, pricing: ZuoraPricing)

  /**
    * General algorithm:
    * <ol>
    * <li>For a given date, gather chargeNumber fields from invoice preview.</li>
    * <li>For each chargeNumber, match it with ratePlanCharge number on sub and get corresponding ratePlanCharge.</li>
    * <li>For each ratePlanCharge, match its productRatePlanChargeId with id in catalogue and get pricing currency, price and discount percentage.</li>
    * <li>Get combined chargeAmount field for old price, and combined pricing price for new price, and currency.</li>
    * </ol>
    */
  def priceData(
      pricingData: ZuoraPricingData,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      startDate: LocalDate
  ): Either[AmendmentDataFailure, PriceData] = {

    def hasNotPriceAndDiscount(ratePlanCharge: ZuoraRatePlanCharge) =
      ratePlanCharge.price.isDefined ^ ratePlanCharge.discountPercentage.exists(_ > 0)

    def ratePlanCharge(invoiceItem: ZuoraInvoiceItem) =
      ZuoraRatePlanCharge
        .matchingRatePlanCharge(subscription, invoiceItem)
        .filterOrElse(
          hasNotPriceAndDiscount,
          AmendmentDataFailure(s"Rate plan charge '${invoiceItem.chargeNumber}' has price and discount")
        )

    def pricings(ratePlanCharges: Seq[ZuoraRatePlanCharge]) = {
      /*
       * distinct because where a sub has a discount rate plan,
       * the same discount will appear against each product rate plan charge in the invoice preview.
       */
      val pricings = ratePlanCharges.distinctBy(_.productRatePlanChargeId).map(pricing)
      val failures = pricings.collect { case Left(failure) => failure }
      if (failures.isEmpty) Right(pricings.collect { case Right(pricing) => pricing })
      else
        Left(AmendmentDataFailure(s"Failed to find matching pricing for rate plan charges: ${failures.mkString(", ")}"))
    }

    def pricing(ratePlanCharge: ZuoraRatePlanCharge) =
      ZuoraPricing
        .matchingPricing(pricingData, ratePlanCharge)
        .toRight(ratePlanCharge.productRatePlanChargeId)
        .map(pricing => RatePlanChargeAndPricing(ratePlanCharge, pricing))

    val invoiceItems = ZuoraInvoiceItem.items(invoiceList, subscription, startDate)

    val ratePlanCharges = {
      val ratePlanCharges = invoiceItems.map(ratePlanCharge)
      val failures = ratePlanCharges.collect { case Left(failure) => failure }
      if (failures.isEmpty) Right(ratePlanCharges.collect { case Right(charge) => charge })
      else Left(AmendmentDataFailure(failures.map(_.reason).mkString(", ")))
    }

    for {
      ratePlanCharges <- ratePlanCharges
      pricings <- pricings(ratePlanCharges)
      currency <- pricings.headOption
        .map(p => Right(p.pricing.currency))
        .getOrElse(Left(AmendmentDataFailure(s"No invoice items for date: $startDate")))
      oldPrice <- totalChargeAmount(subscription, invoiceList, startDate)
      newPrice <- combinePrices(pricings)
      billingPeriod <- ratePlanCharges
        .flatMap(_.billingPeriod)
        .headOption
        .toRight(AmendmentDataFailure("Unknown billing period"))
    } yield {
      PriceData(
        currency,
        oldPrice,
        newPrice,
        billingPeriod
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
      invoiceItem <- ZuoraInvoiceItem.items(invoiceList, subscription, serviceStartDate)
      ratePlanCharge <- ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toSeq
    } yield individualChargeAmount(ratePlanCharge)

    val discounts = amounts.collect { case Left(percentageDiscount) => percentageDiscount }

    if (discounts.length > 1) Left(AmendmentDataFailure(s"Multiple discounts applied: ${discounts.mkString(", ")}"))
    else
      Right {
        applyDiscountAndThenSum(
          discountPercentage = discounts.headOption,
          beforeDiscount = amounts.collect { case Right(amount) => amount }
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

  /**
    * Combines prices over the given billing period.
    *
    * TODO: this assumes that the prices are per month,
    * and that billing periods of over a month are priced in the same ratio.
    * This is only a safe assumption for vouchers.
    */
  private def combinePrices(
      ratePlanChargeAndPricings: Seq[RatePlanChargeAndPricing]
  ): Either[AmendmentDataFailure, BigDecimal] = {

    def price(ratePlanChargeAndPricing: RatePlanChargeAndPricing): Either[AmendmentDataFailure, Option[BigDecimal]] = {
      def multiplier(ratePlanCharge: ZuoraRatePlanCharge) = ratePlanCharge.billingPeriod match {
        case Some("Month")       => Right(1)
        case Some("Quarter")     => Right(3)
        case Some("Semi_Annual") => Right(6)
        case Some("Annual")      => Right(12)
        case other =>
          Left(AmendmentDataFailure(s"Rate plan charge '${ratePlanCharge.number}' has unknown billing period: $other"))
      }
      ratePlanChargeAndPricing.pricing.price.filter(_ > 0) match {
        case None => Right(None)
        case Some(price) =>
          multiplier(ratePlanChargeAndPricing.ratePlanCharge) map (multiplier => Some(price * multiplier))
      }
    }

    val discountPercentageOrFailure = {
      val discounts = ratePlanChargeAndPricings.flatMap(_.ratePlanCharge.discountPercentage.filter(_ > 0))
      if (discounts.length > 1) Left(AmendmentDataFailure("Subscription has more than one discount"))
      else Right(discounts.headOption)
    }

    val prices = ratePlanChargeAndPricings.map(price)

    for {
      discountPercentage <- discountPercentageOrFailure
      _ <- prices.collectFirst { case Left(e) => e }.toLeft(())
    } yield
      applyDiscountAndThenSum(
        discountPercentage,
        beforeDiscount = prices.collect { case Right(price) => price }.flatten
      )
  }

  private def applyDiscountAndThenSum(discountPercentage: Option[Double], beforeDiscount: Seq[BigDecimal]): BigDecimal =
    beforeDiscount.map(applyDiscount(discountPercentage)).sum

  private def applyDiscount(discountPercentage: Option[Double])(beforeDiscount: BigDecimal) =
    roundDown(discountPercentage.fold(beforeDiscount)(percentage => (100 - percentage) / 100 * beforeDiscount))

  def roundDown(d: BigDecimal): BigDecimal = d.setScale(2, RoundingMode.DOWN)
}
