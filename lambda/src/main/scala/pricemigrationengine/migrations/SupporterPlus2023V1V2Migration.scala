package pricemigrationengine.model
import pricemigrationengine.model.CohortSpec
import pricemigrationengine.model.ZuoraProductCatalogue.{homeDeliveryRatePlans, productPricingMap}
import scala.math.BigDecimal.RoundingMode
import java.time.LocalDate

object SupporterPlus2023V1V2Migration {

  val maxLeadTime = 33
  val minLeadTime = 31

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

  def subscriptionRatePlan(subscription: ZuoraSubscription): Either[DataExtractionFailure, ZuoraRatePlan] = {
    subscription.ratePlans.filter(rp => rp.productName == "Supporter Plus").headOption match {
      case None =>
        Left(DataExtractionFailure(s"Subscription ${subscription.subscriptionNumber} doesn't have any rate plan"))
      case Some(ratePlan) => Right(ratePlan)
    }
  }

  def subscriptionRatePlanCharges(
      subscription: ZuoraSubscription,
      ratePlan: ZuoraRatePlan
  ): Either[DataExtractionFailure, List[ZuoraRatePlanCharge]] = {

    ratePlan.ratePlanCharges match {
      case Nil => {
        // Although not enforced by the signature of the function, for this error message to make sense we expect that
        // the rate plan belongs to the currency
        Left(
          DataExtractionFailure(s"Subscription ${subscription.subscriptionNumber} has a rate plan, but with no charge")
        )
      }
      case _ => Right(ratePlan.ratePlanCharges)
    }
  }

  def getOldPrice(
      subscription: ZuoraSubscription,
      ratePlanCharges: List[ZuoraRatePlanCharge]
  ): Either[DataExtractionFailure, BigDecimal] = {
    ratePlanCharges match {
      case Nil => {
        // Although not enforced by the signature of the function, for this error message to make sense we expect that
        // the rate plan charge belongs to the currency
        Left(
          DataExtractionFailure(
            s"Subscription ${subscription.subscriptionNumber} has no rate plan charges"
          )
        )
      }
      case _ => {
        Right(ratePlanCharges.flatMap(_.price).sum)
      }
    }
  }

  def currencyToNewPriceMonthlies(currency: String): Either[DataExtractionFailure, BigDecimal] = {
    newPriceMapMonthlies.get(currency) match {
      case None => Left(DataExtractionFailure(s"Could not determine a new monthly price for currency: ${currency}"))
      case Some(price) => Right(price)
    }
  }

  def currencyToNewPriceAnnuals(currency: String): Either[DataExtractionFailure, BigDecimal] = {
    newPriceMapAnnuals.get(currency) match {
      case None => Left(DataExtractionFailure(s"Could not determine a new annual price for currency: ${currency}"))
      case Some(price) => Right(price)
    }
  }

