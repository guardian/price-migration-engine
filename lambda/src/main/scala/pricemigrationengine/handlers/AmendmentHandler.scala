package pricemigrationengine.handlers

import pricemigrationengine.model.{AmendmentHandlerHelper, ZuoraOrdersApiPrimitives}
import pricemigrationengine.model.CohortTableFilter.{NotificationSendDateWrittenToSalesforce, ZuoraCancellation}
import pricemigrationengine.model._
import pricemigrationengine.migrations._
import pricemigrationengine.services._
import zio.{Clock, ZIO}

import java.time.LocalDate
import zio._
import ujson._

/** Carries out price-rise amendments in Zuora.
  */
object AmendmentHandler extends CohortHandler {

  private val batchSize = 15

  private def main(cohortSpec: CohortSpec): ZIO[Logging with CohortTable with Zuora, Failure, HandlerOutput] = {
    for {

      // The batch size of this lambda is particularly low (currently 15)
      // which should be plenty of time (observations show that it runs in 4 minutes in average)
      // but the refactoring we made here: https://github.com/guardian/price-migration-engine/pull/1221
      // (retry of the invoice preview retrieval) means that we have much less control
      // over the total time taken by the run.

      // Specifically in one instance an item took 13 minutes to complete, which caused issues
      // for the lambda itself. To address this problem we monitor the time since the start
      // of the run, and exit after 10 minutes.

      startingTime <- Clock.nanoTime
      deadline = startingTime + 10.minutes.toNanos

      catalogue <- Zuora.fetchProductCatalogue
      count <- {
        val items = cohortSpec.subscriptionNumber match {
          case None =>
            CohortTable
              .fetch(NotificationSendDateWrittenToSalesforce, None)
              .take(batchSize)
          case Some(subscriptionNumber) =>
            CohortTable
              .fetch(NotificationSendDateWrittenToSalesforce, None)
              .filter(item => item.subscriptionName == subscriptionNumber)
        }
        items
          .takeWhileZIO(_ =>
            // When we reach the deadline, we ignore later items from the array.
            // They will be picked up by the next run of the lambda.
            Clock.nanoTime.map(_ < deadline)
          )
          .mapZIO(item =>
            amend(cohortSpec, catalogue, item).tapBoth(Logging.logFailure(item), Logging.logSuccess(item))
          )
          .collect {
            // Here we are only counting `SuccessfulAmendmentResult`
            // and `SubscriptionCancelledInZuoraAmendmentResult`.
            // This will ensure that `AmendmentPreventedDueToLockResult` and `AmendmentPostponed`
            // are not counted, which would cause the lambda to return `HandlerOutput(true)`
            // too early.
            case s: SuccessfulAmendmentResult                   => s
            case c: SubscriptionCancelledInZuoraAmendmentResult => c
          }
          .runCount
      }

    } yield HandlerOutput(isComplete = count < batchSize)
  }

  private def amend(
      cohortSpec: CohortSpec,
      catalogue: ZuoraProductCatalogue,
      item: CohortItem
  ): ZIO[CohortTable with Zuora with Logging, Failure, AmendmentResult] =
    doAmendment(cohortSpec, catalogue, item).foldZIO(
      failure = {
        case _: SubscriptionCancelledInZuoraFailure => {
          // This happens when the subscription was cancelled in Zuora
          // in which case we simply update the processing state for this item in the database
          // Although it was given to us as a failure of `doAmendment`, the only effect of the database update, if it
          // is not recorded as a failure of `amend`, is to allow the processing to continue.
          CohortTable
            .update(
              CohortItem(
                item.subscriptionName,
                processingStage = ZuoraCancellation
              )
            )
            .as(SubscriptionCancelledInZuoraAmendmentResult(item.subscriptionName))
        }
        case e: ZuoraUpdateFailure => {
          // We are only interested in the ZuoraUpdateFailures corresponding to message
          // "Operation failed due to a lock competition"
          // We succeed them without cohort item update to be done later.
          if (e.reason.contains("lock competition")) {
            ZIO.succeed(AmendmentPreventedDueToLockResult(subscriptionNumber = item.subscriptionName))
          } else {
            ZIO.fail(e)
          }
        }
        case e: ZuoraAmendmentPayloadBuildingFailure => {
          ZIO.succeed(AmendmentPostponed(subscriptionNumber = item.subscriptionName))
        }
        case e => ZIO.fail(e)
      },
      success = { result =>
        CohortTable.update(CohortItem.fromSuccessfulAmendmentResult(result)).as(result)
      }
    )

