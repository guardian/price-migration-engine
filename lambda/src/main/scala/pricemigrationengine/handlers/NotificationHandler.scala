package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.Context
import pricemigrationengine.model.CohortTableFilter.{NotificationSendComplete, NotificationSendProcessingOrError, SalesforcePriceRiceCreationComplete}
import pricemigrationengine.model.membershipworkflow.{EmailMessage, EmailPayload, EmailPayloadContactAttributes, EmailPayloadSubscriberAttributes}
import pricemigrationengine.model.{CohortItem, CohortTableFilter, EmailSenderFailure, Failure, NotificationHandlerFailure}
import pricemigrationengine.services._
import zio.clock.Clock
import zio.console.Console
import zio.stream.ZStream
import zio.{Runtime, ZEnv, ZIO, ZLayer, clock}

object NotificationHandler {
  //Mapping to environment specific braze campaign id is provided by membership-workflow:
  //https://github.com/guardian/membership-workflow/blob/master/conf/PROD.public.conf#L39
  val BrazeCampaignName = "SV_VO_Pricerise_Q22020"

  val Successful = 1
  val Unsuccessful = 0

  private val NotificationLeadTimeDays = 37

  val main: ZIO[Logging with CohortTable with SalesforceClient with Clock with EmailSender, Failure, Unit] = {
    for {
      today <- Time.today
      subscriptions <- CohortTable.fetch(
        SalesforcePriceRiceCreationComplete,
        Some(today.plusDays(NotificationLeadTimeDays))
      )
      count <- subscriptions
        .take(100)
        .mapM(sendNotification)
        .fold(0) { (sum, count) => sum + count }
      _ <- Logging.info(s"Successfully sent $count prices rise notifications")
    } yield ()
  }

  def sendNotification(
      cohortItem: CohortItem
  ): ZIO[EmailSender with SalesforceClient with CohortTable with Clock with Logging, Failure, Int] = {
    val result = for {
      _ <- Logging.info(s"Processing subscription: ${cohortItem.subscriptionName}")
      sfSubscription <- SalesforceClient.getSubscriptionByName(cohortItem.subscriptionName)
      contact <- SalesforceClient.getContact(sfSubscription.Buyer__c)
      emailAddress <- requiredField(contact.Email, "Contact.Email")
      firstName <- requiredField(contact.FirstName, "Contact.FirstName")
      lastName <- requiredField(contact.LastName, "Contact.LastName")
      otherAddress <- requiredField(contact.OtherAddress, "Contact.OtherAddress" )
      street <- requiredField(otherAddress.street, "Contact.OtherAddress.street")
      postalCode <- requiredField(otherAddress.postalCode, "Contact.OtherAddress.postalCode")
      country <- requiredField(otherAddress.country, "Contact.OtherAddress.country")
      estimatedNewPrice <- requiredField(cohortItem.estimatedNewPrice.map(_.toString()), "CohortItem.estimatedNewPrice")
      startDate <- requiredField(cohortItem.startDate.map(_.toString()), "CohortItem.startDate")
      billingPeriod <- requiredField(cohortItem.billingPeriod, "CohortItem.billingPeriod")
      paymentFrequency <- paymentFrequency(billingPeriod)

      _ <- updateCohortItemStatus(cohortItem.subscriptionName, NotificationSendProcessingOrError)

      _ <- EmailSender.sendEmail(
        message = EmailMessage(
          EmailPayload(
            Address = emailAddress,
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

    result.catchAll { failure =>
      for {
        _ <- Logging.error(s"Failed to send price rise notification: $failure")
      } yield Unsuccessful
    }
  }

  def requiredField[A](field: Option[A], fieldName: String): ZIO[Any, NotificationHandlerFailure, A] = {
    ZIO.fromOption(field).orElseFail(NotificationHandlerFailure(s"$fieldName is a required field"))
  }

  val paymentFrequencyMapping = Map(
    "Month" -> "Monthly",
    "Quarter" -> "Quarterly",
    "Semi_Annual" -> "Semiannually"
  )

  def paymentFrequency(billingPeriod: String) =
    ZIO
      .fromOption(paymentFrequencyMapping.get(billingPeriod))
      .orElseFail(EmailSenderFailure(s"No payment frequency mapping found for billing period: $billingPeriod"))

  def updateCohortItemStatus(subscriptionNumber: String, processingStage: CohortTableFilter) = {
    for {
      now <- Time.thisInstant
      _ <- CohortTable
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

  private def env(
      loggingService: Logging.Service
  ): ZLayer[Any, Any, Logging with CohortTable with SalesforceClient with Clock with EmailSender] = {
    val loggingLayer = ZLayer.succeed(loggingService)
    val cohortTableLayer =
      loggingLayer ++ EnvConfiguration.dynamoDbImpl >>>
        DynamoDBClient.dynamoDB ++ loggingLayer >>>
        DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.cohortTableImp ++
          EnvConfiguration.stageImp ++ EnvConfiguration.salesforceImp ++ EnvConfiguration.emailSenderImp >>>
        CohortTableLive.impl ++ SalesforceClientLive.impl ++ Clock.live ++ EmailSenderLive.impl
    (loggingLayer ++ cohortTableLayer)
      .tapError(e => loggingService.error(s"Failed to create service environment: $e"))
  }

  private val runtime = Runtime.default

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    main
      .provideSomeLayer(
        env(ConsoleLogging.service(Console.Service.live))
      )
      .fold(_ => 1, _ => 0)

  def handleRequest(unused: Unit, context: Context): Unit =
    runtime.unsafeRun(
      main.provideSomeLayer(
        env(LambdaLogging.service(context))
      )
    )
}