  def currencyToNewPrice(billingP: String, currency: String): Either[DataExtractionFailure, BigDecimal] = {
    if (billingP == "Month") {
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
  ): Either[DataExtractionFailure, ZuoraRatePlanCharge] =
    ZuoraRatePlanCharge
      .matchingRatePlanCharge(subscription, invoiceItem)
      .filterOrElse(
        hasNotPriceAndDiscount,
        DataExtractionFailure(s"Rate plan charge '${invoiceItem.chargeNumber}' has price and discount")
      )

  def ratePlanChargesOrFail(
      subscription: ZuoraSubscription,
      invoiceItems: Seq[ZuoraInvoiceItem]
  ): Either[DataExtractionFailure, Seq[ZuoraRatePlanCharge]] = {
    val ratePlanCharges = invoiceItems.map(item => ratePlanCharge(subscription, item))
    val failures = ratePlanCharges.collect { case Left(failure) => failure }

    if (failures.isEmpty) Right(ratePlanCharges.collect { case Right(charge) => charge })
    else Left(DataExtractionFailure(failures.map(_.reason).mkString(", ")))
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
          s"[SupporterPlus2023V1V2] Failed to find matching product rate plan charges for rate plan charges: ${failures.mkString(", ")}"
        )
      )
  }

  def billingPeriod(
      account: ZuoraAccount,
      catalogue: ZuoraProductCatalogue,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      nextServiceStartDate: LocalDate,
  ): Either[DataExtractionFailure, String] = {
    val invoiceItems = ZuoraInvoiceItem.items(invoiceList, subscription, nextServiceStartDate)
    for {
      ratePlanCharges <- ratePlanChargesOrFail(subscription, invoiceItems)
      billingPeriod <- ratePlanCharges
        .map(rpc => rpc.billingPeriod)
        .filter(period => period.isDefined)
        .map(rpc => rpc.get)
        .headOption
        .toRight(DataExtractionFailure("Unknown billing period"))
    } yield billingPeriod
  }

  def priceData(
      account: ZuoraAccount,
      catalogue: ZuoraProductCatalogue,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      nextServiceDate: LocalDate,
      cohortSpec: CohortSpec
  ): Either[DataExtractionFailure, PriceData] = {
    val result = (for {
      ratePlan <- subscriptionRatePlan(subscription)
      ratePlanCharges <- subscriptionRatePlanCharges(subscription, ratePlan)
      currency = ratePlanCharges.head.currency
      oldPrice <- getOldPrice(subscription, ratePlanCharges)
      billingP <- billingPeriod(account, catalogue, subscription, invoiceList, nextServiceDate)
      newPrice <- currencyToNewPrice(billingP, currency: String)
    } yield {
      PriceData(currency, oldPrice, newPrice, billingP)
    })
    result match {
      case Left(_) => result
      case Right(priceData) => {
        if (priceData.newPrice <= priceData.oldPrice) {
          Right(priceData)
        } else {
          // I this case the new price is higher than the old price. (Meaning that the v2 rate plan is higher than the)
          // old rate plan charge plus whichever extra contribution the subscription was paying.
          // We would want to know about that and investigate.
          Left(
            DataExtractionFailure(
              s"[SupporterPlus2023V1V2] Possibly incorrect pricing for subscription ${subscription.subscriptionNumber}. The new price, ${priceData.newPrice}, is higher than the old price, ${priceData.oldPrice}."
            )
          )
        }
      }
    }
  }

  def updateOfRatePlanToCurrentMonth(
      item: CohortItem,
      activeRatePlan: ZuoraRatePlan,
      effectiveDate: LocalDate,
  ): Either[DataExtractionFailure, ZuoraSubscriptionUpdate] = {

    /*
      So... the logic here, which is going to be similar for Annuals, is that we compare the estimated price
      with the price recorded in newPriceMapAnnuals. If they are similar, we will interpret as the fact that the
      subscription was paying the exact rate plan amount. If it is higher than we will issue a charge override
      for the contribution rate plan charge.

      All of this logic will be checked in tests.
     */

    val chargeOverrides: Option[List[ChargeOverride]] = for {
      currency <- item.currency
      price <- currencyToNewPrice("Month", currency).toOption
      oldPrice <- item.oldPrice
    } yield {
      val baseCharge = ChargeOverride(
        productRatePlanChargeId = "8a128ed885fc6ded018602296af13eba", // Monthly Base Charge
        billingPeriod = "Month",
        price = price
      )
      if (oldPrice > price) {
        List(
          baseCharge,
          ChargeOverride(
            productRatePlanChargeId = "8a128d7085fc6dec01860234cd075270", // Monthly Contribution
            billingPeriod = "Month",
            price = oldPrice - price
          )
        )
      } else {
        List(baseCharge)
      }
    }

    chargeOverrides match {
      case None => Left(DataExtractionFailure(s"Could not compute charge overrides for item: ${item}"))
      case Some(charges) =>
        Right(
          ZuoraSubscriptionUpdate(
            add = List(
              AddZuoraRatePlan("8a128ed885fc6ded018602296ace3eb8", effectiveDate, charges)
            ), // supporter plus monthly v2 with specific charges
            remove = List(RemoveZuoraRatePlan(activeRatePlan.id, effectiveDate)),
            currentTerm = None,
            currentTermPeriodType = None
          )
        )
    }
  }

  def updateOfRatePlanToCurrentAnnual(
      item: CohortItem,
      activeRatePlan: ZuoraRatePlan,
      effectiveDate: LocalDate,
  ): Either[DataExtractionFailure, ZuoraSubscriptionUpdate] = {

    val chargeOverrides = for {
      currency <- item.currency
      price <- currencyToNewPrice("Annual", currency).toOption
      oldPrice <- item.oldPrice
    } yield {
      val baseCharge = ChargeOverride(
        productRatePlanChargeId = "8a128ed885fc6ded01860228f7cb3d5f", // Annual Base Charge
        billingPeriod = "Annual",
        price = price
      )
      if (oldPrice > price) {
        List(
          baseCharge,
          ChargeOverride(
            productRatePlanChargeId = "8a12892d85fc6df4018602451322287f", // Annual Contribution
            billingPeriod = "Annual",
            price = oldPrice - price
          )
        )
      } else {
        List(baseCharge)
      }
    }

    chargeOverrides match {
      case None => Left(DataExtractionFailure(""))
      case Some(charges) =>
        Right(
          ZuoraSubscriptionUpdate(
            add = List(
              AddZuoraRatePlan("8a128ed885fc6ded01860228f77e3d5a", effectiveDate, charges)
            ), // supporter plus annual v2 with specific charges
            remove = List(RemoveZuoraRatePlan(activeRatePlan.id, effectiveDate)),
            currentTerm = None,
            currentTermPeriodType = None
          )
        )
    }
  }

  def zuoraUpdate(
      item: CohortItem,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      effectiveDate: LocalDate,
  ): Either[DataExtractionFailure, ZuoraSubscriptionUpdate] = {

    val activeRatePlans = (for {
      invoiceItem <- ZuoraInvoiceItem.items(invoiceList, subscription, effectiveDate)
      ratePlanCharge <- ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toSeq
      price <- ratePlanCharge.price.toSeq
      if price > 0
      ratePlan <- ZuoraRatePlan.ratePlanChargeToMatchingRatePlan(subscription, ratePlanCharge).toSeq
    } yield ratePlan).distinct

    if (activeRatePlans.isEmpty)
      Left(DataExtractionFailure(s"No rate plans to update for subscription ${subscription.subscriptionNumber}"))
    else if (activeRatePlans.size > 1)
      Left(DataExtractionFailure(s"Multiple rate plans to update: ${activeRatePlans.map(_.id)}"))
    else {
      // At this point we know that we have exactly one active rate plan
      val activeRatePlan = activeRatePlans.head
      item.billingPeriod match {
        case Some("Month")  => updateOfRatePlanToCurrentMonth(item, activeRatePlan, effectiveDate)
        case Some("Annual") => updateOfRatePlanToCurrentAnnual(item, activeRatePlan, effectiveDate)
        case _ =>
          Left(
            DataExtractionFailure(
              s"Unsupported billing period (expecting Month, Annual), got ${item.billingPeriod} from ${item}"
            )
          )
      }
    }
  }
}
