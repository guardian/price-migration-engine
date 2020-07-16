package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter.{
  Cancelled,
  NotificationSendComplete,
  NotificationSendProcessingOrError,
  SalesforcePriceRiceCreationComplete
}
import pricemigrationengine.model._
import pricemigrationengine.model.membershipworkflow.{
  EmailMessage,
  EmailPayload,
  EmailPayloadContactAttributes,
  EmailPayloadSubscriberAttributes
}
import pricemigrationengine.services._
import zio.clock.Clock
import zio.{ZEnv, ZIO, ZLayer}

object NotificationHandler extends CohortHandler {

  //Mapping to environment specific braze campaign id is provided by membership-workflow:
  //https://github.com/guardian/membership-workflow/blob/master/conf/PROD.public.conf#L39
  val BrazeCampaignName = "SV_VO_Pricerise_Q22020"

  val Successful = 1
  val Unsuccessful = 0

  val Cancelled_Status = "Cancelled"

  private val NotificationLeadTimeDays = 37

  val main: ZIO[Logging with CohortTable with SalesforceClient with Clock with EmailSender, Failure, HandlerOutput] = {
    for {
      today <- Time.today
      subscriptions <- CohortTable.fetch(
        SalesforcePriceRiceCreationComplete,
        Some(today.plusDays(NotificationLeadTimeDays))
      )
      count <-
        subscriptions
          .mapM(sendNotification)
          .fold(0) { (sum, count) => sum + count }
      _ <- Logging.info(s"Successfully sent $count price rise notifications")
    } yield HandlerOutput(isComplete = true)
  }

  def sendNotification(
      cohortItem: CohortItem
  ): ZIO[EmailSender with SalesforceClient with CohortTable with Clock with Logging, Failure, Int] = {
    val result = for {
      sfSubscription <-
        SalesforceClient
          .getSubscriptionByName(cohortItem.subscriptionName)
      count <-
        if (sfSubscription.Status__c != Cancelled_Status) {
          sendNotification(cohortItem, sfSubscription)
        } else {
          putSubIntoCancelledStatus(cohortItem.subscriptionName)
        }
    } yield count

    result.catchAll { failure =>
      for {
        _ <- Logging.error(s"Failed to send price rise notification: $failure")
      } yield Unsuccessful
    }
  }

  def sendNotification(
      cohortItem: CohortItem,
      sfSubscription: SalesforceSubscription
  ): ZIO[EmailSender with SalesforceClient with CohortTable with Clock with Logging, Failure, Int] =
    for {
      _ <- Logging.info(s"Processing subscription: ${cohortItem.subscriptionName}")
      contact <- SalesforceClient.getContact(sfSubscription.Buyer__c)
      firstName <- requiredField(contact.FirstName, "Contact.FirstName")
      lastName <- requiredField(contact.LastName, "Contact.LastName")
      otherAddress <- requiredField(contact.OtherAddress, "Contact.OtherAddress")
      street <- requiredField(otherAddress.street, "Contact.OtherAddress.street")
      postalCode <- requiredField(otherAddress.postalCode, "Contact.OtherAddress.postalCode")
      country <- requiredField(otherAddress.country, "Contact.OtherAddress.country")
      estimatedNewPrice <- requiredField(cohortItem.estimatedNewPrice.map(_.toString()), "CohortItem.estimatedNewPrice")
      startDate <- requiredField(cohortItem.startDate.map(_.toString()), "CohortItem.startDate")
      billingPeriod <- requiredField(cohortItem.billingPeriod, "CohortItem.billingPeriod")
      paymentFrequency <- paymentFrequency(billingPeriod)

      _ <- logMissingEmailAddress(cohortItem, contact)

      _ <- updateCohortItemStatus(cohortItem.subscriptionName, NotificationSendProcessingOrError)

      _ <- EmailSender.sendEmail(
        message = EmailMessage(
          EmailPayload(
            Address = contact.Email,
            ContactAttributes = EmailPayloadContactAttributes(
              SubscriberAttributes = EmailPayloadSubscriberAttributes(
                title = contact.Salutation,
                first_name = firstName,
                last_name = lastName,
                billing_address_1 = street,
                billing_address_2 = None, //See 'Billing Address Format' section in the readme
                billing_city = otherAddress.city,
                billing_postal_code = postalCode,
                billing_state = otherAddress.state,
                billing_country = country,
                payment_amount = estimatedNewPrice,
                next_payment_date = startDate,
                payment_frequency = paymentFrequency,
                subscription_id = cohortItem.subscriptionName
              )
            )
          ),
          BrazeCampaignName,
          contact.Id,
          contact.IdentityID__c
        )
      )

      _ <- updateCohortItemStatus(cohortItem.subscriptionName, NotificationSendComplete)
    } yield Successful

  def requiredField[A](field: Option[A], fieldName: String): ZIO[Any, NotificationHandlerFailure, A] = {
    ZIO.fromOption(field).orElseFail(NotificationHandlerFailure(s"$fieldName is a required field"))
  }

  def logMissingEmailAddress(cohortItem: CohortItem, sfContact: SalesforceContact)  = {
    if(sfContact.Email.isEmpty) {
      Logging.info(
        s"Subscription ${cohortItem.subscriptionName} is for contact ${sfContact.Id} that has not email address"
      )
    } else {
      ZIO.unit
    }
  }

  val paymentFrequencyMapping = Map(
    "Month" -> "Monthly",
    "Quarter" -> "Quarterly",
    "Semi_Annual" -> "Semiannually",
    "Annual" -> "Annually"
  )

  def paymentFrequency(billingPeriod: String) =
    ZIO
      .fromOption(paymentFrequencyMapping.get(billingPeriod))
      .orElseFail(EmailSenderFailure(s"No payment frequency mapping found for billing period: $billingPeriod"))

  def updateCohortItemStatus(subscriptionNumber: String, processingStage: CohortTableFilter) = {
    for {
      now <- Time.thisInstant
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

  private def env(
      cohortSpec: CohortSpec,
      loggingService: Logging.Service
  ): ZLayer[Any, Failure, Logging with CohortTable with SalesforceClient with Clock with EmailSender] = {
    val loggingLayer = ZLayer.succeed(loggingService)
    val cohortTableLayer =
      loggingLayer ++ EnvConfiguration.dynamoDbImpl >>>
        DynamoDBClient.dynamoDB ++ loggingLayer >>>
        DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.cohortTableImp ++
          EnvConfiguration.stageImp ++ EnvConfiguration.salesforceImp ++ EnvConfiguration.emailSenderImp >>>
        CohortTableLive.impl(cohortSpec.tableName) ++ SalesforceClientLive.impl ++ Clock.live ++ EmailSenderLive.impl
    (loggingLayer ++ cohortTableLayer)
      .tapError(e => loggingService.error(s"Failed to create service environment: $e"))
  }

  def handle(input: CohortSpec, loggingService: Logging.Service): ZIO[ZEnv, Failure, HandlerOutput] =
    main.provideCustomLayer(env(input, loggingService))
}
