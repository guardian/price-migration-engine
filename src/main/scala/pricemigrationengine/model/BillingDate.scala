package pricemigrationengine.model

import java.time.temporal.{ChronoUnit, TemporalAdjusters}
import java.time.{DayOfWeek, LocalDate}

import scala.annotation.tailrec

/*
 * This is a contained chaos, derived from code in support-service-lambdas.
 * It will become cleaner as more test cases build up.
 */
object BillingDate {

  def nextBillingDate(
      subscription: ZuoraSubscription,
      account: ZuoraAccount,
      after: LocalDate,
      currentDate: LocalDate
  ): Either[AmendmentDataFailure, LocalDate] =
    for {
      subscriptionData <- SubscriptionData.apply(subscription, account, currentDate)
      issueData <- subscriptionData.issueDataForDate(after)
    } yield issueData.nextBillingPeriodStartDate

  private object ZuoraProductTypes {
    sealed case class ZuoraProductType(name: String)

    object NewspaperVoucherBook extends ZuoraProductType("Newspaper - Voucher Book")
  }

  private case class SupportedRatePlan(name: String, ratePlanCharges: List[SupportedRatePlanCharge])

  private case class SupportedRatePlanCharge(name: String, dayOfWeek: DayOfWeek)

  private object RatePlanChargeData {
    def apply(
        subscription: ZuoraSubscription,
        ratePlanCharge: ZuoraRatePlanCharge,
        account: ZuoraAccount,
        issueDayOfWeek: DayOfWeek
    ): Either[AmendmentDataFailure, RatePlanChargeData] =
      for {
        billingPeriodName <- ratePlanCharge.billingPeriod
          .toRight(AmendmentDataFailure("RatePlanCharge.billingPeriod is required"))
        schedule <- RatePlanChargeBillingSchedule(subscription, ratePlanCharge, account)
      } yield RatePlanChargeData(ratePlanCharge, schedule, billingPeriodName, issueDayOfWeek)
  }

  private case class SupportedProduct(
      name: String,
      productType: ZuoraProductTypes.ZuoraProductType,
      annualIssueLimitPerEdition: Int,
      ratePlans: List[SupportedRatePlan]
  )

  private object SupportedProduct {
    lazy val supportedProducts = List(
      SupportedProduct(
        name = "Newspaper Voucher",
        productType = ZuoraProductTypes.NewspaperVoucherBook,
        annualIssueLimitPerEdition = 10,
        ratePlans = List(
          SupportedRatePlan("Everyday", everyDayCharges),
          SupportedRatePlan("Everyday+", everyDayCharges),
          SupportedRatePlan("Saturday", saturdayCharges),
          SupportedRatePlan("Saturday+", saturdayCharges),
          SupportedRatePlan("Sixday", sixDayCharges),
          SupportedRatePlan("Sixday+", sixDayCharges),
          SupportedRatePlan("Sunday", sundayCharges),
          SupportedRatePlan("Sunday+", sundayCharges),
          SupportedRatePlan("Weekend", weekendCharges),
          SupportedRatePlan("Weekend+", weekendCharges)
        )
      )
    )

    private lazy val everyDayCharges = List(
      SupportedRatePlanCharge("Monday", DayOfWeek.MONDAY),
      SupportedRatePlanCharge("Tuesday", DayOfWeek.TUESDAY),
      SupportedRatePlanCharge("Wednesday", DayOfWeek.WEDNESDAY),
      SupportedRatePlanCharge("Thursday", DayOfWeek.THURSDAY),
      SupportedRatePlanCharge("Friday", DayOfWeek.FRIDAY),
      SupportedRatePlanCharge("Saturday", DayOfWeek.SATURDAY),
      SupportedRatePlanCharge("Sunday", DayOfWeek.SUNDAY)
    )

    private lazy val saturdayCharges = List(
      SupportedRatePlanCharge("Saturday", DayOfWeek.SATURDAY)
    )

