package pricemigrationengine.model

import pricemigrationengine.model.ZuoraProductCatalogue.{productPricingMap, homeDeliveryRatePlans}

import java.time.LocalDate
import scala.math.BigDecimal.RoundingMode

case class AmendmentData(startDate: LocalDate, priceData: PriceData)

case class PriceData(currency: Currency, oldPrice: BigDecimal, newPrice: BigDecimal, billingPeriod: String)

/** <p>Data used to estimate and report on price-rise amendments to subscriptions.</p>
  *
  * <p>The general approach here is to use a combination of Zuora invoice previews, subscriptions and the product
  * catalogue to determine billing dates, current charges and future charges.</p>
  *
  * <p>We use invoice previews only to find future billing dates and the list of rate plan charge numbers that will
  * apply on future billing dates. The amounts given in the invoice preview are unreliable because they don't include
  * tax or give any way to calculate tax.</p>
  *
  * <p>To find the detail of the rate plan charge, we use the rate plan charge numbers from invoice previews to look up
  * rate plan charges in subscriptions. The price of a rate plan charge in a subscription is the only reliable way to
  * get the price including tax.</p>
  *
  * <p>The combination of a subscription rate plan charge and the corresponding product rate plan charge, found in the
  * product catalogue, give us all the information we need to calculate future charges including taxes and
  * discounts.</p>
  */
object AmendmentData {

  def apply(
      catalogue: ZuoraProductCatalogue,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      earliestStartDate: LocalDate
  ): Either[AmendmentDataFailure, AmendmentData] = {
    val isEchoLegacy = subscription.ratePlans.filter(_.ratePlanName == "Echo-Legacy").nonEmpty

    for {
      startDate <-
        if (isEchoLegacy) nextServiceStartDateEchoLegacy(invoiceList, subscription, onOrAfter = earliestStartDate)
        else nextServiceStartDate(invoiceList, subscription, onOrAfter = earliestStartDate)
      price <- priceData(catalogue, subscription, invoiceList, startDate)
    } yield AmendmentData(startDate, priceData = price)
  }

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

  def nextServiceStartDateEchoLegacy(
      invoiceList: ZuoraInvoiceList,
      subscription: ZuoraSubscription,
      onOrAfter: LocalDate
  ): Either[AmendmentDataFailure, LocalDate] =
    ZuoraInvoiceItem
      .itemsForSubscription(invoiceList, subscription)
      .filter(_.productName == "Newspaper Delivery")
      .groupBy(_.serviceStartDate)
      .collect {
        case (_, chargedDays) if chargedDays.length == 7 => chargedDays
      }
      .flatten
      .toSeq
      .map(_.serviceStartDate)
      .sortBy(_.toEpochDay)
      .dropWhile(_.isBefore(onOrAfter))
      .headOption
      .toRight(AmendmentDataFailure(s"Cannot determine next billing date on or after $onOrAfter from $invoiceList"))

  /** New prices can only be calculated from a combination of the subscription rate plan charge and its corresponding
    * product rate plan charge.<br/> This is because discounts are only reliable in the subscription rate plan charge,
    * and new prices have to come from the product rate plan charge.
    */
  case class RatePlanChargePair(
      chargeFromSubscription: ZuoraRatePlanCharge,
      chargeFromProduct: ZuoraProductRatePlanCharge
  )

