package pricemigrationengine.handlers

import pricemigrationengine.migrations.newspaper2024Migration
import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.{Clock, IO, Random, ZIO}
import pricemigrationengine.util.Date
import java.time.LocalDate

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
        val cohortItemToWrite = MigrationType(cohortSpec) match {
          case SupporterPlus2023V1V2MA => {
            // SupporterPlus2023V1V2 is different here, because it's a rate plan migration and not a price increase
            // The first of its kind. In particular we do not want processing stage `NoPriceIncrease`
            CohortItem.fromSuccessfulEstimationResult(result)
          }
          case _ => {
            if (result.estimatedNewPrice <= result.oldPrice) CohortItem.fromNoPriceIncreaseEstimationResult(result)
            else CohortItem.fromSuccessfulEstimationResult(result)
          }
        }

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

  def startDateGeneralLowerBound(
      cohortSpec: CohortSpec,
      today: LocalDate
  ): LocalDate = {
    // The startDateGeneralLowerBound is a function of the cohort spec and the notification min time.
    // The cohort spec carries the lowest date we specify there can be a price migration, and the notification min
    // time ensures the legally required lead time for customer communication. The max of those two dates is the date
    // from which we can realistically perform a price increase. With that said, other policies can apply, for
    // instance:
    // - The one year policy, which demand that we do not price rise customers during the subscription first year
    // - The spread: a mechanism, used for monthlies, by which we do not let a large number of monthlies migrate
    //   during a single month.

    Date.datesMax(
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
    Date.datesMax(lowerbound, subscription.customerAcceptanceDate.plusMonths(12))
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

  // In legacy print product cases, we have spread the price rises over 3 months for monthly subscriptions, this is
  // the default behaviour. For annual subscriptions we are not applying any spread and defaulting to value 1.
  def decideSpreadPeriod(
      subscription: ZuoraSubscription,
      invoicePreview: ZuoraInvoiceList,
      cohortSpec: CohortSpec
  ): Int = {
    if (isMonthlySubscription(subscription, invoicePreview)) {
      MigrationType(cohortSpec) match {
        case Membership2023Monthlies => 1
        case Membership2023Annuals   => 1
        case Newspaper2024           => newspaper2024Migration.Estimation.startDateSpreadPeriod(subscription)
        case _                       => 3
      }
    } else 1
  }

  def decideStartDateLowerboundWithRandomAddition(
      subscription: ZuoraSubscription,
      invoicePreview: ZuoraInvoiceList,
      cohortSpec: CohortSpec,
      today: LocalDate
  ): IO[ConfigFailure, LocalDate] = {

    // We start by deciding the start date general lower bound, which is determined by the cohort's
    // earliestPriceMigrationStartDate and the notification period to this migration. See comment in
    // the body of startDateGeneralLowerbound for details.

    // Note that for Newspaper2024 we use that migration own version of this function (due to the unusual scheduling
    // nature of that migration), which also takes the subscription.

    val startDateLowerBound1 = MigrationType(cohortSpec) match {
      case Newspaper2024 =>
        newspaper2024Migration.Estimation.startDateGeneralLowerbound(cohortSpec, today, subscription)
      case _ => startDateGeneralLowerBound(cohortSpec, today)
    }

    // We now respect the policy of not increasing members during their first year
    val startDateLowerBound2 = oneYearPolicy(startDateLowerBound1, subscription)

    // Looking up the spread period for this migration
    val spreadPeriod = decideSpreadPeriod(subscription, invoicePreview, cohortSpec)

    for {
      randomFactor <- Random.nextIntBetween(0, spreadPeriod)
    } yield startDateLowerBound2.plusMonths(randomFactor)
  }

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] = {
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