    private lazy val sixDayCharges = List(
      SupportedRatePlanCharge("Monday", DayOfWeek.MONDAY),
      SupportedRatePlanCharge("Tuesday", DayOfWeek.TUESDAY),
      SupportedRatePlanCharge("Wednesday", DayOfWeek.WEDNESDAY),
      SupportedRatePlanCharge("Thursday", DayOfWeek.THURSDAY),
      SupportedRatePlanCharge("Friday", DayOfWeek.FRIDAY),
      SupportedRatePlanCharge("Saturday", DayOfWeek.SATURDAY)
    )

    private lazy val weekendCharges = List(
      SupportedRatePlanCharge("Saturday", DayOfWeek.SATURDAY),
      SupportedRatePlanCharge("Sunday", DayOfWeek.SUNDAY)
    )

    private lazy val sundayCharges = List(SupportedRatePlanCharge("Sunday", DayOfWeek.SUNDAY))
  }

  private trait SubscriptionData {
    def issueDataForDate(issueDate: LocalDate): Either[AmendmentDataFailure, IssueData]
    def issueDataForPeriod(startDateInclusive: LocalDate, endDateInclusive: LocalDate): List[IssueData]
    def productType: ZuoraProductTypes.ZuoraProductType
    def editionDaysOfWeek: List[DayOfWeek]
  }

  private def getSupportedProductForRatePlan(ratePlan: ZuoraRatePlan) =
    SupportedProduct.supportedProducts.find(_.name == ratePlan.productName)

  private def getSupportedRatePlanForRatePlan(ratePlan: ZuoraRatePlan, supportedProduct: SupportedProduct) =
    supportedProduct.ratePlans.find(_.name == ratePlan.ratePlanName)

  private def getUnexpiredRatePlanCharges(ratePlan: ZuoraRatePlan, currentDate: LocalDate) =
    ratePlan.ratePlanCharges.filter(_.chargedThroughDate.forall(!_.isBefore(currentDate)))

  private def getSupportedRatePlanCharge(
      supportedRatePlan: SupportedRatePlan,
      unExpiredRatePlanCharge: ZuoraRatePlanCharge
  ) =
    supportedRatePlan.ratePlanCharges.find(_.name == unExpiredRatePlanCharge.name)

  private def getZuoraProductType(
      supportedProducts: List[SupportedProduct]
  ): Either[AmendmentDataFailure, ZuoraProductTypes.ZuoraProductType] =
    supportedProducts
      .map(_.productType)
      .distinct match {
      case Nil               => Left(AmendmentDataFailure("Could not derive product type as there are no supported rateplan charges"))
      case List(productType) => Right(productType)
      case moreThanOne =>
        Left(
          AmendmentDataFailure(
            s"Could not derive product type as they are rate plan charges from more than one product type $moreThanOne"
          )
        )
    }

  private def ratePlanChargeDataForDate(
      ratePlanChargeData: List[RatePlanChargeData],
      date: LocalDate
  ): Either[AmendmentDataFailure, RatePlanChargeData] =
    ratePlanChargeData
      .find { ratePlanCharge =>
        ratePlanCharge.billingSchedule.isDateCoveredBySchedule(date) &&
        ratePlanCharge.issueDayOfWeek == date.getDayOfWeek
      }
      .toRight(
        AmendmentDataFailure(s"Subscription does not have a rate plan for date $date")
      )

