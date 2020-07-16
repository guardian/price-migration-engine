package pricemigrationengine.handlers

import java.time._
import java.time.temporal.ChronoUnit

import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model._
import pricemigrationengine.model.membershipworkflow.EmailMessage
import pricemigrationengine.services._
import pricemigrationengine.{StubClock, TestLogging}
import zio.Exit.Success
import zio.Runtime.default
import zio._
import zio.stream.ZStream

import scala.collection.mutable.ArrayBuffer

class NotificationHandlerTest extends munit.FunSuite {
  val expectedSubscriptionName = "Sub-0001"
  val expectedStartDate = LocalDate.of(2020, 1, 1)
  val expectedCurrency = "GBP"
  val expectedBillingPeriod = "Month"
  val expectedBillingPeriodInNotification = "Monthly"
  val expectedOldPrice = BigDecimal(11.11)
  val expectedEstimatedNewPrice = BigDecimal(22.22)
  val expectedSFSubscriptionId = "1234"
  val expectedBuyerId = "buyer-1"
  val expectedIdentityId = "buyer1-identity-id"
  val expectedEmailAddress = "buyer@email.address"
  val expectedFirstName = "buyer1FirstName"
  val expectedLastName = "buyer1LastName"
  val expectedStreet = "buyer1Street"
  val expectedCity = "buyer1City"
  val expectedState = "buyer1State"
  val expectedPostalCode = "buyer1PostalCode"
  val expectedCountry = "buyer1Country"
  val expectedDataExtensionName = "SV_VO_Pricerise_Q22020"
  val expectedSalutation = "Ms"
  val expectedSfStatus = "Active"

