package pricemigrationengine.handlers

import pricemigrationengine.model.{AmendmentHandlerHelper, ZuoraOrdersApiPrimitives}
import pricemigrationengine.model.CohortTableFilter.{
  AmendmentComplete,
  ExcludedFromMigration,
  NotificationSendDateWrittenToSalesforce,
  NotificationSendDateWrittenToSalesforceN4HOLD,
  UserOptOut,
  ZuoraCancellation
}
import pricemigrationengine.model._
import pricemigrationengine.migrations._
import pricemigrationengine.services._
import zio.{Clock, ZIO}

import java.time.{LocalDate, ZoneOffset}
import zio._
import ujson._

/** Carries out price-rise amendments in Zuora.
  */
object AmendmentHandler extends CohortHandler {

  // The exact value doesn't matter because this handler monitors its own run time,
  // but the variable itself is used to detect the end of the daily process.
  private val batchSize = 100

  private def main(
      cohortSpec: CohortSpec
  ): ZIO[Logging with CohortTable with Zuora with SalesforceClient, Failure, HandlerOutput] = {
    for {
      now <- Clock.instant
      startingTime <- Clock.nanoTime
      deadline = startingTime + 10.minutes.toNanos
      _ <- performN4Unlock() // Remove this at the end of N4, in November 2026
      count <- {
        val items = cohortSpec.subscriptionNumber match {
          case None =>
            CohortTable
              .fetch(NotificationSendDateWrittenToSalesforce, None)
              .filter(item => AmendmentHandlerHelper.isReadyToAmend(cohortSpec, item, now))
              .filter(item => Dispatch.belongs(cohortSpec, item))
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
          .mapZIO(item => processCohortItem(cohortSpec, item))
          .mapZIO(_.map(CohortTable.update).getOrElse(ZIO.unit))
          .runCount
      }
      endingTime <- Clock.nanoTime
    } yield {
      val reachedDeadline = endingTime >= deadline
      val isComplete = (count < batchSize) && !reachedDeadline
      HandlerOutput(isComplete = isComplete)
    }
  }

  def processCohortItem(
      cohortSpec: CohortSpec,
      item: CohortItem
  ): ZIO[SalesforceClient with Logging with Zuora, Failure, Option[CohortItem]] = {
    for {
      now <- Clock.instant
      subscription <- Zuora.fetchSubscription(item.subscriptionName)
      analyseResult <- ZIO
        .fromOption(
          SubscriptionAmendmentAnalyseResult.analyseSubscriptionForAmendment(
            cohortSpec,
            item,
            subscription,
            LocalDate.ofInstant(now, ZoneOffset.UTC)
          )
        )
        .orElseFail(
          DataExtractionFailure(
            s"[0c1a6fc5] could not determine SubscriptionAmendmentAnalyseResult for item {$item}"
          )
        )
      _ <- Logging.info(
        s"[470b97f8] analyse subscription for amendment, item: ${item}, result: ${SubscriptionAmendmentAnalyseResult.toString(analyseResult)}"
      )
      maybeUpdate <- evaluateAnalyseResult(
        cohortSpec,
        item,
        analyseResult
      )
    } yield maybeUpdate
  }

  def evaluateAnalyseResult(
      cohortSpec: CohortSpec,
      item: CohortItem,
      analyseResult: SubscriptionAmendmentAnalyseResult
  ): ZIO[SalesforceClient with Logging with Zuora, Failure, Option[CohortItem]] = for {
    maybeUpdate <- analyseResult match {
      case SAARReadyToAmend =>
        performAmendmentAttempt(
          cohortSpec,
          item
        )
      case SAARCancelledInZuora =>

        ZIO.some(
          CohortItem(
            item.subscriptionName,
            processingStage = ZuoraCancellation
          )
        )
      case SAARExcludeFromMigration =>
        ZIO.some(
          CohortItem(
            item.subscriptionName,
            processingStage = ExcludedFromMigration
          )
        )
      case SAARFailNoisily =>
        ZIO.fail(
          AmendmentFailure(
            s"[908d45f0] Processing SAARFailNoisily from the amendment handler for ${item}. Please investigate."
          )
        )
    }
  } yield maybeUpdate

  def performN4Unlock(): ZIO[CohortTable with Logging, Failure, Unit] = {
    // This effect performs the monitoring of N4 items and unlock those for which delayN4AmendmentUntil
    // has been reached. The unlocking corresponds to moving the items from
    // NotificationSendDateWrittenToSalesforceN4HOLD to NotificationSendDateWrittenToSalesforce

    // This function will be removed at the end of N4, in November 2026

    for {
      today <- Clock.currentDateTime.map(_.toLocalDate)
      _ <- CohortTable
        .fetch(NotificationSendDateWrittenToSalesforceN4HOLD, None)
        .mapZIO(item =>
          item.delayN4AmendmentUntil match {
            case Some(unlockDate) if Date.equalOrInOrder(unlockDate, today) => {
              CohortTable.update(
                CohortItem(
                  item.subscriptionName,
                  processingStage = NotificationSendDateWrittenToSalesforce,
                )
              )
            }
            case _ => ZIO.unit
          }
        )
        .runDrain
    } yield ()
  }

  private def performAmendmentAttempt(
      cohortSpec: CohortSpec,
      item: CohortItem
  ): ZIO[Zuora with Logging with SalesforceClient, Failure, Option[CohortItem]] =
    (for {
      result <- performAmendmentAttemptWithResult(cohortSpec, item)
      updatedItem <- result match {
        case r: AARSuccessfulAmendment => {
          ZIO.succeed(
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
          ZIO.succeed(
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
    } yield updatedItem).foldZIO(
      failure = {
        case e: ZuoraUpdateFailure => {
          // If the failure was a lock competition, we do not want to alarm by reporting a
          // ZIO.fail. Instead, we return a ZIO.none to leave the item untouched in dynamo,
          // and the item will be retried in the next run of the lambda.
          if (e.reason.contains("lock competition")) {
            ZIO.none
          } else {
            ZIO.fail(e)
          }
        }
        case e => ZIO.fail(e)
      },
      success = { update => ZIO.some(update) }
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

  private def doAmendmentUsingOrdersApiWithJsonValues(
      cohortSpec: CohortSpec,
      item: CohortItem
  ): ZIO[Zuora with Logging, Failure, AARSuccessfulAmendment] = {
    for {
      subscriptionBeforeUpdate <- Zuora.fetchSubscription(item.subscriptionName)

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

      subscriptionAfterUpdate <- Zuora.fetchSubscription(item.subscriptionName)

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

      // Date: 29 October 2025
      // Author: Pascal
      // This check was introduced to add extra security to N4. To be decommissioned at the end of N4
      // unless we decide to generalise it and absorb the price check and the billing period check in
      // one single unit.
      _ <- (MigrationType(cohortSpec) match {
        case ProductMigration2025N4 =>
          ZIO.fromEither(
            ProductMigration2025N4Migration.postAmendmentStructureIntegrityCheck(
              subscriptionBeforeUpdate,
              subscriptionAfterUpdate,
              today
            )
          )
        case _ => ZIO.succeed(())
      }).mapError(message => AmendmentFailure(message))

      whenDone <- Clock.instant
    } yield AARSuccessfulAmendment(
      item.subscriptionName,
      amendmentEffectiveDate,
      newPrice,
      subscriptionAfterUpdate.id,
      whenDone
    )
  }

  private def performAmendmentAttemptWithResult(
      cohortSpec: CohortSpec,
      item: CohortItem
  ): ZIO[Zuora with Logging with SalesforceClient, Failure, AmendmentAttemptResult] = {
    MigrationType(cohortSpec) match {
      case Test1              => ZIO.fail(ConfigFailure("Branch not supported"))
      case GuardianWeekly2025 =>
        doAmendmentUsingOrdersApiWithJsonValues(
          cohortSpec: CohortSpec,
          item: CohortItem
        )
      case Newspaper2025P1 =>
        doAmendmentUsingOrdersApiWithJsonValues(
          cohortSpec: CohortSpec,
          item: CohortItem
        )
      case Newspaper2025P3 =>
        doAmendmentUsingOrdersApiWithJsonValues(
          cohortSpec: CohortSpec,
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
              doAmendmentUsingOrdersApiWithJsonValues(
                cohortSpec,
                item
              )
        } yield result
      }

      case Membership2025 =>
        doAmendmentUsingOrdersApiWithJsonValues(
          cohortSpec: CohortSpec,
          item: CohortItem
        )
      case DigiSubs2025 =>
        doAmendmentUsingOrdersApiWithJsonValues(
          cohortSpec: CohortSpec,
          item: CohortItem
        )
      case SupporterPlus2026 =>
        doAmendmentUsingOrdersApiWithJsonValues(
          cohortSpec: CohortSpec,
          item: CohortItem
        )
      case SupporterPlus2026N2 =>
        doAmendmentUsingOrdersApiWithJsonValues(
          cohortSpec: CohortSpec,
          item: CohortItem
        )
      case SupporterPlus2026N3 =>
        doAmendmentUsingOrdersApiWithJsonValues(
          cohortSpec: CohortSpec,
          item: CohortItem
        )
      case SupporterPlus2026N4 =>
        doAmendmentUsingOrdersApiWithJsonValues(
          cohortSpec: CohortSpec,
          item: CohortItem
        )
      case SupporterPlus2026N5 =>
        doAmendmentUsingOrdersApiWithJsonValues(
          cohortSpec: CohortSpec,
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