  private def createSubscriptionData(
      nonZeroRatePlanChargeDatas: List[RatePlanChargeData],
      zuoraProductType: ZuoraProductTypes.ZuoraProductType
  ): SubscriptionData =
    new SubscriptionData {
      def issueDataForDate(issueDate: LocalDate): Either[AmendmentDataFailure, IssueData] = {
        for {
          ratePlanChargeData <- ratePlanChargeDataForDate(nonZeroRatePlanChargeDatas, issueDate)
          billingPeriod <- ratePlanChargeData.billingSchedule.billDatesCoveringDate(issueDate)
        } yield {
          applyAnyDiscounts(IssueData(issueDate, billingPeriod))
        }
      }

      def applyAnyDiscounts(issueData: IssueData): IssueData = {
        issueData
      }

      def issueDataForPeriod(startDateInclusive: LocalDate, endDateInclusive: LocalDate): List[IssueData] = {
        nonZeroRatePlanChargeDatas
          .flatMap(_.getIssuesForPeriod(startDateInclusive, endDateInclusive))
          .map(applyAnyDiscounts)
          .sortBy(_.issueDate)(Ordering.fromLessThan(_.isBefore(_)))
      }

      override def productType: ZuoraProductTypes.ZuoraProductType = {
        zuoraProductType
      }

      override def editionDaysOfWeek: List[DayOfWeek] =
        nonZeroRatePlanChargeDatas.map(_.issueDayOfWeek).distinct
    }

  private trait RatePlanChargeBillingSchedule {
    def billDatesCoveringDate(date: LocalDate): Either[AmendmentDataFailure, BillDates]

    def isDateCoveredBySchedule(date: LocalDate): Boolean
  }

  private object RatePlanChargeBillingSchedule {
    def apply(
        subscription: ZuoraSubscription,
        ratePlanCharge: ZuoraRatePlanCharge,
        account: ZuoraAccount
    ): Either[AmendmentDataFailure, RatePlanChargeBillingSchedule] = {
      apply(
        subscription.customerAcceptanceDate,
        subscription.contractEffectiveDate,
        ratePlanCharge.billingDay,
        ratePlanCharge.triggerEvent,
        ratePlanCharge.triggerDate,
        ratePlanCharge.processedThroughDate,
        account.billingAndPayment.billCycleDay,
        ratePlanCharge.upToPeriodsType,
        ratePlanCharge.upToPeriods,
        ratePlanCharge.billingPeriod,
        ratePlanCharge.specificBillingPeriod,
        ratePlanCharge.endDateCondition,
        ratePlanCharge.effectiveStartDate
      )
    }

    private def apply(
        customerAcceptanceDate: LocalDate,
        contractEffectiveDate: LocalDate,
        billingDay: Option[String],
        triggerEvent: Option[String],
        triggerDate: Option[LocalDate],
        processedThroughDate: Option[LocalDate],
        billCycleDay: Int,
        upToPeriodType: Option[String],
        upToPeriods: Option[Int],
        optionalBillingPeriodName: Option[String],
        specificBillingPeriod: Option[Int],
        endDateCondition: Option[String],
        effectiveStartDate: LocalDate
    ): Either[AmendmentDataFailure, RatePlanChargeBillingSchedule] = {
      for {
        endDateCondition <- endDateCondition.toRight(
          AmendmentDataFailure("RatePlanCharge.endDateCondition is required")
        )
        billingPeriodName <- optionalBillingPeriodName.toRight(
          AmendmentDataFailure("RatePlanCharge.billingPeriod is required")
        )
        billingPeriod <- billingPeriodForName(billingPeriodName, specificBillingPeriod)

        calculatedRatePlanStartDate <- ratePlanStartDate(
          customerAcceptanceDate,
          contractEffectiveDate,
          billingDay,
          triggerEvent,
          triggerDate,
          billCycleDay
        )

        calculatedRatePlanEndDate <- ratePlanEndDate(
          billingPeriod,
          calculatedRatePlanStartDate,
          endDateCondition,
          upToPeriodType,
          upToPeriods
        )

        scheduleForCalculatedStartDate = RatePlanChargeBillingSchedule(
          calculatedRatePlanStartDate,
          calculatedRatePlanEndDate,
          billingPeriod
        )

        endDateBasedOnEffectiveStartDate <- ratePlanEndDate(
          billingPeriod,
          effectiveStartDate,
          endDateCondition,
          upToPeriodType,
          upToPeriods
        )

        scheduleForEffectiveStartDate = RatePlanChargeBillingSchedule(
          effectiveStartDate,
          endDateBasedOnEffectiveStartDate,
          billingPeriod
        )

        billingSchedule <- selectScheduleThatPredictsProcessedThroughDate(
          List(
            scheduleForCalculatedStartDate,
            scheduleForEffectiveStartDate
          ),
          processedThroughDate
        )
      } yield billingSchedule
    }