  /** General algorithm: <ol> <li>For a given date, gather chargeNumber fields from invoice preview.</li> <li>For each
    * chargeNumber, match it with ratePlanCharge number on sub and get corresponding ratePlanCharge.</li> <li>For each
    * ratePlanCharge, match its productRatePlanChargeId with id in catalogue and get pricing currency, price and
    * discount percentage.</li> <li>Get combined chargeAmount field for old price, and combined pricing price for new
    * price, and currency.</li> </ol>
    */
  def priceData(
      catalogue: ZuoraProductCatalogue,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      startDate: LocalDate
  ): Either[AmendmentDataFailure, PriceData] = {

    def hasNotPriceAndDiscount(ratePlanCharge: ZuoraRatePlanCharge) =
      ratePlanCharge.price.isDefined ^ ratePlanCharge.discountPercentage.exists(_ > 0)

    def ratePlanCharge(invoiceItem: ZuoraInvoiceItem): Either[AmendmentDataFailure, ZuoraRatePlanCharge] =
      ZuoraRatePlanCharge
        .matchingRatePlanCharge(subscription, invoiceItem)
        .filterOrElse(
          hasNotPriceAndDiscount,
          AmendmentDataFailure(s"Rate plan charge '${invoiceItem.chargeNumber}' has price and discount")
        )

    def ratePlanChargePairs(
        ratePlanCharges: Seq[ZuoraRatePlanCharge]
    ): Either[AmendmentDataFailure, Seq[RatePlanChargePair]] = {
      /*
       * distinct because where a sub has a discount rate plan,
       * the same discount will appear against each product rate plan charge in the invoice preview.
       */
      val pairs = ratePlanCharges.distinctBy(_.productRatePlanChargeId).map(ratePlanChargePair)

      val failures = pairs.collect { case Left(failure) => failure }
      if (failures.isEmpty) Right(pairs.collect { case Right(pricing) => pricing })
      else
        Left(
          AmendmentDataFailure(
            s"Failed to find matching product rate plan charges for rate plan charges: ${failures.mkString(", ")}"
          )
        )
    }

    def ratePlanChargePair(
        ratePlanCharge: ZuoraRatePlanCharge
    ): Either[ZuoraProductRatePlanChargeId, RatePlanChargePair] = {
      productPricingMap(catalogue)
        .get(ratePlanCharge.productRatePlanChargeId)
        .toRight(ratePlanCharge.productRatePlanChargeId)
        .map(productRatePlanCharge => RatePlanChargePair(ratePlanCharge, productRatePlanCharge))
    }
    val invoiceItems = ZuoraInvoiceItem.items(invoiceList, subscription, startDate)

    val ratePlanChargesOrFail: Either[AmendmentDataFailure, Seq[ZuoraRatePlanCharge]] = {
      val ratePlanCharges = invoiceItems.map(ratePlanCharge)

      val failures = ratePlanCharges.collect { case Left(failure) => failure }

      if (failures.isEmpty) Right(ratePlanCharges.collect { case Right(charge) => charge })
      else Left(AmendmentDataFailure(failures.map(_.reason).mkString(", ")))
    }

    for {
      ratePlanCharges <- ratePlanChargesOrFail
      ratePlan <- ZuoraRatePlan
        .ratePlan(subscription, ratePlanCharges.head)
        .toRight(AmendmentDataFailure(s"Failed to get RatePlan for charges: $ratePlanCharges"))

      isEchoLegacy = ratePlan.ratePlanName == "Echo-Legacy"

      pairs <-
        if (isEchoLegacy) EchoLegacy.getNewRatePlans(catalogue, ratePlanCharges).map(_.chargePairs)
        else ratePlanChargePairs(ratePlanCharges)

      currency <- pairs.headOption
        .map(p => Right(p.chargeFromSubscription.currency))
        .getOrElse(Left(AmendmentDataFailure(s"No invoice items for date: $startDate")))
      oldPrice <- totalChargeAmount(subscription, invoiceList, startDate)
      newPrice <- totalChargeAmount(pairs)
      billingPeriod <- pairs
        .flatMap(_.chargeFromSubscription.billingPeriod)
        .headOption
        .toRight(AmendmentDataFailure("Unknown billing period"))
    } yield PriceData(currency, oldPrice, newPrice, billingPeriod)
  }

  /** Total charge amount, including taxes and discounts, for the service period starting on the given service start
    * date.
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

  /** Either a left discount percentage or a right absolute amount.
    */
  def individualChargeAmount(ratePlanCharge: ZuoraRatePlanCharge): Either[Double, BigDecimal] =
    ratePlanCharge.price match {
      case None             => ratePlanCharge.discountPercentage.toLeft(0)
      case Some(p) if p > 0 => Right(p)
      case Some(_)          => Right(0)
    }

