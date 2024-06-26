package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model._
import pricemigrationengine.model.membershipworkflow._
import pricemigrationengine.services._
import zio.{Clock, ZIO}
import com.gu.i18n
import pricemigrationengine.migrations.{
  DigiSubs2023Migration,
  GW2024Migration,
  Membership2023Migration,
  newspaper2024Migration
}
import pricemigrationengine.model.RateplansProbe

import java.time.{LocalDate, ZoneId}
import java.time.format.DateTimeFormatter

object NotificationHandler extends CohortHandler {

  private val batchSize = 150

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
        .take(batchSize)
        .mapZIO(item => sendNotification(cohortSpec)(item, today))
        .runCount
    } yield HandlerOutput(isComplete = count < batchSize)
  }

  def sendNotification(cohortSpec: CohortSpec)(
      cohortItem: CohortItem,
      today: LocalDate
  ): ZIO[EmailSender with SalesforceClient with CohortTable with Logging with Zuora, Failure, Unit] = {
    for {
      _ <-
        if (thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem)) {
          ZIO.succeed(())
        } else {
          ZIO.fail(
            NotificationNotEnoughLeadTimeFailure(
              s"[notification] The start date of item ${cohortItem.subscriptionName} (startDate: ${cohortItem.startDate}) is too close to today ${today}"
            )
          )
        }
      _ <- cohortItemRatePlansChecks(cohortSpec, cohortItem)
      sfSubscription <-
        SalesforceClient
          .getSubscriptionByName(cohortItem.subscriptionName)
      _ <-
        if (sfSubscription.Status__c != Cancelled_Status) {
          sendNotification(cohortSpec, cohortItem, sfSubscription)
        } else {
          putSubIntoCancelledStatus(cohortSpec, cohortItem, Some("Item has been cancelled in Zuora"))
        }
    } yield ()
  }

  def sendNotification(
      cohortSpec: CohortSpec,
      cohortItem: CohortItem,
      sfSubscription: SalesforceSubscription
  ): ZIO[EmailSender with SalesforceClient with CohortTable with Logging, Failure, Unit] =
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

      priceWithOptionalCappingWithCurrencySymbol = MigrationType(cohortSpec) match {
        case Legacy                  => s"${currencySymbol}${PriceCap.priceCapLegacy(oldPrice, estimatedNewPrice)}"
        case Membership2023Monthlies => s"${currencySymbol}${estimatedNewPrice}"
        case Membership2023Annuals   => s"${currencySymbol}${estimatedNewPrice}"
        case SupporterPlus2023V1V2MA => s"${currencySymbol}${estimatedNewPrice}"
        case DigiSubs2023            => s"${currencySymbol}${estimatedNewPrice}"
        case Newspaper2024           => s"${currencySymbol}${estimatedNewPrice}"
        case GW2024 =>
          s"${currencySymbol}${PriceCap.priceCapForNotification(oldPrice, estimatedNewPrice, GW2024Migration.priceCap)}"
      }

      _ <- logMissingEmailAddress(cohortItem, contact)

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
                payment_amount = priceWithOptionalCappingWithCurrencySymbol,
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
    } yield ()

  // -------------------------------------------------------------------
  // Subscription Rate Plan Checks

  private def subscriptionRatePlansCheck(
      cohortSpec: CohortSpec,
      item: CohortItem,
      subscription: ZuoraSubscription,
      date: LocalDate
  ): ZIO[CohortTable with Zuora with SalesforceClient with Logging, Failure, Unit] = {
    for {
      _ <- RateplansProbe.probe(subscription: ZuoraSubscription, date) match {
        case ShouldProceed => ZIO.succeed(())
        case ShouldCancel =>
          val result = CancelledAmendmentResult(item.subscriptionName)
          for {
            _ <- CohortTable
              .update(
                CohortItem
                  .fromCancelledAmendmentResult(result, "(cause: f5c291b0) Migration cancelled by RateplansProbe")
              )
            _ <- notifySalesforceOfCancelledStatus(cohortSpec, item, Some("Migration cancelled by RateplansProbe"))
          } yield ZIO.fail(
            RatePlansProbeFailure("(cause: f5c291b0) Migration cancelled by RateplansProbe")
          )
        case IndeterminateConclusion =>
          ZIO.fail(
            RatePlansProbeFailure(
              s"[4f7589ea] NotificationHandler probeRatePlans could not determine a probe outcome for cohort item ${item}. Please investigate."
            )
          )
      }
    } yield ()
  }

  private def cohortItemRatePlansChecks(
      cohortSpec: CohortSpec,
      item: CohortItem
  ): ZIO[CohortTable with Zuora with SalesforceClient with Logging, Failure, Unit] = {
    for {
      subscription <- Zuora.fetchSubscription(item.subscriptionName)
      estimationInstant <- ZIO
        .fromOption(item.whenEstimationDone)
        .mapError(ex => AmendmentDataFailure(s"[3026515c] Could not extract whenEstimationDone from item ${item}"))
      _ <- subscriptionRatePlansCheck(
        cohortSpec,
        item,
        subscription,
        estimationInstant.atZone(ZoneId.of("Europe/London")).toLocalDate
      )
    } yield ()
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
      case GW2024                  => GW2024Migration.maxLeadTime
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
      case GW2024                  => GW2024Migration.minLeadTime
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

  def notifySalesforceOfCancelledStatus(
      cohortSpec: CohortSpec,
      cohortItem: CohortItem,
      reason: Option[String]
  ): ZIO[Logging with SalesforceClient, Failure, Unit] = {
    for {
      salesforcePriceRiseId <-
        ZIO
          .fromOption(cohortItem.salesforcePriceRiseId)
          .orElseFail(SalesforcePriceRiseWriteFailure("salesforcePriceRiseId is required to update Salesforce"))
      priceRise = SalesforcePriceRise(
        Migration_Name__c = Some(cohortSpec.cohortName),
        Migration_Status__c = Some("Cancellation"),
        Cancellation_Reason__c = reason
      )
      _ <- SalesforceClient.updatePriceRise(salesforcePriceRiseId, priceRise)
    } yield ()
  }

  def putSubIntoCancelledStatus(
      cohortSpec: CohortSpec,
      cohortItem: CohortItem,
      reason: Option[String]
  ): ZIO[Logging with SalesforceClient with CohortTable, Failure, Unit] = {
    for {
      _ <-
        CohortTable
          .update(
            CohortItem(
              subscriptionName = cohortItem.subscriptionName,
              processingStage = Cancelled
            )
          )
      _ <- notifySalesforceOfCancelledStatus(cohortSpec, cohortItem, reason)
      _ <- Logging.error(
        s"Subscription ${cohortItem.subscriptionName} has been cancelled, price rise notification not sent"
      )
    } yield ()
  }
}