    private def apply(
        ratePlanStartDate: LocalDate,
        ratePlanEndDate: Option[LocalDate],
        billingPeriod: BillingPeriod
    ): RatePlanChargeBillingSchedule =
      new RatePlanChargeBillingSchedule {
        override def isDateCoveredBySchedule(date: LocalDate): Boolean = {
          (date == ratePlanStartDate || date.isAfter(ratePlanStartDate)) &&
          ratePlanEndDate.forall(endDate => date == endDate || date.isBefore(endDate))
        }

        override def billDatesCoveringDate(date: LocalDate): Either[AmendmentDataFailure, BillDates] = {
          if (isDateCoveredBySchedule(date)) {
            billDatesCoveringDate(date, ratePlanStartDate, 0)
          } else {
            Left(AmendmentDataFailure(s"Billing schedule does not cover date $date"))
          }
        }

        @tailrec
        private def billDatesCoveringDate(
            date: LocalDate,
            startDate: LocalDate,
            billingPeriodIndex: Int
        ): Either[AmendmentDataFailure, BillDates] = {
          val currentPeriod = BillDates(
            startDate.plus(billingPeriod.multiple.toLong * billingPeriodIndex, billingPeriod.unit),
            startDate.plus(billingPeriod.multiple.toLong * (billingPeriodIndex + 1), billingPeriod.unit).minusDays(1)
          )
          if (currentPeriod.startDate.isAfter(date)) {
            Left(AmendmentDataFailure(s"Billing schedule does not cover date $date"))
          } else if (!currentPeriod.endDate.isBefore(date)) {
            Right(currentPeriod)
          } else {
            billDatesCoveringDate(date, startDate, billingPeriodIndex + 1)
          }
        }
      }

    private def selectScheduleThatPredictsProcessedThroughDate(
        schedules: List[RatePlanChargeBillingSchedule],
        optionalProcessedThroughDate: Option[LocalDate]
    ): Either[AmendmentDataFailure, RatePlanChargeBillingSchedule] =
      optionalProcessedThroughDate match {
        case Some(processedThroughDate) =>
          schedules
            .find { schedule =>
              val processThoughDateIsAtStartOfBillingSchedule = schedule
                .billDatesCoveringDate(processedThroughDate)
                .map(_.startDate == processedThroughDate)
                .getOrElse(false)
              val dayBeforeProcessedThroughDate = processedThroughDate.minusDays(1)
              val processThoughDateIsJustAfterEndOfBillingSchedule = schedule
                .billDatesCoveringDate(dayBeforeProcessedThroughDate)
                .map(_.endDate == dayBeforeProcessedThroughDate)
                .getOrElse(false)
              processThoughDateIsAtStartOfBillingSchedule || processThoughDateIsJustAfterEndOfBillingSchedule
            }
            .toRight(
              AmendmentDataFailure(
                s"Could not create schedule that correctly predicts processed through date $processedThroughDate"
              )
            )
        case None =>
          Right(schedules.head)
      }

    private def billingPeriodForName(
        billingPeriodName: String,
        optionalSpecificBillingPeriod: Option[Int]
    ): Either[AmendmentDataFailure, BillingPeriod] = {
      billingPeriodName match {
        case "Annual"                      => Right(BillingPeriod(ChronoUnit.YEARS, 1))
        case "Semi_Annual" | "Semi-Annual" => Right(BillingPeriod(ChronoUnit.MONTHS, 6))
        case "Quarter"                     => Right(BillingPeriod(ChronoUnit.MONTHS, 3))
        case "Month"                       => Right(BillingPeriod(ChronoUnit.MONTHS, 1))
        case "Specific_Weeks" | "Specific Weeks" =>
          optionalSpecificBillingPeriod
            .toRight(AmendmentDataFailure(s"specificBillingPeriod is required for $billingPeriodName billing period"))
            .map(BillingPeriod(ChronoUnit.WEEKS, _))
        case "Specific_Months" | "Specific Months" =>
          optionalSpecificBillingPeriod
            .toRight(AmendmentDataFailure(s"specificBillingPeriod is required for $billingPeriodName billing period"))
            .map(BillingPeriod(ChronoUnit.MONTHS, _))
        case _ => Left(AmendmentDataFailure(s"Failed to determine duration of billing period: $billingPeriodName"))
      }
    }