  /** Total charge amount, including taxes and discounts, for a given set of <code>RatePlanChargePairs</code>, using the
    * product rate plan charge price as a basis.<br/> Absolute prices will come from the product rate plan charge.<br/>
    * Percentage discount amounts will come from the subscription rate plan charge.<br/> Absolute discount amounts will
    * be ignored.
    */
  private def totalChargeAmount(
      ratePlanChargePairs: Seq[RatePlanChargePair]
  ): Either[AmendmentDataFailure, BigDecimal] = {

    def price(ratePlanChargePair: RatePlanChargePair): Either[AmendmentDataFailure, Option[BigDecimal]] =
      ZuoraPricing
        .pricing(ratePlanChargePair.chargeFromProduct, ratePlanChargePair.chargeFromSubscription.currency)
        .flatMap(_.price)
        .filter(_ > 0) match {
        case None => Right(None)
        case Some(price) =>
          adjustedForBillingPeriod(
            price,
            ratePlanChargePair.chargeFromSubscription.billingPeriod,
            ratePlanChargePair.chargeFromProduct.billingPeriod
          ).map(Some(_))
            .left
            .map(e =>
              AmendmentDataFailure(
                s"Failed to calculate amount of rate plan charge ${ratePlanChargePair.chargeFromSubscription.number}: $e"
              )
            )
      }

    val discountPercentageOrFailure = {
      val discounts = ratePlanChargePairs.flatMap(_.chargeFromSubscription.discountPercentage.filter(_ > 0))
      if (discounts.length > 1) Left(AmendmentDataFailure("Subscription has more than one discount"))
      else Right(discounts.headOption)
    }

    val prices = ratePlanChargePairs.map(price)

    for {
      discountPercentage <- discountPercentageOrFailure
      _ <- prices.collectFirst { case Left(e) => e }.toLeft(())
    } yield applyDiscountAndThenSum(
      discountPercentage,
      beforeDiscount = prices.collect { case Right(price) => price }.flatten
    )
  }

  private def applyDiscountAndThenSum(discountPercentage: Option[Double], beforeDiscount: Seq[BigDecimal]): BigDecimal =
    beforeDiscount.map(applyDiscount(discountPercentage)).sum

  private def applyDiscount(discountPercentage: Option[Double])(beforeDiscount: BigDecimal) =
    roundDown(discountPercentage.fold(beforeDiscount)(percentage => (100 - percentage) / 100 * beforeDiscount))

  def roundDown(d: BigDecimal): BigDecimal = d.setScale(2, RoundingMode.DOWN)

  /** In some cases, a product rate plan charge has a monthly billing period but a subscription has overridden it with a
    * rate plan charge with a different billing period. In these cases, the price has to be multiplied by the number of
    * months in the subscription billing period.
    */
  private[model] def adjustedForBillingPeriod(
      price: BigDecimal,
      subscriptionBillingPeriod: Option[String],
      productBillingPeriod: Option[String]
  ): Either[AmendmentDataFailure, BigDecimal] = {

    val multiple = (subscriptionBillingPeriod, productBillingPeriod) match {
      case (Some(billingPeriod), Some(prodBillingPeriod)) if billingPeriod == prodBillingPeriod => Right(1)
      case (Some(billingPeriod), Some("Month")) =>
        monthMultiples
          .get(billingPeriod)
          .map(Right(_))
          .getOrElse(Left(AmendmentDataFailure(s"Unknown billing period: $billingPeriod")))
      case (billingPeriod, productBillingPeriod) =>
        Left(
          AmendmentDataFailure(
            s"Invalid billing period combinations: subscription = $billingPeriod, product = $productBillingPeriod"
          )
        )
    }

    multiple map (_ * price)
  }

  private val monthMultiples: Map[String, Int] = Map(
    "Quarter" -> 3,
    "Semi_Annual" -> 6,
    "Annual" -> 12
  )
}
