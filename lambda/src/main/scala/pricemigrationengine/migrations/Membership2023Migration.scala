package pricemigrationengine.migrations

import pricemigrationengine.model.{
  AddZuoraRatePlan,
  AmendmentDataFailure,
  CohortSpec,
  Currency,
  Membership2023Annuals,
  Membership2023Monthlies,
  MigrationType,
  PriceData,
  RemoveZuoraRatePlan,
  ZuoraAccount,
  ZuoraInvoiceItem,
  ZuoraInvoiceList,
  ZuoraProductCatalogue,
  ZuoraRatePlan,
  ZuoraRatePlanCharge,
  ZuoraSubscription,
  ZuoraSubscriptionUpdate
}

import java.time.LocalDate

object Membership2023Migration {

  val maxLeadTime = 33
  val minLeadTime = 31

  val priceMapMonthlies: Map[Currency, BigDecimal] = Map(
    "GBP" -> BigDecimal(7),
    "AUD" -> BigDecimal(14.99),
    "CAD" -> BigDecimal(12.99),
    "EUR" -> BigDecimal(9.99),
    "USD" -> BigDecimal(9.99),
  )

  val priceMapAnnuals: Map[Currency, BigDecimal] = Map(
    "GBP" -> BigDecimal(75),
    "AUD" -> BigDecimal(160),
    "CAD" -> BigDecimal(120),
    "EUR" -> BigDecimal(95),
    "USD" -> BigDecimal(120),
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
    priceMapMonthlies.get(currency) match {
      case None => Left(AmendmentDataFailure(s"Could not determine a new monthly price for currency: ${currency}"))
      case Some(price) => Right(price)
    }
  }

  def currencyToNewPriceAnnuals(currency: String): Either[AmendmentDataFailure, BigDecimal] = {
    priceMapAnnuals.get(currency) match {
      case None => Left(AmendmentDataFailure(s"Could not determine a new annual price for currency: ${currency}"))
      case Some(price) => Right(price)
    }
  }

  def priceData(
      account: ZuoraAccount,
      catalogue: ZuoraProductCatalogue,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      nextServiceDate: LocalDate,
      cohortSpec: CohortSpec
  ): Either[AmendmentDataFailure, PriceData] = {
    MigrationType(cohortSpec) match {
      case Membership2023Monthlies =>
        for {
          ratePlan <- subscriptionRatePlan(subscription)
          ratePlanCharge <- subscriptionRatePlanCharge(subscription, ratePlan)
          currency = ratePlanCharge.currency
          oldPrice <- getOldPrice(subscription, ratePlanCharge)
          newPrice <- currencyToNewPriceMonthlies(currency: String)
        } yield PriceData(currency, oldPrice, newPrice, "Month")
      case Membership2023Annuals =>
        for {
          ratePlan <- subscriptionRatePlan(subscription)
          ratePlanCharge <- subscriptionRatePlanCharge(subscription, ratePlan)
          currency = ratePlanCharge.currency
          oldPrice <- getOldPrice(subscription, ratePlanCharge)
          newPrice <- currencyToNewPriceAnnuals(currency: String)
        } yield PriceData(currency, oldPrice, newPrice, "Annual")
      case _ => Left(AmendmentDataFailure(s"(error: 7ba45f10) Incorrect cohort spec for this function: ${cohortSpec}"))
    }
  }

  def zuoraUpdate_Monthlies(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      effectiveDate: LocalDate,
  ): Either[AmendmentDataFailure, ZuoraSubscriptionUpdate] = {

    // This variant has a simpler signature than its classic counterpart.

    val activeRatePlans = (for {
      invoiceItem <- ZuoraInvoiceItem.items(invoiceList, subscription, effectiveDate)
      ratePlanCharge <- ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toSeq
      price <- ratePlanCharge.price.toSeq
      if price > 0
      ratePlan <- ZuoraRatePlan.ratePlanChargeToMatchingRatePlan(subscription, ratePlanCharge).toSeq
    } yield ratePlan).distinct

    if (activeRatePlans.isEmpty)
      Left(AmendmentDataFailure(s"No rate plans to update for subscription ${subscription.subscriptionNumber}"))
    else if (activeRatePlans.size > 1)
      Left(AmendmentDataFailure(s"Multiple rate plans to update: ${activeRatePlans.map(_.id)}"))
    else {

      // At this point we know that we have exactly one activeRatePlans
      val activeRatePlan = activeRatePlans.head

      // In the case of Membership Batch 1 and 2 (monthlies), things are now more simple. We can hardcode the rate plan
      Right(
        ZuoraSubscriptionUpdate(
          add = List(AddZuoraRatePlan("8a1287c586832d250186a2040b1548fe", effectiveDate)),
          remove = List(RemoveZuoraRatePlan(activeRatePlan.id, effectiveDate)),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    }
  }

  def zuoraUpdate_Annuals(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      effectiveDate: LocalDate,
  ): Either[AmendmentDataFailure, ZuoraSubscriptionUpdate] = {

    // This variant has a simpler signature than its classic counterpart.

    val activeRatePlans = (for {
      invoiceItem <- ZuoraInvoiceItem.items(invoiceList, subscription, effectiveDate)
      ratePlanCharge <- ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toSeq
      price <- ratePlanCharge.price.toSeq
      if price > 0
      ratePlan <- ZuoraRatePlan.ratePlanChargeToMatchingRatePlan(subscription, ratePlanCharge).toSeq
    } yield ratePlan).distinct

    if (activeRatePlans.isEmpty)
      Left(AmendmentDataFailure(s"No rate plans to update for subscription ${subscription.subscriptionNumber}"))
    else if (activeRatePlans.size > 1)
      Left(AmendmentDataFailure(s"Multiple rate plans to update: ${activeRatePlans.map(_.id)}"))
    else {

      // At this point we know that we have exactly one activeRatePlans
      val activeRatePlan = activeRatePlans.head

      // Batch 3 (annuals)
      Right(
        ZuoraSubscriptionUpdate(
          add = List(AddZuoraRatePlan("8a129ce886834fa90186a20c3ee70b6a", effectiveDate)),
          remove = List(RemoveZuoraRatePlan(activeRatePlan.id, effectiveDate)),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    }
  }
}
