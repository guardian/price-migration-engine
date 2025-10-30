package pricemigrationengine.model

import pricemigrationengine.migrations.{
  GuardianWeekly2025Migration,
  HomeDelivery2025Migration,
  Membership2025Migration,
  Newspaper2025P1Migration,
  Newspaper2025P3Migration,
  ProductMigration2025N4Migration
}
import ujson.Value

import java.time.LocalDate

object AmendmentHandlerHelper {
  def subscriptionHasCorrectBillingPeriodAfterUpdate(
      billingPeriodReferenceOpt: Option[String],
      subscriptionAfterUpdate: ZuoraSubscription,
      invoicePreviewAfterUpdate: ZuoraInvoiceList
  ): Option[Boolean] = {
    for {
      billingPeriodReference <- billingPeriodReferenceOpt
      ratePlan <- SI2025RateplanFromSubAndInvoices.determineRatePlan(
        subscriptionAfterUpdate,
        invoicePreviewAfterUpdate
      )
      billingPeriodAfterUpdate <- SI2025Extractions.determineBillingPeriod(ratePlan)
    } yield billingPeriodReference == BillingPeriod.toString(billingPeriodAfterUpdate)
  }

  def postAmendmentBillingPeriodCheck(
      item: CohortItem,
      subscriptionAfterUpdate: ZuoraSubscription,
      invoicePreviewAfterUpdate: ZuoraInvoiceList
  ): Either[Failure, Unit] = {
    val result = AmendmentHandlerHelper.subscriptionHasCorrectBillingPeriodAfterUpdate(
      item.billingPeriod,
      subscriptionAfterUpdate,
      invoicePreviewAfterUpdate
    )
    result match {
      case None =>
        Left(
          DataExtractionFailure(
            s"[b001b590] could not perform the billing period check with subscription: ${item.subscriptionName}"
          )
        )
      case Some(false) =>
        Left(
          AmendmentFailure(
            s"[f2e43c45] subscription: ${item.subscriptionName}, has failed the post amendment billing period check"
          )
        )
      case Some(true) => Right(())
    }
  }

  def priceEquality(float1: BigDecimal, float2: BigDecimal): Boolean = {
    (float1 - float2).abs < 0.001
  }

  private def shouldPerformFinalPriceCheck(cohortSpec: CohortSpec): Boolean = {
    MigrationType(cohortSpec) match {
      case Test1                  => true // default value
      case SupporterPlus2024      => false // [1]
      case GuardianWeekly2025     => true
      case Newspaper2025P1        => true
      case HomeDelivery2025       => true
      case Newspaper2025P3        => true
      case ProductMigration2025N4 => false
      case Membership2025         => true
    }

    // [1] We do not apply the check to the SupporterPlus2024 migration where, due to the way
    // the prices are computed, the new price can be higher than the
    // estimated price (which wasn't including the extra contribution).
  }

  def postAmendmentPriceCheck(
      cohortSpec: CohortSpec,
      cohortItem: CohortItem,
      subscriptionAfterUpdate: ZuoraSubscription,
      commsPrice: BigDecimal,
      newPrice: BigDecimal,
      today: LocalDate
  ): Either[String, Unit] = {
    if (shouldPerformFinalPriceCheck(cohortSpec: CohortSpec)) {
      if (SI2025Extractions.subscriptionHasActiveDiscounts(subscriptionAfterUpdate, today)) {
        if (newPrice <= commsPrice) {
          // should perform final check
          // has active discount, therefore only performing the inequality check
          // has passed the check
          Right(())
        } else {
          // should perform final check
          // has active discount, therefore only performing the inequality check
          // has failed the check
          Left(
            s"[6831cff2] Item ${cohortItem} has gone through the amendment step but has failed the final price check. commsPrice was ${commsPrice}, but the final price was ${newPrice} (nb: has discounts)"
          )
        }
      } else {
        if (AmendmentHandlerHelper.priceEquality(commsPrice, newPrice)) {
          // should perform final check
          // has no active discount, therefore performing the "equality" check
          // has passed the check
          Right(())
        } else {
          // should perform final check
          // has no active discount, therefore performing the "equality" check
          // has failed the check
          Left(
            s"[e9054daa] Item ${cohortItem} has gone through the amendment step but has failed the final price check. commsPrice was ${commsPrice}, but the final price was ${newPrice} (nb: no discounts)"
          )
        }
      }
    } else {
      // should not perform final check
      Right(())
    }
  }

  def amendmentOrderPayload(
      cohortSpec: CohortSpec,
      cohortItem: CohortItem,
      orderDate: LocalDate,
      accountNumber: String,
      subscriptionNumber: String,
      effectDate: LocalDate,
      zuora_subscription: ZuoraSubscription,
      oldPrice: BigDecimal,
      commsPrice: BigDecimal,
      invoiceList: ZuoraInvoiceList
  ): Either[Failure, Value] = {
    MigrationType(cohortSpec) match {
      case Test1 => Left(ConfigFailure("case not supported"))
      case SupporterPlus2024 =>
        Left(MigrationRoutingFailure("SupporterPlus2024 should not use doAmendment_ordersApi_json_values"))
      case GuardianWeekly2025 =>
        GuardianWeekly2025Migration.amendmentOrderPayload(
          cohortItem,
          orderDate,
          accountNumber,
          subscriptionNumber,
          effectDate,
          zuora_subscription,
          commsPrice,
          invoiceList
        )
      case Newspaper2025P1 =>
        Newspaper2025P1Migration.amendmentOrderPayload(
          cohortItem,
          orderDate,
          accountNumber,
          subscriptionNumber,
          effectDate,
          zuora_subscription,
          oldPrice,
          commsPrice,
          invoiceList
        )
      case HomeDelivery2025 =>
        HomeDelivery2025Migration.amendmentOrderPayload(
          cohortItem,
          orderDate,
          accountNumber,
          subscriptionNumber,
          effectDate,
          zuora_subscription,
          oldPrice,
          commsPrice,
          invoiceList
        )
      case Newspaper2025P3 =>
        Newspaper2025P3Migration.amendmentOrderPayload(
          cohortItem,
          orderDate,
          accountNumber,
          subscriptionNumber,
          effectDate,
          zuora_subscription,
          oldPrice,
          commsPrice,
          invoiceList
        )
      case ProductMigration2025N4 =>
        ProductMigration2025N4Migration.amendmentOrderPayload(
          orderDate,
          accountNumber,
          subscriptionNumber,
          effectDate,
          zuora_subscription,
          invoiceList
        )
      case Membership2025 =>
        Membership2025Migration.amendmentOrderPayload(
          cohortItem,
          orderDate,
          accountNumber,
          subscriptionNumber,
          effectDate,
          zuora_subscription,
          commsPrice,
          invoiceList
        )
    }
  }

}
