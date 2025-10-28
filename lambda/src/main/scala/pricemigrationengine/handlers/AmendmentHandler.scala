package pricemigrationengine.handlers

import pricemigrationengine.model.{AmendmentHandlerHelper, ZuoraOrdersApiPrimitives}
import pricemigrationengine.model.CohortTableFilter.{
  AmendmentComplete,
  NotificationSendDateWrittenToSalesforce,
  UserOptOut,
  ZuoraCancellation
}
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

  private def main(
      cohortSpec: CohortSpec
  ): ZIO[Logging with CohortTable with Zuora with SalesforceClient, Failure, HandlerOutput] = {
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
            // If we reach the deadline, isComplete will be false.
            Clock.nanoTime.map(_ < deadline)
          )
          .mapZIO(item =>
            performAmendment(cohortSpec, catalogue, item).tapBoth(Logging.logFailure(item), Logging.logSuccess(item))
          )
          .runCount
      }
      now <- Clock.nanoTime
    } yield {
      val reachedDeadline = now >= deadline
      val isComplete = (count < batchSize) && !reachedDeadline
      HandlerOutput(isComplete = isComplete)
    }
  }

  private def performAmendment(
      cohortSpec: CohortSpec,
      catalogue: ZuoraProductCatalogue,
      item: CohortItem
  ): ZIO[CohortTable with Zuora with Logging with SalesforceClient, Failure, Unit] = {
    // This function performs the amendment (through the migration dispatch)
    // and updates the Cohort Item.
    (for {
      result <- performAmendmentMigrationDispatch(cohortSpec, catalogue, item)
      _ <- result match {
        case r: AARSuccessfulAmendment => {
          CohortTable.update(
            CohortItem(
              r.subscriptionNumber,
              processingStage = AmendmentComplete,
              amendmentEffectiveDate = Some(r.amendmentEffectiveDate),
              newPrice = Some(r.newPrice),
              newSubscriptionId = Some(r.newSubscriptionId),
              whenAmendmentDone = Some(r.whenDone)
            )
          )
        }
        case r: AARUserOptOut => {
          CohortTable
            .update(
              CohortItem(
                r.subscriptionNumber,
                processingStage = UserOptOut
              )
            )
        }
        case _ =>
          ZIO
            .fail(
              AmendmentFailure(
                s"[7f2bf362] unexpected amendment attempt result while processing subscription: ${item.subscriptionName}"
              )
            )
      }
    } yield ()).foldZIO(
      failure = {
        case e: SubscriptionCancelledInZuoraFailure => {
          CohortTable
            .update(
              CohortItem(
                item.subscriptionName,
                processingStage = ZuoraCancellation
              )
            )
        }
        case e: ZuoraUpdateFailure => {
          // We are only interested in the ZuoraUpdateFailures corresponding to message
          // "Operation failed due to a lock competition"
          // We succeed them without cohort item update to be done later.
          if (e.reason.contains("lock competition")) {
            ZIO.succeed(())
          } else {
            ZIO.fail(e)
          }
        }
        case e: ZuoraAmendmentPayloadBuildingFailure => {
          // In this case we do not mutate the cohort item, which will be picked
          // up at the next run of the lambda
          ZIO.succeed(())
        }
        case e => ZIO.fail(e)
      },
      success = { _ => ZIO.succeed(()) }
    )
  }

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

  private def doAmendment_ordersApi_typed_deprecated(
      cohortSpec: CohortSpec,
      catalogue: ZuoraProductCatalogue,
      item: CohortItem
  ): ZIO[Zuora with Logging, Failure, AARSuccessfulAmendment] = {
    for {
      subscriptionBeforeUpdate <- fetchSubscription(item)

      amendmentEffectiveDate <- ZIO
        .fromOption(item.amendmentEffectiveDate)
        .orElseFail(DataExtractionFailure(s"No start date in $item"))

      oldPrice <- ZIO.fromOption(item.oldPrice).orElseFail(DataExtractionFailure(s"No old price in $item"))

      commsPrice <-
        ZIO
          .fromOption(item.commsPrice)
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
              commsPrice = commsPrice
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
        case Membership2025 =>
          ZIO.fail(
            MigrationRoutingFailure("Membership2025 should not use doAmendment_ordersApi_typed_deprecated")
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
    } yield AARSuccessfulAmendment(
      item.subscriptionName,
      amendmentEffectiveDate,
      newPrice,
      subscriptionAfterUpdate.id,
      whenDone
    )
  }

  private def doAmendment_ordersApi_json_values(
      cohortSpec: CohortSpec,
      catalogue: ZuoraProductCatalogue,
      item: CohortItem
  ): ZIO[Zuora with Logging, Failure, AARSuccessfulAmendment] = {
    for {
      subscriptionBeforeUpdate <- fetchSubscription(item)

      amendmentEffectiveDate <- ZIO
        .fromOption(item.amendmentEffectiveDate)
        .orElseFail(DataExtractionFailure(s"No start date in $item"))

      oldPrice <- ZIO.fromOption(item.oldPrice).orElseFail(DataExtractionFailure(s"No old price in $item"))

      commsPrice <-
        ZIO
          .fromOption(item.commsPrice)
          .orElseFail(DataExtractionFailure(s"No commsPrice in $item"))

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
          AmendmentHandlerHelper.amendmentOrderPayload(
            cohortSpec = cohortSpec,
            cohortItem = item,
            orderDate = LocalDate.now(),
            accountNumber = account.basicInfo.accountNumber,
            subscriptionNumber = subscriptionBeforeUpdate.subscriptionNumber,
            effectDate = amendmentEffectiveDate,
            zuora_subscription = subscriptionBeforeUpdate,
            oldPrice = oldPrice,
            commsPrice = commsPrice,
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
          ZuoraUpdateFailure(
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
          AmendmentHandlerHelper
            .postAmendmentPriceCheck(cohortSpec, item, subscriptionAfterUpdate, commsPrice, newPrice, today)
        )
        .mapError(message => AmendmentFailure(message))

      whenDone <- Clock.instant
    } yield AARSuccessfulAmendment(
      item.subscriptionName,
      amendmentEffectiveDate,
      newPrice,
      subscriptionAfterUpdate.id,
      whenDone
    )
  }

  private def performAmendmentMigrationDispatch(
      cohortSpec: CohortSpec,
      catalogue: ZuoraProductCatalogue,
      item: CohortItem
  ): ZIO[Zuora with Logging with SalesforceClient, Failure, AmendmentAttemptResult] = {
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
      case ProductMigration2025N4 => {
        for {
          salesforcePriceRiseId <- ZIO
            .fromOption(item.salesforcePriceRiseId)
            .orElseFail(AmendmentFailure(s"Missing salesforcePriceRiseId for ${item.subscriptionName}"))
          priceRise <- SalesforceClient.getPriceRise(salesforcePriceRiseId)
          optOutFlag <- ZIO
            .fromOption(priceRise.Customer_Opt_Out__c)
            .orElseFail(
              AmendmentFailure(
                s"Missing Customer_Opt_Out__c in price rise $salesforcePriceRiseId, subscription: ${item.subscriptionName}"
              )
            )
          result <-
            if (optOutFlag)
              ZIO.succeed(AARUserOptOut(item.subscriptionName))
            else
              doAmendment_ordersApi_json_values(
                cohortSpec,
                catalogue,
                item
              )
        } yield result
      }

      case Membership2025 =>
        doAmendment_ordersApi_json_values(
          cohortSpec: CohortSpec,
          catalogue: ZuoraProductCatalogue,
          item: CohortItem
        )
    }
  }

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] = {
    main(input).provideSome[Logging](
      EnvConfig.cohortTable.layer,
      EnvConfig.zuora.layer,
      EnvConfig.stage.layer,
      DynamoDBZIOLive.impl,
      DynamoDBClientLive.impl,
      CohortTableLive.impl(input),
      ZuoraLive.impl,
      SalesforceClientLive.impl,
      EnvConfig.salesforce.layer
    )
  }
}
