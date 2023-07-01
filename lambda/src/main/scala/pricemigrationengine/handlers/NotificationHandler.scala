package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model._
import pricemigrationengine.model.membershipworkflow._
import pricemigrationengine.services._
import zio.{Clock, ZIO}
import com.gu.i18n

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object NotificationHandler extends CohortHandler {

  val Successful = 1
  val Unsuccessful = 0
  val Cancelled_Status = "Cancelled"

  // For general information about the notification period see the docs/notification-periods.md

  // The standard notification period for letter products (where the notification is delivered by email)
  // is -49 (included) to -35 (excluded) days.
  val guLettersNotificationLeadTime = 49
  private val engineLettersMinNotificationLeadTime = 35

  // Membership migration
  // Notification period: -33 (included) to -31 (excluded) days
  private val membershipPriceRiseNotificationLeadTime = 33
  private val membershipMinNotificationLeadTime = 31

  def maxLeadTime(cohortSpec: CohortSpec): Int = {
    if (CohortSpec.isMembershipPriceRise(cohortSpec)) {
      membershipPriceRiseNotificationLeadTime
    } else {
      guLettersNotificationLeadTime
    }
  }

  def minLeadTime(cohortSpec: CohortSpec): Int = {
    if (CohortSpec.isMembershipPriceRise(cohortSpec)) {
      membershipMinNotificationLeadTime
    } else {
      engineLettersMinNotificationLeadTime
    }
  }

  def thereIsEnoughNotificationLeadTime(cohortSpec: CohortSpec, today: LocalDate, cohortItem: CohortItem): Boolean = {
    // To help with backward compatibility with existing tests, we apply this condition from 1st Dec 2022.
    if (today.isBefore(LocalDate.of(2020, 12, 1))) {
      true
    } else {
      cohortItem.startDate match {
        case Some(sd) => today.plusDays(minLeadTime(cohortSpec)).isBefore(sd)
        case _        => false
      }
    }
  }

  def main(
      cohortSpec: CohortSpec
  ): ZIO[Logging with CohortTable with SalesforceClient with EmailSender, Failure, HandlerOutput] = {
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
  ): ZIO[EmailSender with SalesforceClient with CohortTable with Logging, Failure, Int] = {

    // We are starting with a simple check. That the item's startDate is at least minLeadTime(cohortSpec) days away
    // from the current day. This will avoid headaches caused by letters not being sent early enough relatively to
    // previously computed start dates, which can happen if, for argument sake, the engine is down for a few days.

    if (thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem)) {
      val result = for {
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

  def sendNotification(
      cohortSpec: CohortSpec,
      cohortItem: CohortItem,
      sfSubscription: SalesforceSubscription
  ): ZIO[EmailSender with SalesforceClient with CohortTable with Logging, Failure, Int] =
    for {
      _ <- Logging.info(s"Processing subscription: ${cohortItem.subscriptionName}")
      contact <- SalesforceClient.getContact(sfSubscription.Buyer__c)
      firstName <- requiredField(contact.FirstName, "Contact.FirstName").orElse(
        requiredField(contact.Salutation.fold(Some("Member"))(Some(_)), "Contact.Salutation")
      )
      lastName <- requiredField(contact.LastName, "Contact.LastName")
      address <- targetAddress(contact)
      street <-
        if (CohortSpec.isMembershipPriceRise(cohortSpec)) {
          requiredField(address.street.fold(Some(""))(Some(_)), "Contact.OtherAddress.street")
        } else {
          requiredField(address.street, "Contact.OtherAddress.street")
        }
      postalCode = address.postalCode.getOrElse("")
      country <-
        if (CohortSpec.isMembershipPriceRise(cohortSpec)) {
          requiredField(address.country.fold(Some("United Kingdom"))(Some(_)), "Contact.OtherAddress.country")
        } else {
          requiredField(address.country, "Contact.OtherAddress.country")
        }
      oldPrice <- requiredField(cohortItem.oldPrice, "CohortItem.oldPrice")
      estimatedNewPrice <- requiredField(cohortItem.estimatedNewPrice, "CohortItem.estimatedNewPrice")
      startDate <- requiredField(cohortItem.startDate.map(_.toString()), "CohortItem.startDate")
      billingPeriod <- requiredField(cohortItem.billingPeriod, "CohortItem.billingPeriod")
      paymentFrequency <- paymentFrequency(billingPeriod)
      currencyISOCode <- requiredField(cohortItem.currency, "CohortItem.currency")
      currencySymbol <- currencyISOtoSymbol(currencyISOCode)

      // In the case of membership price rise, we need to not cap the price
      cappedEstimatedNewPriceWithCurrencySymbol = s"${currencySymbol}${PriceCap.cappedPrice(oldPrice, estimatedNewPrice, CohortSpec.isMembershipPriceRise(cohortSpec))}"

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

  def targetAddress(contact: SalesforceContact): ZIO[Any, NotificationHandlerFailure, SalesforceAddress] =
    (for {
      billingAddress <- requiredField(contact.OtherAddress, "Contact.OtherAddress")
      _ <- requiredField(billingAddress.street, "Contact.OtherAddress.street")
      _ <- requiredField(billingAddress.city, "Contact.OtherAddress.city")
    } yield billingAddress).orElse(
      requiredField(contact.MailingAddress, "Contact.MailingAddress")
    )

  def requiredField[A](field: Option[A], fieldName: String): ZIO[Any, NotificationHandlerFailure, A] = {
    ZIO.fromOption(field).orElseFail(NotificationHandlerFailure(s"$fieldName is a required field"))
  }

  def logMissingEmailAddress(cohortItem: CohortItem, sfContact: SalesforceContact): ZIO[Logging, Nothing, Unit] = {
    Logging
      .info(
        s"Subscription ${cohortItem.subscriptionName} is for contact ${sfContact.Id} that has not email address"
      )
      .when(sfContact.Email.isEmpty)
      .unit
  }

  val paymentFrequencyMapping = Map(
    "Month" -> "Monthly",
    "Quarter" -> "Quarterly",
    "Semi_Annual" -> "Semiannually",
    "Annual" -> "Annually"
  )

  private def paymentFrequency(billingPeriod: String) =
    ZIO
      .fromOption(paymentFrequencyMapping.get(billingPeriod))
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

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] = {
    if (!CohortSpec.isMembershipPriceRiseBatch3(input)) {
      main(input).provideSome[Logging](
        EnvConfig.salesforce.layer,
        EnvConfig.cohortTable.layer,
        EnvConfig.emailSender.layer,
        EnvConfig.stage.layer,
        DynamoDBClientLive.impl,
        DynamoDBZIOLive.impl,
        CohortTableLive.impl(input),
        SalesforceClientLive.impl,
        EmailSenderLive.impl
      )
    } else {
      ZIO.succeed(HandlerOutput(isComplete = true))
    }
  }
}