    private def ratePlanStartDate(
        customerAcceptanceDate: LocalDate,
        contractEffectiveDate: LocalDate,
        optionalBillingDay: Option[String],
        optionalTriggerEvent: Option[String],
        optionalTriggerDate: Option[LocalDate],
        billCycleDay: Int
    ): Either[AmendmentDataFailure, LocalDate] = {
      optionalBillingDay match {
        case None | Some("ChargeTriggerDay") =>
          ratePlanTriggerDate(
            optionalTriggerEvent,
            optionalTriggerDate,
            customerAcceptanceDate,
            contractEffectiveDate
          )
        case Some("DefaultFromCustomer") =>
          for {
            triggerDate <- ratePlanTriggerDate(
              optionalTriggerEvent,
              optionalTriggerDate,
              customerAcceptanceDate,
              contractEffectiveDate
            )
          } yield adjustDateForBillCycleDate(triggerDate, billCycleDay)
        case Some(unsupported) =>
          Left(AmendmentDataFailure(s"RatePlanCharge.billingDay = $unsupported is not supported"))
      }
    }

    private def ratePlanEndDate(
        billingPeriod: BillingPeriod,
        ratePlanStartDate: LocalDate,
        endDateCondition: String,
        upToPeriodsType: Option[String],
        upToPeriods: Option[Int]
    ): Either[AmendmentDataFailure, Option[LocalDate]] = {
      endDateCondition match {
        case "Subscription_End" | "SubscriptionEnd" => Right(None) //This assumes all subscriptions will renew for ever
        case "Fixed_Period" | "FixedPeriod" =>
          ratePlanFixedPeriodEndDate(
            billingPeriod,
            ratePlanStartDate,
            upToPeriodsType,
            upToPeriods
          ).map(endDate => Some(endDate))
        case unsupported =>
          Left(AmendmentDataFailure(s"RatePlanCharge.endDateCondition=$unsupported is not supported"))
      }
    }

    private def adjustDateForBillCycleDate(date: LocalDate, billCycleDay: Int): LocalDate = {
      val dateWithCorrectMonth = if (date.getDayOfMonth < billCycleDay) {
        date.plusMonths(1)
      } else {
        date
      }

      val lastDateOfMonth = dateWithCorrectMonth `with` TemporalAdjusters.lastDayOfMonth()

      dateWithCorrectMonth.withDayOfMonth(Math.min(lastDateOfMonth.getDayOfMonth, billCycleDay))
    }

    private def ratePlanTriggerDate(
        optionalTriggerEvent: Option[String],
        optionalTriggerDate: Option[LocalDate],
        customerAcceptanceDate: LocalDate,
        contractEffectiveDate: LocalDate
    ): Either[AmendmentDataFailure, LocalDate] =
      optionalTriggerEvent match {
        case Some("CustomerAcceptance") => Right(customerAcceptanceDate)
        case Some("ContractEffective")  => Right(contractEffectiveDate)
        case Some("SpecificDate") =>
          optionalTriggerDate
            .toRight(AmendmentDataFailure("RatePlan.triggerDate is required when RatePlan.triggerEvent=SpecificDate"))
        case Some(unsupported) =>
          Left(AmendmentDataFailure(s"RatePlan.triggerEvent=$unsupported is not supported"))
        case None =>
          Left(AmendmentDataFailure("RatePlan.triggerEvent is a required field"))
      }