  private def fetchSubscription(item: CohortItem): ZIO[Zuora, Failure, ZuoraSubscription] =
    Zuora
      .fetchSubscription(item.subscriptionName)
      .filterOrFail(_.status != "Cancelled")(
        SubscriptionCancelledInZuoraFailure(s"subscription ${item.subscriptionName} has been cancelled in Zuora")
      )

  private def renewSubscription(
      subscription: ZuoraSubscription,
      effectDate: LocalDate,
      account: ZuoraAccount
  ): ZIO[Zuora with Logging, Failure, Unit] = {
    val payload = ZuoraOrdersApiPrimitives.subscriptionRenewalPayload(
      LocalDate.now().toString,
      account.basicInfo.accountNumber,
      subscription.subscriptionNumber,
      effectDate.toString
    )
    for {
      _ <- Logging.info(s"[cce20c51] Renewing subscription ${subscription.subscriptionNumber} with payload ${payload}")
      _ <- Zuora.applyOrderAsynchronously(subscription.subscriptionNumber, payload, "subscription renewal")
    } yield ()
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
    }

    // [1] We do not apply the check to the SupporterPlus2024 migration where, due to the way
    // the prices are computed, the new price can be higher than the
    // estimated price (which wasn't including the extra contribution).
  }

  def postAmendmentPriceCheck(
      cohortSpec: CohortSpec,
      cohortItem: CohortItem,
      subscriptionAfterUpdate: ZuoraSubscription,
      estimatedNewPrice: BigDecimal,
      newPrice: BigDecimal,
      today: LocalDate
  ): Either[String, Unit] = {
    if (shouldPerformFinalPriceCheck(cohortSpec: CohortSpec)) {
      if (
        SI2025Extractions.subscriptionHasActiveDiscounts(subscriptionAfterUpdate, today)
        || AmendmentHandlerHelper.newPriceHasBeenCappedAt20Percent(cohortItem.oldPrice.get, newPrice)
        // Purposeful use of `.get` in the above as a cohortItem in Amendment step without
        // an `oldPrice` would be extremely pathological
      ) {
        if (newPrice > estimatedNewPrice) {
          // should perform final check
          // has active discount, therefore only performing the inequality check
          // has failed the check
          Left(
            s"[6831cff2] Item ${cohortItem} has gone through the amendment step but has failed the final price check. Estimated price was ${estimatedNewPrice}, but the final price was ${newPrice} (nb: has discounts)"
          )
        } else {
          // should perform final check
          // has active discount, therefore only performing the inequality check
          // has passed the check
          Right(())
        }
      } else {
        if (AmendmentHandlerHelper.priceEquality(estimatedNewPrice, newPrice)) {
          // should perform final check
          // has no active discount, therefore performing the "equality" check
          // has passed the check
          Right(())
        } else {
          // should perform final check
          // has no active discount, therefore performing the "equality" check
          // has failed the check
          Left(
            s"[e9054daa] Item ${cohortItem} has gone through the amendment step but has failed the final price check. Estimated price was ${estimatedNewPrice}, but the final price was ${newPrice} (nb: no discounts)"
          )
        }
      }
    } else {
      // should not perform final check
      Right(())
    }
  }

  private def doAmendment_ordersApi_typed_deprecated(
      cohortSpec: CohortSpec,
      catalogue: ZuoraProductCatalogue,
      item: CohortItem
  ): ZIO[Zuora with Logging, Failure, SuccessfulAmendmentResult] = {
    for {
      subscriptionBeforeUpdate <- fetchSubscription(item)

      amendmentEffectiveDate <- ZIO
        .fromOption(item.amendmentEffectiveDate)
        .orElseFail(DataExtractionFailure(s"No start date in $item"))

      oldPrice <- ZIO.fromOption(item.oldPrice).orElseFail(DataExtractionFailure(s"No old price in $item"))

      estimatedNewPrice <-
        ZIO
          .fromOption(item.estimatedNewPrice)
          .orElseFail(DataExtractionFailure(s"No estimated new price in $item"))

      invoicePreviewTargetDate = amendmentEffectiveDate.plusMonths(13)

      account <- Zuora.fetchAccount(
        subscriptionBeforeUpdate.accountNumber,
        subscriptionBeforeUpdate.subscriptionNumber
      )

      _ <- renewSubscription(subscriptionBeforeUpdate, subscriptionBeforeUpdate.termEndDate, account)

      order <- MigrationType(cohortSpec) match {
        case Test1 => ZIO.fail(ConfigFailure("Branch not supported"))
        case SupporterPlus2024 =>
          ZIO.fromEither(
            SupporterPlus2024Migration.amendmentOrderPayload(
              orderDate = LocalDate.now(),
              accountNumber = account.basicInfo.accountNumber,
              subscriptionNumber = subscriptionBeforeUpdate.subscriptionNumber,
              mainChargeEffectDate = amendmentEffectiveDate,
              subscription = subscriptionBeforeUpdate,
              oldPrice = oldPrice,
              estimatedNewPrice = estimatedNewPrice
            )
          )
        case GuardianWeekly2025 =>
          ZIO.fail(MigrationRoutingFailure("GuardianWeekly2025 should not use doAmendment_ordersApi_typed_deprecated"))
        case Newspaper2025P1 =>
          ZIO.fail(MigrationRoutingFailure("Newspaper2025P1 should not use doAmendment_ordersApi_typed_deprecated"))
        case HomeDelivery2025 =>
          ZIO.fail(MigrationRoutingFailure("HomeDelivery2025 should not use doAmendment_ordersApi_typed_deprecated"))
        case Newspaper2025P3 =>
          ZIO.fail(MigrationRoutingFailure("Newspaper2025P3 should not use doAmendment_ordersApi_typed_deprecated"))
        case ProductMigration2025N4 =>
          ZIO.fail(
            MigrationRoutingFailure("ProductMigration2025N4 should not use doAmendment_ordersApi_typed_deprecated")
          )
      }
      _ <- Logging.info(
        s"Amending subscription ${subscriptionBeforeUpdate.subscriptionNumber} with order ${order}"
      )

      _ <- Zuora.applyAmendmentOrder_typed_deprecated(subscriptionBeforeUpdate, order)

      subscriptionAfterUpdate <- fetchSubscription(item)

      invoicePreviewAfterUpdate <-
        Zuora.fetchInvoicePreview(subscriptionAfterUpdate.accountId, invoicePreviewTargetDate)

      _ <- {
        val test = AmendmentHandlerHelper.subscriptionHasCorrectBillingPeriodAfterUpdate(
          item.billingPeriod,
          subscriptionAfterUpdate,
          invoicePreviewAfterUpdate
        )
        test match {
          case None =>
            ZIO.fail(
              DataExtractionFailure(
                s"[b001b590] could not perform the billing period check with subscription: ${item.subscriptionName}"
              )
            )
          case Some(false) =>
            ZIO.fail(
              AmendmentFailure(
                s"[f2e43c45] subscription: ${item.subscriptionName}, has failed the post amendment billing period check"
              )
            )
          case Some(true) => ZIO.succeed(())
        }
      }

      newPrice <-
        ZIO.fromEither(
          AmendmentData.totalChargeAmount(
            subscriptionAfterUpdate,
            invoicePreviewAfterUpdate,
            amendmentEffectiveDate
          )
        )

      whenDone <- Clock.instant
    } yield SuccessfulAmendmentResult(
      item.subscriptionName,
      amendmentEffectiveDate,
      oldPrice,
      newPrice,
      estimatedNewPrice,
      subscriptionAfterUpdate.id,
      whenDone
    )
  }

  private def amendmentOrderPayload(
      cohortSpec: CohortSpec,
      cohortItem: CohortItem,
      orderDate: LocalDate,
      accountNumber: String,
      subscriptionNumber: String,
      effectDate: LocalDate,
      zuora_subscription: ZuoraSubscription,
      oldPrice: BigDecimal,
      estimatedNewPrice: BigDecimal,
      priceCap: Option[BigDecimal],
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
          oldPrice,
          estimatedNewPrice,
          priceCap,
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
          estimatedNewPrice,
          priceCap,
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
          estimatedNewPrice,
          priceCap,
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
          estimatedNewPrice,
          priceCap,
          invoiceList
        )
      case ProductMigration2025N4 =>
        ProductMigration2025N4Migration.amendmentOrderPayload(
          cohortItem,
          orderDate,
          accountNumber,
          subscriptionNumber,
          effectDate,
          zuora_subscription,
          oldPrice,
          estimatedNewPrice,
          priceCap,
          invoiceList
        )
    }
  }

  private def doAmendment_ordersApi_json_values(
      cohortSpec: CohortSpec,
      catalogue: ZuoraProductCatalogue,
      item: CohortItem
  ): ZIO[Zuora with Logging, Failure, SuccessfulAmendmentResult] = {
    for {
      subscriptionBeforeUpdate <- fetchSubscription(item)

      amendmentEffectiveDate <- ZIO
        .fromOption(item.amendmentEffectiveDate)
        .orElseFail(DataExtractionFailure(s"No start date in $item"))

      oldPrice <- ZIO.fromOption(item.oldPrice).orElseFail(DataExtractionFailure(s"No old price in $item"))

      estimatedNewPrice <-
        ZIO
          .fromOption(item.estimatedNewPrice)
          .orElseFail(DataExtractionFailure(s"No estimated new price in $item"))

      invoicePreviewTargetDate = amendmentEffectiveDate.plusMonths(13)

      account <- Zuora.fetchAccount(
        subscriptionBeforeUpdate.accountNumber,
        subscriptionBeforeUpdate.subscriptionNumber
      )

      _ <- renewSubscription(subscriptionBeforeUpdate, subscriptionBeforeUpdate.termEndDate, account)

      order <- (for {
        _ <- Logging.info(
          s"[e0418da6] fetching invoice preview before update, accountId: ${subscriptionBeforeUpdate.accountId}, target date: ${invoicePreviewTargetDate}"
        )
        invoicePreviewBeforeUpdate <-
          Zuora.fetchInvoicePreview(subscriptionBeforeUpdate.accountId, invoicePreviewTargetDate)
        _ <- Logging.info(
          s"[ec0e9b31] found invoice preview: ${invoicePreviewBeforeUpdate}"
        )
        _ <- Logging.info(
          s"[11ebeaa4] building amendment payload"
        )
        order <- ZIO.fromEither(
          amendmentOrderPayload(
            cohortSpec = cohortSpec,
            cohortItem = item,
            orderDate = LocalDate.now(),
            accountNumber = account.basicInfo.accountNumber,
            subscriptionNumber = subscriptionBeforeUpdate.subscriptionNumber,
            effectDate = amendmentEffectiveDate,
            zuora_subscription = subscriptionBeforeUpdate,
            oldPrice = oldPrice,
            estimatedNewPrice = estimatedNewPrice,
            priceCap = EstimationHandlerHelper.capRatio(cohortSpec).map(ratio => BigDecimal(ratio)),
            invoiceList = invoicePreviewBeforeUpdate
          )
        )
      } yield order)
        .retry(
          // Values chosen to ensure that the operation doesn't last more than 5 minutes
          // so that if an item was started just before the 10 minutes mark deadline of the handler,
          // then the entire lambda will complete before 15 minutes (ish)
          Schedule.spaced(1.minute) && Schedule.recurs(5)
        )
        .mapError(e =>
          // Note that there are two reason why this would happen
          // 1. MigrationRoutingFailure, or
          // 2. The `retry` has exited
          ZuoraAmendmentPayloadBuildingFailure(
            s"[2eecdf44] subscription: ${subscriptionBeforeUpdate.subscriptionNumber}, reason: ${e.reason}"
          )
        )
      _ <- Logging.info(
        s"[6e6da544] Amending subscription ${subscriptionBeforeUpdate.subscriptionNumber} with order ${order}"
      )

      _ <- Zuora.applyOrderAsynchronously(subscriptionBeforeUpdate.subscriptionNumber, order, "subscription amendment")

      subscriptionAfterUpdate <- fetchSubscription(item)

      invoicePreviewAfterUpdate <-
        Zuora.fetchInvoicePreview(subscriptionAfterUpdate.accountId, invoicePreviewTargetDate)

      newPrice <-
        ZIO.fromEither(
          AmendmentData.totalChargeAmount(
            subscriptionAfterUpdate,
            invoicePreviewAfterUpdate,
            amendmentEffectiveDate
          )
        )

      today <- Clock.currentDateTime.map(_.toLocalDate)

      _ <- ZIO
        .fromEither(
          postAmendmentPriceCheck(cohortSpec, item, subscriptionAfterUpdate, estimatedNewPrice, newPrice, today)
        )
        .mapError(message => AmendmentFailure(message))

      whenDone <- Clock.instant
    } yield SuccessfulAmendmentResult(
      item.subscriptionName,
      amendmentEffectiveDate,
      oldPrice,
      newPrice,
      estimatedNewPrice,
      subscriptionAfterUpdate.id,
      whenDone
    )
  }

  private def doAmendment(
      cohortSpec: CohortSpec,
      catalogue: ZuoraProductCatalogue,
      item: CohortItem
  ): ZIO[Zuora with Logging, Failure, SuccessfulAmendmentResult] = {
    MigrationType(cohortSpec) match {
      case Test1 => ZIO.fail(ConfigFailure("Branch not supported"))
      case SupporterPlus2024 =>
        doAmendment_ordersApi_typed_deprecated(
          cohortSpec: CohortSpec,
          catalogue: ZuoraProductCatalogue,
          item: CohortItem
        )
      case GuardianWeekly2025 =>
        doAmendment_ordersApi_json_values(
          cohortSpec: CohortSpec,
          catalogue: ZuoraProductCatalogue,
          item: CohortItem
        )
      case Newspaper2025P1 =>
        doAmendment_ordersApi_json_values(
          cohortSpec: CohortSpec,
          catalogue: ZuoraProductCatalogue,
          item: CohortItem
        )
      case HomeDelivery2025 =>
        doAmendment_ordersApi_json_values(
          cohortSpec: CohortSpec,
          catalogue: ZuoraProductCatalogue,
          item: CohortItem
        )
      case Newspaper2025P3 =>
        doAmendment_ordersApi_json_values(
          cohortSpec: CohortSpec,
          catalogue: ZuoraProductCatalogue,
          item: CohortItem
        )
      case ProductMigration2025N4 =>
        doAmendment_ordersApi_json_values(
          cohortSpec: CohortSpec,
          catalogue: ZuoraProductCatalogue,
          item: CohortItem
        )
    }
  }

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] = {
    // [1] We are preventing the amendment of ProductMigration2025N4 items
    MigrationType(input) match {
      case ProductMigration2025N4 => ZIO.succeed(HandlerOutput(isComplete = true)) // See [1] above
      case _ =>
        main(input).provideSome[Logging](
          EnvConfig.cohortTable.layer,
          EnvConfig.zuora.layer,
          EnvConfig.stage.layer,
          DynamoDBZIOLive.impl,
          DynamoDBClientLive.impl,
          CohortTableLive.impl(input),
          ZuoraLive.impl
        )
    }

  }
}
