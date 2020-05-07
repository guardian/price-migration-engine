package pricemigrationengine.model

import java.time.LocalDate

case class AmendmentData(startDate: LocalDate, priceData: PriceData)

case class PriceData(currency: String, oldPrice: BigDecimal, newPrice: BigDecimal)

object AmendmentData {

  def apply(
      pricing: ZuoraPricingData,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      earliestStartDate: LocalDate
  ): Either[AmendmentDataFailure, AmendmentData] =
    for {
      startDate <- nextBillingDate(invoiceList, after = earliestStartDate.minusDays(1))
      price <- priceData(pricing, subscription, invoiceList, startDate)
    } yield AmendmentData(startDate, priceData = price)

  def nextBillingDate(invoiceList: ZuoraInvoiceList, after: LocalDate): Either[AmendmentDataFailure, LocalDate] = {
    invoiceList.invoiceItems
      .map(_.serviceStartDate)
      .sortBy(_.toEpochDay)
      .dropWhile(date => !date.isAfter(after))
      .headOption
      .toRight(AmendmentDataFailure(s"Cannot determine next billing date after $after from $invoiceList"))
  }

  /**
    * General algorithm:
    * <ol>
    * <li>For a given date, gather chargeNumber and chargeAmount fields from invoice preview.</li>
    * <li>For each chargeNumber, match it with ratePlanCharge number on sub and get corresponding productRatePlanChargeId.</li>
    * <li>For each productRatePlanChargeId, match it with id in catalogue and get pricing currency and price.</li>
    * <li>Get combined chargeAmount field for old price, and combined pricing price for new price, and currency.</li>
    * </ol>
    */
  def priceData(
      pricing: ZuoraPricingData,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      startDate: LocalDate
  ): Either[AmendmentDataFailure, PriceData] = {

    def matchingProductRatePlanChargeId(invoiceItem: ZuoraInvoiceItem): Option[ZuoraProductRatePlanChargeId] = {
      val ratePlanCharges = subscription.ratePlans.flatMap(_.ratePlanCharges)
      ratePlanCharges.find(_.number == invoiceItem.chargeNumber).map(_.productRatePlanChargeId)
    }

    def matchingPricing(productRatePlanChargeId: ZuoraProductRatePlanChargeId): Option[ZuoraPricing] =
      pricing.get(productRatePlanChargeId)

    /*
     * Our matching operations can fail so this gathers up the results
     * and just returns the failing results if there are any,
     * otherwise returns all the successful results.
     */
    def eitherFailingOrPassingResults[K, V](m: Map[K, Option[V]]): Either[Seq[K], Seq[V]] = {
      def pairToEither(pair: (K, Option[V])): Either[K, V] =
        pair match {
          case (k, None)    => Left(k)
          case (_, Some(v)) => Right(v)
        }
      m.partitionMap(pairToEither) match {
        case (Nil, values) => Right(values.toSeq)
        case (keys, _)     => Left(keys.toSeq)
      }
    }

    val invoiceItems = invoiceList.invoiceItems.filter(_.serviceStartDate == startDate)

    for {
      productRatePlanChargeIds <- eitherFailingOrPassingResults(
        invoiceItems.map(item => item -> matchingProductRatePlanChargeId(item)).toMap
      ).left.map(
        invoiceItems =>
          AmendmentDataFailure(
            s"Failed to find matching rate plan charge for invoice items: ${invoiceItems.map(_.chargeNumber).mkString}"
        )
      )
      pricings <- eitherFailingOrPassingResults(
        productRatePlanChargeIds.map(id => id -> matchingPricing(id)).toMap
      ).left.map(
        ids => AmendmentDataFailure(s"Failed to find matching pricing for rate plan charges: ${ids.mkString}")
      )
      pricing <- pricings.headOption
        .map(p => Right(p))
        .getOrElse(Left(AmendmentDataFailure(s"No invoice items for date: $startDate")))
    } yield {
      PriceData(
        currency = pricing.currency,
        oldPrice = invoiceItems.map(_.chargeAmount).sum,
        newPrice = pricings.map(_.price).sum
      )
    }
  }
}
