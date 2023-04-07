package pricemigrationengine.handlers

import pricemigrationengine.handlers.NotificationHandler.minLeadTime
import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model.{CohortSpec, _}
import pricemigrationengine.services._
import zio.{Clock, IO, Random, UIO, ZIO}

import java.time.LocalDate
import java.time.temporal._

/** Calculates start date and new price for a set of CohortItems.
  *
  * Expected input is a CohortSpec in json format.
  *
  * Output is a HandlerOutput in json format.
  */
object EstimationHandler extends CohortHandler {

  // TODO: move to config
  private val batchSize = 150

  def main(cohortSpec: CohortSpec): ZIO[Logging with CohortTable with Zuora, Failure, HandlerOutput] =
    for {
      today <- Clock.currentDateTime.map(_.toLocalDate)
      catalogue <- Zuora.fetchProductCatalogue
      count <- CohortTable
        .fetch(ReadyForEstimation, None)
        .take(batchSize)
        .mapZIO(item =>
          estimate(catalogue, cohortSpec)(today, item).tapBoth(Logging.logFailure(item), Logging.logSuccess(item))
        )
        .runCount
        .tapError(e => Logging.error(e.toString))
    } yield HandlerOutput(isComplete = count < batchSize)

  private[handlers] def estimate(
      catalogue: ZuoraProductCatalogue,
      cohortSpec: CohortSpec
  )(
      today: LocalDate,
      item: CohortItem,
  ): ZIO[CohortTable with Zuora, Failure, EstimationResult] =
    doEstimation(catalogue, item, cohortSpec, today).foldZIO(
      failure = {
        case failure: AmendmentDataFailure =>
          val result = FailedEstimationResult(item.subscriptionName, failure.reason)
          CohortTable.update(CohortItem.fromFailedEstimationResult(result)).as(result)
        case _: CancelledSubscriptionFailure =>
          val result = CancelledEstimationResult(item.subscriptionName)
          CohortTable.update(CohortItem.fromCancelledEstimationResult(result)).as(result)
        case e => ZIO.fail(e)
      },
      success = { result =>
        val cohortItemToWrite =
          if (result.estimatedNewPrice <= result.oldPrice) CohortItem.fromNoPriceIncreaseEstimationResult(result)
          else CohortItem.fromSuccessfulEstimationResult(result)
        for {
          cohortItem <- cohortItemToWrite
          _ <- CohortTable.update(cohortItem)
        } yield result
      }
    )

  private def doEstimation(
      catalogue: ZuoraProductCatalogue,
      item: CohortItem,
      cohortSpec: CohortSpec,
      today: LocalDate,
  ): ZIO[Zuora, Failure, SuccessfulEstimationResult] = {
    for {
      subscription <-
        Zuora
          .fetchSubscription(item.subscriptionName)
          .filterOrFail(_.status != "Cancelled")(CancelledSubscriptionFailure(item.subscriptionName))
      account <- Zuora.fetchAccount(subscription.accountNumber, subscription.subscriptionNumber)
      invoicePreviewTargetDate = cohortSpec.earliestPriceMigrationStartDate.plusMonths(16)
      invoicePreview <- Zuora.fetchInvoicePreview(subscription.accountId, invoicePreviewTargetDate)
      startDateLowerBound <- decideStartDateLowerboundWithRandomAddition(
        subscription,
        invoicePreview,
        cohortSpec,
        today
      )
      result <- ZIO.fromEither(
        EstimationResult(account, catalogue, subscription, invoicePreview, startDateLowerBound, cohortSpec)
      )
    } yield result
  }

  def datesMax(date1: LocalDate, date2: LocalDate): LocalDate = if (date1.isBefore(date2)) date2 else date1

  def startDateGeneralLowerbound(cohortSpec: CohortSpec, today: LocalDate): LocalDate = {
    datesMax(
      cohortSpec.earliestPriceMigrationStartDate,
      today.plusDays(
        NotificationHandler.minLeadTime(cohortSpec: CohortSpec) + 1
      ) // +1 because we need to be strictly over minLeadTime days away. Exactly minLeadTime is not enough.
    )
  }

  // This function takes a date and a subscription and returns the highest between that date
  // and the customer's customerAcceptanceDate. Doing so we implement the policy of not increasing customers during
  // their first year.
  def oneYearPolicy(lowerbound: LocalDate, subscription: ZuoraSubscription): LocalDate = {
    datesMax(lowerbound, subscription.customerAcceptanceDate.plusMonths(12))
  }

  // Determines whether the subscription is a monthly subscription
  // To do so, we simply check whether the billing period of a rate plan charge contains "Month"
  def isMonthlySubscription(subscription: ZuoraSubscription, invoicePreview: ZuoraInvoiceList): Boolean = {
    invoicePreview.invoiceItems
      .flatMap(invoiceItem => ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toOption)
      .flatMap(_.billingPeriod)
      .headOption
      .contains("Month")
  }

  // In legacy print product cases, we have spread the price rises over 3 months for monthly subscriptions, but
  // in the case of membership we want to do this over a single month, hence a value of 1. For annual subscriptions
  // we do not need to apply a spread
  def decideSpreadPeriod(
      subscription: ZuoraSubscription,
      invoicePreview: ZuoraInvoiceList,
      cohortSpec: CohortSpec
  ): Int = {
    if (isMonthlySubscription(subscription, invoicePreview))
      if (CohortSpec.isMembershipPriceRiseMonthlies(cohortSpec)) 1 else 3
    else 1
  }

  def decideStartDateLowerboundWithRandomAddition(
      subscription: ZuoraSubscription,
      invoicePreview: ZuoraInvoiceList,
      cohortSpec: CohortSpec,
      today: LocalDate
  ): IO[ConfigFailure, LocalDate] = {
    // We start by deciding the start date general lower bound, which is determined by the cohort's
    // earliestPriceMigrationStartDate and the notification period to this migration
    val startDateLowerBound1 = startDateGeneralLowerbound(cohortSpec: CohortSpec, today: LocalDate)

    // We now respect the policy of not increasing members during their first year
    val startDateLowerBound2 = oneYearPolicy(startDateLowerBound1, subscription)

    val spreadPeriod = decideSpreadPeriod(subscription, invoicePreview, cohortSpec)
    for {
      randomFactor <- Random.nextIntBetween(0, spreadPeriod)
    } yield startDateLowerBound2.plusMonths(randomFactor)
  }

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] =
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
