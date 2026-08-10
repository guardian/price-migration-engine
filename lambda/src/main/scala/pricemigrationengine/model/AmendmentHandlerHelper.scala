package pricemigrationengine.model

import pricemigrationengine.migrations.{
  DigiSubs2025Migration,
  GuardianWeekly2025Migration,
  Membership2025Migration,
  Newspaper2025P1Migration,
  Newspaper2025P3Migration,
  ProductMigration2025N4Migration,
  SupporterPlus2026Migration
}
import ujson.Value

import java.time.{Duration, Instant, LocalDate}

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
      case GuardianWeekly2025     => true
      case Newspaper2025P1        => true
      case Newspaper2025P3        => true
      case ProductMigration2025N4 => false
      case Membership2025         => true
      case DigiSubs2025           => true
      case SupporterPlus2026      => false
      case SupporterPlus2026N2    => false
      case SupporterPlus2026N3    => false
      case SupporterPlus2026N4    => false
      case SupporterPlus2026N5    => false
    }
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
      case Test1              => Left(ConfigFailure("case not supported"))
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
      case DigiSubs2025 =>
        DigiSubs2025Migration.amendmentOrderPayload(
          cohortItem,
          orderDate,
          accountNumber,
          subscriptionNumber,
          effectDate,
          zuora_subscription,
          commsPrice,
          invoiceList
        )
      case SupporterPlus2026 =>
        SupporterPlus2026Migration.amendmentOrderPayload(
          cohortItem,
          orderDate,
          accountNumber,
          subscriptionNumber,
          effectDate,
          zuora_subscription,
          commsPrice,
          invoiceList
        )
      case SupporterPlus2026N2 =>
        SupporterPlus2026Migration.amendmentOrderPayload(
          cohortItem,
          orderDate,
          accountNumber,
          subscriptionNumber,
          effectDate,
          zuora_subscription,
          commsPrice,
          invoiceList
        )
      case SupporterPlus2026N3 =>
        SupporterPlus2026Migration.amendmentOrderPayload(
          cohortItem,
          orderDate,
          accountNumber,
          subscriptionNumber,
          effectDate,
          zuora_subscription,
          commsPrice,
          invoiceList
        )
      case SupporterPlus2026N4 =>
        SupporterPlus2026Migration.amendmentOrderPayload(
          cohortItem,
          orderDate,
          accountNumber,
          subscriptionNumber,
          effectDate,
          zuora_subscription,
          commsPrice,
          invoiceList
        )
      case SupporterPlus2026N5 =>
        SupporterPlus2026Migration.amendmentOrderPayload(
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

  def isReadyToAmend(cohortSpec: CohortSpec, item: CohortItem, now: Instant): Boolean = {
    def itIsFewDaysAfterNotification(item: CohortItem): Boolean = {
      // Now minus 3 days, meaning that the amendment could happen 3 days
      // or 4 days later, depending on the exact time of the day
      val cursor = now.minus(Duration.ofDays(3))

      // Here we are going to `.get` an option knowing that a missing value will cause
      // a runtime error and stop the handler. If that value is not set, it means that
      // something incredibly wrong happened during notification and the CohortItem is corrupted.
      val notificationInstant = item.whenNotificationSent.get

      notificationInstant.isBefore(cursor)
    }
    MigrationType(cohortSpec) match {
      case Test1                  => true
      case GuardianWeekly2025     => true
      case Newspaper2025P1        => true
      case Newspaper2025P3        => true
      case ProductMigration2025N4 => true
      case Membership2025         => true
      case DigiSubs2025           => true
      case SupporterPlus2026      => itIsFewDaysAfterNotification(item)
      case SupporterPlus2026N2    => itIsFewDaysAfterNotification(item)
      case SupporterPlus2026N3    => itIsFewDaysAfterNotification(item)
      case SupporterPlus2026N4    => itIsFewDaysAfterNotification(item)
      case SupporterPlus2026N5    => itIsFewDaysAfterNotification(item)
    }
  }

}

sealed trait SubscriptionAmendmentAnalyseResult

// "SAAR" means "Subscription Amendment Analyse Result"

object SAARReadyToAmend extends SubscriptionAmendmentAnalyseResult
object SAARCancelledInZuora extends SubscriptionAmendmentAnalyseResult
object SAARExcludeFromMigration extends SubscriptionAmendmentAnalyseResult
object SAARFailNoisily extends SubscriptionAmendmentAnalyseResult

object SubscriptionAmendmentAnalyseResult {

  def toString(result: SubscriptionAmendmentAnalyseResult): String = {
    result match {
      case SAARReadyToAmend         => "SAARReadyToAmend"
      case SAARCancelledInZuora     => "SAARCancelledInZuora"
      case SAARExcludeFromMigration => "SAARExcludeFromMigration"
      case SAARFailNoisily          => "SAARFailNoisily"
    }
  }

  def subscriptionIsAmendableSupporterPlus2026(
      item: CohortItem,
      subscription: ZuoraSubscription,
      today: LocalDate
  ): Option[Boolean] = {
    for {
      ratePlan <- SI2025RateplanFromSub.uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(
        subscription,
        today
      )
      subscriptionBillingPeriod <- SI2025Extractions.determineBillingPeriod(ratePlan)
      itemBillingPeriod <- item.billingPeriod
    } yield ratePlan.productName == "Supporter Plus" &&
      BillingPeriod.toString(subscriptionBillingPeriod) == itemBillingPeriod
  }

  def analyseSupporterPlus2026(
      item: CohortItem,
      subscription: ZuoraSubscription,
      today: LocalDate
  ): Option[SubscriptionAmendmentAnalyseResult] = {
    subscriptionIsAmendableSupporterPlus2026(
      item,
      subscription,
      today
    ) match {
      case None              => Some(SAARFailNoisily)
      case Some(consistency) =>
        if (consistency) {
          Some(SAARReadyToAmend)
        } else {
          Some(SAARExcludeFromMigration)
        }
    }
  }

  def analyseSubscriptionForAmendment(
      cohortSpec: CohortSpec,
      item: CohortItem,
      subscription: ZuoraSubscription,
      today: LocalDate
  ): Option[SubscriptionAmendmentAnalyseResult] = {
    if (subscription.status == "Cancelled") {
      Some(SAARCancelledInZuora)
    } else {
      // Note that the reason why we are choosing not to apply the SupporterPlus2026 analyse
      // to other migration, is because although we noted how useful it is (see comment f4cb8d58)
      // I do not want to use a migration specific attribute to do so. If we want to extend this to
      // other migrations we will have to introduce a general CohortItem attribute.
      MigrationType(cohortSpec) match {
        case Test1                  => Some(SAARReadyToAmend)
        case GuardianWeekly2025     => Some(SAARReadyToAmend)
        case Newspaper2025P1        => Some(SAARReadyToAmend)
        case Newspaper2025P3        => Some(SAARReadyToAmend)
        case ProductMigration2025N4 => Some(SAARReadyToAmend)
        case Membership2025         => Some(SAARReadyToAmend)
        case DigiSubs2025           => Some(SAARReadyToAmend)
        case SupporterPlus2026      => analyseSupporterPlus2026(item, subscription, today)
        case SupporterPlus2026N2    => analyseSupporterPlus2026(item, subscription, today)
        case SupporterPlus2026N3    => analyseSupporterPlus2026(item, subscription, today)
        case SupporterPlus2026N4    => analyseSupporterPlus2026(item, subscription, today)
        case SupporterPlus2026N5    => analyseSupporterPlus2026(item, subscription, today)
      }
    }
  }
}
