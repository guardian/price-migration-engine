package pricemigrationengine.handlers

import java.time._
import java.time.temporal.ChronoUnit

import pricemigrationengine.StubClock
import pricemigrationengine.model.CohortTableFilter.{AmendmentComplete, EstimationComplete}
import pricemigrationengine.model._
import pricemigrationengine.model.membershipworkflow.EmailMessage
import pricemigrationengine.services._
import zio.Exit.Success
import zio.Runtime.default
import zio._
import zio.stream.ZStream

import scala.collection.mutable.ArrayBuffer

class NotificationEmailHandlerTest extends munit.FunSuite {
  val stubLogging = console.Console.live >>> ConsoleLogging.impl
  val expectedSubscriptionName = "Sub-0001"
  val expectedStartDate = LocalDate.of(2020, 1, 1)
  val expectedCurrency = "GBP"
  val expectedBillingPeriod = "Monthly"
  val expectedNewPrice = BigDecimal(19.99)
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


  def createStubCohortTable(updatedResultsWrittenToCohortTable:ArrayBuffer[CohortItem], cohortItem: CohortItem) = {
    ZLayer.succeed(
      new CohortTable.Service {
        override def fetch(
          filter: CohortTableFilter,
          beforeDateInclusive: Option[LocalDate]
        ): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = {
          assertEquals(filter, AmendmentComplete)
          assertEquals(
            beforeDateInclusive,
            Some(
              LocalDate
                .from(StubClock.expectedCurrentTime.plus(30, ChronoUnit.DAYS).atOffset(ZoneOffset.UTC))
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
            priceRiseId: String, priceRise: SalesforcePriceRise
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

  private def stubEmailSender(sendMessages: ArrayBuffer[EmailMessage]) = {
    ZLayer.succeed(
      new EmailSender.Service {
        override def sendEmail(message: EmailMessage): ZIO[Any, EmailSenderFailure, Unit] =
          ZIO.effect {
            sendMessages.addOne(message)
            ()
          }.orElseFail(EmailSenderFailure(""))
      }
    )
  }

  test("SalesforcePriceRiseCreateHandler should get records from cohort table and SF") {
    val stubSalesforceClient =
      stubSFClient(
        List(
          SalesforceSubscription(
            expectedSFSubscriptionId,
            expectedSubscriptionName,
            expectedBuyerId
          )
        ),
        List(
          SalesforceContact(
            Id = expectedBuyerId,
            IdentityID__c = Some(expectedIdentityId),
            Email = Some(expectedEmailAddress),
            FirstName = Some(expectedFirstName),
            LastName = Some(expectedLastName),
            MailingAddress = SalesforceAddress(
              street = Some(expectedStreet),
              city = Some(expectedCity),
              state = Some(expectedState),
              postalCode = Some(expectedPostalCode),
              country = Some(expectedCountry),
            )
          )
        )
      )

    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()

    val cohortItem = CohortItem(
      subscriptionName = expectedSubscriptionName,
      processingStage = AmendmentComplete,
      startDate = Some(expectedStartDate),
      currency = Some(expectedCurrency),
      newPrice = Some(expectedNewPrice),
      oldPrice = Some(expectedOldPrice),
      estimatedNewPrice = Some(expectedEstimatedNewPrice),
      billingPeriod = Some(expectedBillingPeriod)
    )

    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)

    val sentMessages = ArrayBuffer[EmailMessage]()

    val stubEmailSender = stubEmailSender(sentMessages)

    assertEquals(
      default.unsafeRunSync(
        NotificationEmailHandler.main
          .provideLayer(
            stubLogging ++ stubCohortTable ++ StubClock.clock ++ stubSalesforceClient ++ stubEmailSender
          )
      ),
      Success(())
    )

    assertEquals(sentMessages.size, 1)
    assertEquals(sentMessages(0).DataExtensionName, "price-rise-email")
    assertEquals(sentMessages(0).SfContactId, expectedBuyerId)
    assertEquals(sentMessages(0).IdentityUserId, Some(expectedIdentityId))
    assertEquals(sentMessages(0).To.Address, expectedEmailAddress)
    assertEquals(sentMessages(0).To.ContactAttributes.AddressLine1, expectedStreet)
    assertEquals(sentMessages(0).To.ContactAttributes.Town, expectedCity)
    assertEquals(sentMessages(0).To.ContactAttributes.County, expectedState)
    assertEquals(sentMessages(0).To.ContactAttributes.Postcode, expectedPostalCode)
    assertEquals(sentMessages(0).To.ContactAttributes.Country, expectedCountry)
    assertEquals(sentMessages(0).To.ContactAttributes.FirstName, expectedFirstName)
    assertEquals(sentMessages(0).To.ContactAttributes.LastName, expectedLastName)
    assertEquals(sentMessages(0).To.ContactAttributes.NewPrice, expectedNewPrice.toString())
    assertEquals(sentMessages(0).To.ContactAttributes.StartDate, expectedStartDate.toString())
    assertEquals(sentMessages(0).To.ContactAttributes.BillingPeriod, expectedBillingPeriod)
  }
}