    private def ratePlanFixedPeriodEndDate(
        billingPeriod: BillingPeriod,
        ratePlanStartDate: LocalDate,
        optionalUpToPeriodsType: Option[String],
        optionalUpToPeriods: Option[Int]
    ) =
      optionalUpToPeriodsType match {
        case Some("Billing_Periods") | Some("Billing Periods") =>
          optionalUpToPeriods
            .toRight(
              AmendmentDataFailure("RatePlan.upToPeriods is required when RatePlan.upToPeriodsType=Billing_Periods")
            )
            .map { upToPeriods =>
              billingPeriod.unit
                .addTo(ratePlanStartDate, (upToPeriods * billingPeriod.multiple).toLong)
                .minusDays(1)
            }
        case unsupportedBillingPeriodType =>
          Left(
            AmendmentDataFailure(
              s"RatePlan.upToPeriodsType=${unsupportedBillingPeriodType.getOrElse("null")} is not supported"
            )
          )
      }

    case class BillingPeriod(unit: ChronoUnit, multiple: Int)
  }

  private case class RatePlanChargeData(
      ratePlanCharge: ZuoraRatePlanCharge,
      billingSchedule: RatePlanChargeBillingSchedule,
      billingPeriodName: String,
      issueDayOfWeek: DayOfWeek
  ) {
    def getIssuesForPeriod(startDateInclusive: LocalDate, endDateInclusive: LocalDate): List[IssueData] = {
      @tailrec
      def getIssuesForPeriod(
          firstIssueDate: LocalDate,
          endDateInclusive: LocalDate,
          issueData: List[IssueData]
      ): List[IssueData] = {
        if (firstIssueDate.isAfter(endDateInclusive)) {
          issueData
        } else {
          getIssuesForPeriod(
            firstIssueDate.`with`(TemporalAdjusters.next(issueDayOfWeek)),
            endDateInclusive,
            billingSchedule.billDatesCoveringDate(firstIssueDate) match {
              case Left(_)              => issueData
              case Right(billingPeriod) => IssueData(firstIssueDate, billingPeriod) :: issueData
            }
          )
        }
      }

      getIssuesForPeriod(
        startDateInclusive.`with`(TemporalAdjusters.nextOrSame(issueDayOfWeek)),
        endDateInclusive,
        Nil
      )
    }
  }

  private case class BillDates(startDate: LocalDate, endDate: LocalDate)

  private case class IssueData(issueDate: LocalDate, billDates: BillDates) {
    def nextBillingPeriodStartDate: LocalDate = {
      billDates.endDate.plusDays(1)
    }
  }

  private object SubscriptionData {

    def apply(
        subscription: ZuoraSubscription,
        account: ZuoraAccount,
        currentDate: LocalDate
    ): Either[AmendmentDataFailure, SubscriptionData] = {
      val supportedRatePlanCharges: List[(ZuoraRatePlanCharge, SupportedRatePlanCharge, SupportedProduct)] = for {
        ratePlan <- subscription.ratePlans if !ratePlan.lastChangeType.contains("Remove")
        supportedProduct <- getSupportedProductForRatePlan(ratePlan).toList
        supportedRatePlan <- getSupportedRatePlanForRatePlan(ratePlan, supportedProduct).toList
        unExpiredRatePlanCharge <- getUnexpiredRatePlanCharges(ratePlan, currentDate)
        supportedRatePlanCharge <- getSupportedRatePlanCharge(supportedRatePlan, unExpiredRatePlanCharge)
      } yield (unExpiredRatePlanCharge, supportedRatePlanCharge, supportedProduct)

      val ratePlanChargeDatas: List[RatePlanChargeData] = {
        supportedRatePlanCharges
          .map {
            case (ratePlanCharge, supportedRatePlanCharge, _) =>
              RatePlanChargeData(
                subscription,
                ratePlanCharge,
                account,
                supportedRatePlanCharge.dayOfWeek
              )
          }
          .collect { case Right(data) => data }
      }
      for {
        productType <- getZuoraProductType(supportedRatePlanCharges.map(_._3))
      } yield createSubscriptionData(ratePlanChargeDatas, productType)
    }
  }
}
