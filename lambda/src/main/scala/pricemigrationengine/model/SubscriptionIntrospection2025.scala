package pricemigrationengine.model

import java.time.LocalDate

/*

  Date: 27th May 2025
  Author: Pascal

  SubscriptionIntrospection2025, renamed to SI2025, was introduced in May 2025, in preparation of the
  Guardian Weekly and Newspaper, migrations starting soon to help improve the migration
  specific codes.

  "Modern" migrations use libraries to perform most of the legwork, but a bit needs
  to be implemented manually to match specific Marketing requests and, among other things implement
  the price grid (Pascal prefers hardcoding the price grid of each migration, notably in cases
  the prices are not yet, or identical, to the price catalogue)

  The purpose of SI2025 is to facilitate writing the migration specific code
  by abstracting away some helpers functions we have (re)written a few times over the years

 */

object SI2025RateplanFromSubAndInvoices {
  def invoicePreviewToChargeNumber(invoiceList: ZuoraInvoiceList): Option[String] =
    invoiceList.invoiceItems.headOption.map(invoiceItem => invoiceItem.chargeNumber)

  def ratePlanChargeNumberToMatchingRatePlan(
      subscription: ZuoraSubscription,
      ratePlanChargeNumber: String
  ): Option[ZuoraRatePlan] =
    subscription.ratePlans.find(_.ratePlanCharges.exists(_.number == ratePlanChargeNumber))

  // `determineRatePlan` is the main function of this object. Given a subscription and an invoice List
  // it determines the rate plan that corresponds to the invoice list. This is a very reliable
  // way of determining the rate plan that other data should be derived from.

  def determineRatePlan(subscription: ZuoraSubscription, invoiceList: ZuoraInvoiceList): Option[ZuoraRatePlan] = {
    for {
      ratePlanChargeNumber <- SI2025RateplanFromSubAndInvoices.invoicePreviewToChargeNumber(invoiceList)
      ratePlan <- SI2025RateplanFromSubAndInvoices.ratePlanChargeNumberToMatchingRatePlan(
        subscription,
        ratePlanChargeNumber
      )
    } yield ratePlan
  }
}

object SI2025RateplanFromSub {

  // This version of determineRatePlan doesn't use invoice previews and can be used in
  // a situation where only the subscription is available and assuming there is only
  // one active rate plan on the subscription (which is arguably the case of all our
  // subscriptions). This is the one we can use from StartDates to determine the last
  // price migration date in a context where we only have access to the subscription.

  def determineRatePlan(subscription: ZuoraSubscription): Option[ZuoraRatePlan] = {
    subscription.ratePlans
      .filter(ratePlan => ZuoraRatePlan.ratePlanIsActive(ratePlan))
      .find(ratePlan => ratePlan.productName != "Discounts")

  }
}

object SI2025Extractions {

  def determineCurrency(
      ratePlan: ZuoraRatePlan
  ): Option[Currency] = {
    ZuoraRatePlan.ratePlanToCurrency(ratePlan)
  }

  def determineBillingPeriod(
      ratePlan: ZuoraRatePlan
  ): Option[BillingPeriod] = ZuoraRatePlan.ratePlanToBillingPeriod(ratePlan)

  def determineOldPrice(ratePlan: ZuoraRatePlan): BigDecimal = {
    ratePlan.ratePlanCharges.foldLeft(BigDecimal(0))((price: BigDecimal, ratePlanCharge: ZuoraRatePlanCharge) =>
      price + ratePlanCharge.price.getOrElse(BigDecimal(0))
    )
  }

  def determineLastPriceMigrationDate(
      ratePlan: ZuoraRatePlan
  ): Option[LocalDate] = {
    // This function is used to decide the date of the last price migration.

    // It is part of the enforcement of the policy to not re-migrate a subscription within
    // one year of its last price migration. Note that it uses invoice previews to determine
    // the rate plan of interest from which the date is going to be read. As such the value
    // that it returns is the date at which we moved to the latest charges
    // of the rate plans we are currently on.

    for {
      ratePlanCharge <- ratePlan.ratePlanCharges.headOption
      date <- ratePlanCharge.originalOrderDate
    } yield date
  }

  def getDiscountByRatePlanName(subscription: ZuoraSubscription, ratePlanName: String): Option[ZuoraRatePlan] = {
    // The product name is always "Discount", but the ratePlanName is more freeform.
    // I have noticed
    // ratePlanName: "Percentage"
    // ratePlanName: "Customer Experience Adjustment - Voucher"
    // To handle this we use ratePlanName.contains
    subscription.ratePlans
      .filter(ratePlan => ZuoraRatePlan.ratePlanIsActive(ratePlan))
      .filter(ratePlan => ratePlan.productName == "Discounts")
      .find(ratePlan => ratePlan.ratePlanName.contains(ratePlanName))
  }

  def getPercentageOrAdjustementDiscount(subscription: ZuoraSubscription): Option[ZuoraRatePlan] = {
    // This function will extract a "Percentage" or a "Adjustment" ratePlan, whichever is present

    // Note that the choice of values "Percentage" and "Adjustment" comes from the metadata
    // in Marketing spreadsheets and as we have highlighted in the body of function `getDiscountByRatePlanName`
    // those are not even always equal to the rate plan names but may appear as substring.

    var a = getDiscountByRatePlanName(subscription: ZuoraSubscription, "Percentage")
    var b = getDiscountByRatePlanName(subscription: ZuoraSubscription, "Adjustment")
    a.orElse(b)
  }

  def getActiveDiscount(subscription: ZuoraSubscription): List[ZuoraRatePlan] = {
    subscription.ratePlans
      .filter(ratePlan => ZuoraRatePlan.ratePlanIsActive(ratePlan))
      .filter(ratePlan => ratePlan.productName == "Discounts")
  }

  def subscriptionHasActiveDiscounts(subscription: ZuoraSubscription): Boolean = {
    getActiveDiscount(subscription: ZuoraSubscription).nonEmpty
  }
}

object SI2025Templates {

  def determineNewPrice(): BigDecimal = {
    // This function is a placeholder for consistency to remind the engineer that
    // by convention the new prices are hard coded in the migration itself
    throw new Exception("[6170c05c] this function should not be called. See code comment.")
  }

  /*
    The function below is a template, which needs to be copied into the migration module
    and adapted accordingly (for instance you might want to implement `determineNewPrice()`)

    It is only used in the test suite
   */

  def priceData(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList
  ): Either[DataExtractionFailure, PriceData] = {
    val priceDataOpt: Option[PriceData] = for {
      ratePlan <- SI2025RateplanFromSubAndInvoices.determineRatePlan(subscription, invoiceList)
      currency <- SI2025Extractions.determineCurrency(ratePlan)
      oldPrice = SI2025Extractions.determineOldPrice(ratePlan: ZuoraRatePlan)
      newPrice = BigDecimal(
        2.71
      ) // Should replace this by a call to the migration's own `determineNewPrice()` the price grid lookup
      billingPeriod <- SI2025Extractions.determineBillingPeriod(ratePlan)
    } yield PriceData(currency, oldPrice, newPrice, BillingPeriod.toString(billingPeriod))
    priceDataOpt match {
      case Some(pricedata) => Right(pricedata)
      case None =>
        Left(
          DataExtractionFailure(s"Could not determine PriceData for subscription ${subscription.subscriptionNumber}")
        )
    }
  }
}
