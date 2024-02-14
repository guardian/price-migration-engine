package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model._
import pricemigrationengine.model.membershipworkflow._
import pricemigrationengine.services._
import zio.{Clock, ZIO}
import com.gu.i18n
import pricemigrationengine.migrations.{DigiSubs2023Migration, Membership2023Migration, newspaper2024Migration}
import pricemigrationengine.model.RateplansProbe

import java.time.{LocalDate, ZoneId}
import java.time.format.DateTimeFormatter

object NotificationHandler extends CohortHandler {

  val Successful = 1
  val Unsuccessful = 0
  val Cancelled_Status = "Cancelled"

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] = {
    main(input).provideSome[Logging](
      EnvConfig.salesforce.layer,
      EnvConfig.cohortTable.layer,
      EnvConfig.emailSender.layer,
      EnvConfig.zuora.layer,
      EnvConfig.stage.layer,
      DynamoDBClientLive.impl,
      DynamoDBZIOLive.impl,
      CohortTableLive.impl(input),
      SalesforceClientLive.impl,
      EmailSenderLive.impl,
      ZuoraLive.impl
    )
  }

  def main(
      cohortSpec: CohortSpec
  ): ZIO[Logging with CohortTable with SalesforceClient with EmailSender with Zuora, Failure, HandlerOutput] = {
    for {
      today <- Clock.currentDateTime.map(_.toLocalDate)
      count <- CohortTable
        .fetch(SalesforcePriceRiceCreationComplete, Some(today.plusDays(maxLeadTime(cohortSpec))))
        .mapZIO(item => sendNotification(cohortSpec)(item, today))
        .runFold(0) { (sum, count) => sum + count }
      _ <- Logging.info(s"Successfully sent $count price rise notifications")
    } yield HandlerOutput(isComplete = true)
  }

  def sendNotification(cohortSpec: CohortSpec)(
      cohortItem: CohortItem,
      today: LocalDate
  ): ZIO[EmailSender with SalesforceClient with CohortTable with Logging with Zuora, Failure, Int] = {

    // We are starting with a simple check. That the item's startDate is at least minLeadTime(cohortSpec) days away
    // from the current day. This will avoid headaches caused by letters not being sent early enough relatively to
    // previously computed start dates, which can happen if, for argument sake, the engine is down for a few days.

    if (thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem)) {
      val result = for {
        _ <- cohortItemRatePlansChecks(cohortItem)
        sfSubscription <-
          SalesforceClient
            .getSubscriptionByName(cohortItem.subscriptionName)
        count <-
          if (sfSubscription.Status__c != Cancelled_Status) {
            sendNotification(cohortSpec, cohortItem, sfSubscription)
          } else {
            putSubIntoCancelledStatus(cohortItem.subscriptionName)
          }
      } yield count
      result.catchAll { failure =>
        for {
          _ <- Logging.error(
            s"Subscription ${cohortItem.subscriptionName}: Failed to send price rise notification: $failure"
          )
        } yield Unsuccessful
      }
    } else {
      ZIO.fail(
        NotificationNotEnoughLeadTimeFailure(
          s"[notification] The start date of item ${cohortItem.subscriptionName} (startDate: ${cohortItem.startDate}) is too close to today ${today}"
        )
      )
    }
  }

  def sendNotification(
      cohortSpec: CohortSpec,
      cohortItem: CohortItem,
      sfSubscription: SalesforceSubscription
  ): ZIO[EmailSender with SalesforceClient with CohortTable with Logging, Failure, Int] =
    for {
      _ <- Logging.info(s"Processing subscription: ${cohortItem.subscriptionName}")
      contact <- SalesforceClient.getContact(sfSubscription.Buyer__c)
      firstName <- ZIO.fromEither(firstName(contact))
      lastName <- ZIO.fromEither(requiredField(contact.LastName, "Contact.LastName"))
      address <- ZIO.fromEither(address(cohortSpec, contact))
      street <- ZIO.fromEither(street(cohortSpec, address: SalesforceAddress))
      postalCode = address.postalCode.getOrElse("")
      country <- ZIO.fromEither(country(cohortSpec, address))
      oldPrice <- ZIO.fromEither(requiredField(cohortItem.oldPrice, "CohortItem.oldPrice"))
      estimatedNewPrice <- ZIO.fromEither(requiredField(cohortItem.estimatedNewPrice, "CohortItem.estimatedNewPrice"))
      startDate <- ZIO.fromEither(requiredField(cohortItem.startDate.map(_.toString()), "CohortItem.startDate"))
      billingPeriod <- ZIO.fromEither(requiredField(cohortItem.billingPeriod, "CohortItem.billingPeriod"))
      paymentFrequency <- paymentFrequency(billingPeriod)
      currencyISOCode <- ZIO.fromEither(requiredField(cohortItem.currency, "CohortItem.currency"))
      currencySymbol <- currencyISOtoSymbol(currencyISOCode)

      forceEstimated = MigrationType(cohortSpec) match {
        case Membership2023Monthlies => true
        case Membership2023Annuals   => true
        case SupporterPlus2023V1V2MA => true
        case DigiSubs2023            => true
        case Newspaper2024           => true
        case Legacy                  => false
      }
      cappedEstimatedNewPriceWithCurrencySymbol = s"${currencySymbol}${PriceCap(oldPrice, estimatedNewPrice, forceEstimated)}"

      _ <- logMissingEmailAddress(cohortItem, contact)

      _ <- updateCohortItemStatus(cohortItem.subscriptionName, NotificationSendProcessingOrError)

      _ <- EmailSender.sendEmail(
        message = EmailMessage(
          EmailPayload(
            Address = contact.Email,
            ContactAttributes = EmailPayloadContactAttributes(
              SubscriberAttributes = EmailPayloadSubscriberAttributes(
                title = contact.FirstName flatMap (_ =>
                  contact.Salutation // if no first name, we use salutation as first name and leave this field empty
                ),
                first_name = firstName,
                last_name = lastName,
                billing_address_1 = street,
                billing_address_2 = None, // See 'Billing Address Format' section in the readme
                billing_city = address.city,
                billing_postal_code = postalCode,
                billing_state = address.state,
                billing_country = country,
                payment_amount = cappedEstimatedNewPriceWithCurrencySymbol,
                next_payment_date = startDateConversion(startDate),
                payment_frequency = paymentFrequency,
                subscription_id = cohortItem.subscriptionName,
                product_type = sfSubscription.Product_Type__c.getOrElse("")
              )
            )
          ),
          cohortSpec.brazeCampaignName,
          contact.Id,
          contact.IdentityID__c
        )
      )

      _ <- updateCohortItemStatus(cohortItem.subscriptionName, NotificationSendComplete)
    } yield Successful

  // -------------------------------------------------------------------
  // Subscription Rate Plan Checks

  private def fetchSubscription(item: CohortItem): ZIO[Zuora, Failure, ZuoraSubscription] =
    Zuora
      .fetchSubscription(item.subscriptionName)
      .filterOrFail(_.status != "Cancelled")(CancelledSubscriptionFailure(item.subscriptionName))

  private def subscriptionRatePlansCheck(
      item: CohortItem,
      subscription: ZuoraSubscription,
      date: LocalDate
  ): ZIO[CohortTable with Zuora, Failure, Unit] = {
    RateplansProbe.probe(subscription: ZuoraSubscription, date) match {
      case ShouldProceed => ZIO.succeed(())
      case ShouldCancel => {
        val result = CancelledAmendmentResult(item.subscriptionName)
        ZIO.succeed(
          CohortTable
            .update(
              CohortItem
                .fromCancelledAmendmentResult(result, "(cause: f5c291b0) Migration cancelled by RateplansProbe")
            )
            .as(result)
        )
      }
      case IndeterminateConclusion =>
        ZIO.fail(
          RatePlansProbeFailure(
            s"[4f7589ea] NotificationHandler probeRatePlans could not determine a probe outcome for cohort item ${item}. Please investigate."
          )
        )
    }
  }

  private def cohortItemRatePlansChecks(item: CohortItem): ZIO[CohortTable with Zuora, Failure, Unit] = {
    for {
      subscription <- fetchSubscription(item: CohortItem)
      estimationInstant <- ZIO
        .fromOption(item.whenEstimationDone)
        .mapError(ex => AmendmentDataFailure(s"[3026515c] Could not extract whenEstimationDone from item ${item}"))
      result <- subscriptionRatePlansCheck(
        item,
        subscription,
        estimationInstant.atZone(ZoneId.of("Europe/London")).toLocalDate
      )
    } yield result
  }

  // -------------------------------------------------------------------
  // Notification Windows

  // For general information about the notification period see the docs/notification-periods.md

  // The standard notification period for letter products (where the notification is delivered by email)
  // is -49 (included) to -35 (excluded) days. Legally the min is 30 days, but we set 35 days to alert if a
  // subscription if exiting the notification window and needs to be investigated and repaired before the deadline
  // of 30 days.

  // The digital migrations' notification window is from -33 (included) to -31 (excluded)

  def maxLeadTime(cohortSpec: CohortSpec): Int = {
    MigrationType(cohortSpec) match {
      case Membership2023Monthlies => Membership2023Migration.maxLeadTime
      case Membership2023Annuals   => Membership2023Migration.maxLeadTime
      case SupporterPlus2023V1V2MA => SupporterPlus2023V1V2Migration.maxLeadTime
      case DigiSubs2023            => DigiSubs2023Migration.maxLeadTime
      case Newspaper2024           => newspaper2024Migration.StaticData.maxLeadTime
      case Legacy                  => 49
    }
  }

  def minLeadTime(cohortSpec: CohortSpec): Int = {
    MigrationType(cohortSpec) match {
      case Membership2023Monthlies => Membership2023Migration.minLeadTime
      case Membership2023Annuals   => Membership2023Migration.minLeadTime
      case SupporterPlus2023V1V2MA => SupporterPlus2023V1V2Migration.minLeadTime
      case DigiSubs2023            => DigiSubs2023Migration.minLeadTime
      case Newspaper2024           => newspaper2024Migration.StaticData.minLeadTime
      case Legacy                  => 35
    }
  }

  def thereIsEnoughNotificationLeadTime(cohortSpec: CohortSpec, today: LocalDate, cohortItem: CohortItem): Boolean = {
    // To help with backward compatibility with existing tests, we apply this condition from 1st Dec 2020.
    if (today.isBefore(LocalDate.of(2020, 12, 1))) {
      true
    } else {
      cohortItem.startDate match {
        case Some(sd) => today.plusDays(minLeadTime(cohortSpec)).isBefore(sd)
        case _        => false
      }
    }
  }

  // -------------------------------------------------------------------
  // Support Functions

  def currencyISOtoSymbol(iso: String): ZIO[Any, Nothing, String] = {
    ZIO.succeed(i18n.Currency.fromString(iso: String).map(_.identifier).getOrElse(""))
  }

  def dateStrToLocalDate(startDate: String): LocalDate = {
    LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
  }

  def emailUserFriendlyDateFormatter(startDate: LocalDate): String = {
    startDate.format(DateTimeFormatter.ofPattern("d MMMM uuuu"));
  }

  def startDateConversion(startDate: String): String = {
    emailUserFriendlyDateFormatter(dateStrToLocalDate(startDate: String))
  }

  def requiredField[A](field: Option[A], fieldName: String): Either[NotificationHandlerFailure, A] = {
    field match {
      case Some(value) => Right(value)
      case None        => Left(NotificationHandlerFailure(s"$fieldName is a required field"))
    }
  }

  def targetAddress(
      cohortSpec: CohortSpec,
      contact: SalesforceContact
  ): Either[NotificationHandlerFailure, SalesforceAddress] = {
    MigrationType(cohortSpec) match {
      case DigiSubs2023 => Right(SalesforceAddress(Some(""), Some(""), Some(""), Some(""), Some("")))
      case _ =>
        (for {
          billingAddress <- requiredField(contact.OtherAddress, "Contact.OtherAddress")
          _ <- requiredField(billingAddress.street, "Contact.OtherAddress.street")
          _ <- requiredField(billingAddress.city, "Contact.OtherAddress.city")
        } yield billingAddress).left.flatMap(_ => requiredField(contact.MailingAddress, "Contact.MailingAddress"))
    }
  }

  def firstName(contact: SalesforceContact): Either[NotificationHandlerFailure, String] = {
    requiredField(contact.FirstName, "Contact.FirstName").left
      .flatMap(_ => requiredField(contact.Salutation.fold(Some("Member"))(Some(_)), "Contact.Salutation"))
  }

  def address(
      cohortSpec: CohortSpec,
      contact: SalesforceContact
  ): Either[NotificationHandlerFailure, SalesforceAddress] = {
    MigrationType(cohortSpec) match {
      case SupporterPlus2023V1V2MA => Right(SalesforceAddress(None, None, None, None, None))
      case _                       => targetAddress(cohortSpec, contact)
    }
  }

  def street(
      cohortSpec: CohortSpec,
      address: SalesforceAddress
  ): Either[NotificationHandlerFailure, String] = {
    MigrationType(cohortSpec) match {
      case Membership2023Monthlies =>
        requiredField(address.street.fold(Some(""))(Some(_)), "Contact.OtherAddress.street")
      case Membership2023Annuals =>
        requiredField(address.street.fold(Some(""))(Some(_)), "Contact.OtherAddress.street")
      case SupporterPlus2023V1V2MA => Right("")
      case _                       => requiredField(address.street, "Contact.OtherAddress.street")
    }
  }

  def country(
      cohortSpec: CohortSpec,
      address: SalesforceAddress
  ): Either[NotificationHandlerFailure, String] = {
    // The country is usually a required field, this came from the old print migrations. It was
    // not required for the 2023 digital migrations. Although technically required for
    // the 2024 print migration, "United Kingdom" can be substituted for missing values considering
    // that we are only delivery in the UK.
    MigrationType(cohortSpec) match {
      case Membership2023Monthlies =>
        requiredField(address.country.fold(Some("United Kingdom"))(Some(_)), "Contact.OtherAddress.country")
      case Membership2023Annuals =>
        requiredField(address.country.fold(Some("United Kingdom"))(Some(_)), "Contact.OtherAddress.country")
      case SupporterPlus2023V1V2MA =>
        requiredField(address.country.fold(Some("United Kingdom"))(Some(_)), "Contact.OtherAddress.country")
      case Newspaper2024 => Right(address.country.getOrElse("United Kingdom"))
      case _             => requiredField(address.country, "Contact.OtherAddress.country")
    }
  }

  def logMissingEmailAddress(cohortItem: CohortItem, sfContact: SalesforceContact): ZIO[Logging, Nothing, Unit] = {
    Logging
      .info(
        s"Subscription ${cohortItem.subscriptionName} is for contact ${sfContact.Id} that has not email address"
      )
      .when(sfContact.Email.isEmpty)
      .unit
  }

  private def paymentFrequency(billingPeriod: String) =
    ZIO
      .fromOption(BillingPeriod.notificationPaymentFrequencyMapping.get(billingPeriod))
      .orElseFail(EmailSenderFailure(s"No payment frequency mapping found for billing period: $billingPeriod"))

  private def updateCohortItemStatus(subscriptionNumber: String, processingStage: CohortTableFilter) = {
    for {
      now <- Clock.instant
      _ <-
        CohortTable
          .update(
            CohortItem(
              subscriptionName = subscriptionNumber,
              processingStage = processingStage,
              whenNotificationSent = Some(now)
            )
          )
          .mapError { error =>
            NotificationHandlerFailure(s"Failed set status CohortItem $subscriptionNumber to $processingStage: $error")
          }
    } yield ()
  }

  def putSubIntoCancelledStatus(subscriptionName: String): ZIO[Logging with CohortTable, Failure, Int] =
    for {
      _ <-
        CohortTable
          .update(
            CohortItem(
              subscriptionName = subscriptionName,
              processingStage = Cancelled
            )
          )
      _ <- Logging.error(s"Subscription $subscriptionName has been cancelled, price rise notification not sent")
    } yield 0
}