  def createStubCohortTable(updatedResultsWrittenToCohortTable: ArrayBuffer[CohortItem], cohortItem: CohortItem) = {
    ZLayer.succeed(
      new CohortTable.Service {
        override def fetch(
            filter: CohortTableFilter,
            beforeDateInclusive: Option[LocalDate]
        ): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = {
          assertEquals(filter, SalesforcePriceRiceCreationComplete)
          assertEquals(
            beforeDateInclusive,
            Some(
              LocalDate
                .from(StubClock.expectedCurrentTime.plus(37, ChronoUnit.DAYS).atOffset(ZoneOffset.UTC))
            )
          )
          IO.succeed(ZStream(cohortItem))
        }

        override def put(cohortItem: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = ???

        override def update(result: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = {
          updatedResultsWrittenToCohortTable.addOne(result)
          IO.succeed(())
        }
      }
    )
  }

  private def stubSFClient(
      subscriptions: List[SalesforceSubscription],
      contacts: List[SalesforceContact]
  ) = {
    ZLayer.succeed(
      new SalesforceClient.Service {
        override def getSubscriptionByName(
            subscriptionName: String
        ): IO[SalesforceClientFailure, SalesforceSubscription] =
          ZIO
            .fromOption(subscriptions.find(_.Name == subscriptionName))
            .orElseFail(SalesforceClientFailure(s"No subscription for name '$subscriptionName'"))

        override def createPriceRise(
            priceRise: SalesforcePriceRise
        ): IO[SalesforceClientFailure, SalesforcePriceRiseCreationResponse] = ???

        override def updatePriceRise(
            priceRiseId: String,
            priceRise: SalesforcePriceRise
        ): IO[SalesforceClientFailure, Unit] = ???

        override def getContact(
            contactId: String
        ): IO[SalesforceClientFailure, SalesforceContact] =
          ZIO
            .fromOption(contacts.find(_.Id == contactId))
            .orElseFail(SalesforceClientFailure(s"No subscription for name '$contactId'"))
      }
    )
  }

  private def createStubEmailSender(sendMessages: ArrayBuffer[EmailMessage]) = {
    ZLayer.succeed(
      new EmailSender.Service {
        override def sendEmail(message: EmailMessage): ZIO[Any, EmailSenderFailure, Unit] =
          ZIO
            .effect {
              sendMessages.addOne(message)
              ()
            }
            .orElseFail(EmailSenderFailure(""))
      }
    )
  }

  private def createFailingStubEmailSender() = {
    ZLayer.succeed(
      new EmailSender.Service {
        override def sendEmail(message: EmailMessage): ZIO[Any, EmailSenderFailure, Unit] =
          ZIO.fail(EmailSenderFailure("Bang!!"))
      }
    )
  }

  private val salesforceSubscription: SalesforceSubscription =
    SalesforceSubscription(
      expectedSFSubscriptionId,
      expectedSubscriptionName,
      expectedBuyerId,
      expectedSfStatus
    )

  private val salesforceContact: SalesforceContact =
    SalesforceContact(
      Id = expectedBuyerId,
      IdentityID__c = Some(expectedIdentityId),
      Email = Some(expectedEmailAddress),
      Salutation = Some(expectedSalutation),
      FirstName = Some(expectedFirstName),
      LastName = Some(expectedLastName),
      OtherAddress = Some(
        SalesforceAddress(
          street = Some(expectedStreet),
          city = Some(expectedCity),
          state = Some(expectedState),
          postalCode = Some(expectedPostalCode),
          country = Some(expectedCountry)
        )
      )
    )

  private val cohortItem =
    CohortItem(
      subscriptionName = expectedSubscriptionName,
      processingStage = AmendmentComplete,
      startDate = Some(expectedStartDate),
      currency = Some(expectedCurrency),
      oldPrice = Some(expectedOldPrice),
      estimatedNewPrice = Some(expectedEstimatedNewPrice),
      billingPeriod = Some(expectedBillingPeriod)
    )

  test("NotificationHandler should get records from cohort table and SF and send Email with the data") {
    val stubSalesforceClient = stubSFClient(List(salesforceSubscription), List(salesforceContact))
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()
    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)
    val sentMessages = ArrayBuffer[EmailMessage]()
    val stubEmailSender = createStubEmailSender(sentMessages)

    assertEquals(
      default.unsafeRunSync(
        NotificationHandler.main
          .provideLayer(
            TestLogging.logging ++ stubCohortTable ++ StubClock.clock ++ stubSalesforceClient ++ stubEmailSender
          )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(sentMessages.size, 1)
    assertEquals(sentMessages(0).DataExtensionName, expectedDataExtensionName)
    assertEquals(sentMessages(0).SfContactId, expectedBuyerId)
    assertEquals(sentMessages(0).IdentityUserId, Some(expectedIdentityId))
    assertEquals(sentMessages(0).To.Address, Some(expectedEmailAddress))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_address_1, expectedStreet)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_address_2, None)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_city, Some(expectedCity))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_state, Some(expectedState))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_postal_code, expectedPostalCode)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_country, expectedCountry)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.title, Some(expectedSalutation))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.first_name, expectedFirstName)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.last_name, expectedLastName)
    assertEquals(
      sentMessages(0).To.ContactAttributes.SubscriberAttributes.payment_amount,
      expectedEstimatedNewPrice.toString()
    )
    assertEquals(
      sentMessages(0).To.ContactAttributes.SubscriberAttributes.next_payment_date,
      expectedStartDate.toString()
    )
    assertEquals(
      sentMessages(0).To.ContactAttributes.SubscriberAttributes.payment_frequency,
      expectedBillingPeriodInNotification
    )
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.subscription_id, expectedSubscriptionName)

    assertEquals(updatedResultsWrittenToCohortTable.size, 2)
    assertEquals(
      updatedResultsWrittenToCohortTable(0),
      CohortItem(
        subscriptionName = expectedSubscriptionName,
        processingStage = NotificationSendProcessingOrError,
        whenNotificationSent = Some(StubClock.expectedCurrentTime)
      )
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(1),
      CohortItem(
        subscriptionName = expectedSubscriptionName,
        processingStage = NotificationSendComplete,
        whenNotificationSent = Some(StubClock.expectedCurrentTime)
      )
    )
  }

  test("NotificationHandler should leave CohortItem in processing state if email send fails") {
    val stubSalesforceClient = stubSFClient(List(salesforceSubscription), List(salesforceContact))
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()
    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)
    val failingStubEmailSender = createFailingStubEmailSender()

    assertEquals(
      default.unsafeRunSync(
        NotificationHandler.main
          .provideLayer(
            TestLogging.logging ++ stubCohortTable ++ StubClock.clock ++ stubSalesforceClient ++ failingStubEmailSender
          )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(updatedResultsWrittenToCohortTable.size, 1)
    assertEquals(
      updatedResultsWrittenToCohortTable(0),
      CohortItem(
        subscriptionName = expectedSubscriptionName,
        processingStage = NotificationSendProcessingOrError,
        whenNotificationSent = Some(StubClock.expectedCurrentTime)
      )
    )
  }

  test("NotificationHandler should leave CohortItem in cancelled state if subscription is cancelled") {
    val cancelledSubscription = salesforceSubscription.copy(Status__c = "Cancelled")
    val stubSalesforceClient = stubSFClient(List(cancelledSubscription), List(salesforceContact))
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()
    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)
    val sentMessages = ArrayBuffer[EmailMessage]()
    val stubEmailSender = createStubEmailSender(sentMessages)

    assertEquals(
      default.unsafeRunSync(
        NotificationHandler.main
          .provideLayer(
            TestLogging.logging ++ stubCohortTable ++ StubClock.clock ++ stubSalesforceClient ++ stubEmailSender
          )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(updatedResultsWrittenToCohortTable.size, 1)
    assertEquals(
      updatedResultsWrittenToCohortTable(0),
      CohortItem(
        subscriptionName = expectedSubscriptionName,
        processingStage = Cancelled
      )
    )
    assertEquals(sentMessages.size, 0)
  }
}
