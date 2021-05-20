package pricemigrationengine.handlers

import pricemigrationengine.model._
import pricemigrationengine.services._
import pricemigrationengine.{StubClock, TestLogging}
import software.amazon.awssdk.services.s3.model.{ObjectCannedACL, PutObjectResponse}
import zio.Exit.Success
import zio.Runtime.default
import zio._
import zio.stream.ZStream

import java.io.{File, InputStream}
import java.time.LocalDate
import scala.collection.mutable.ArrayBuffer

class SubscriptionIdUploadHandlerTest extends munit.FunSuite {
  test("SubscriptionIdUploadHandler should get subscriptions from s3 and write to cohort table") {
    val stubConfiguration = ZLayer.succeed(
      new StageConfiguration.Service {
        override val config: IO[ConfigurationFailure, StageConfig] =
          IO.succeed(StageConfig("DEV"))
      }
    )

    val subscriptionsWrittenToCohortTable = ArrayBuffer[CohortItem]()

    val stubCohortTable = ZLayer.succeed(
      new CohortTable.Service {
        override def fetch(
            filter: CohortTableFilter,
            beforeDateInclusive: Option[LocalDate]
        ): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = ???
        override def update(result: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = ???
        override def fetchAll(): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = ???
        override def create(cohortItem: CohortItem): ZIO[Any, Failure, Unit] =
          IO.effect {
            subscriptionsWrittenToCohortTable.addOne(cohortItem)
            ()
          }.orElseFail(CohortUpdateFailure(""))
      }
    )

    val stubS3: Layer[Nothing, Has[S3.Service]] = ZLayer.succeed(
      new S3.Service {
        def loadTestResource(path: String) = {
          ZManaged
            .makeEffect(getClass.getResourceAsStream(path)) { stream =>
              stream.close()
            }
            .mapError(ex => S3Failure(s"Failed to load test resource: $ex"))
        }

        override def getObject(s3Location: S3Location): ZManaged[Any, S3Failure, InputStream] =
          s3Location match {
            case S3Location("price-migration-engine-dev", "cohortName/excluded-subscription-ids.csv") =>
              loadTestResource("/SubscriptionExclusions.csv")
            case S3Location("price-migration-engine-dev", "cohortName/salesforce-subscription-id-report.csv") =>
              loadTestResource("/SubscriptionIds.csv")
          }

        override def putObject(
            s3Location: S3Location,
            file: File,
            cannedAccessControlList: Option[ObjectCannedACL]
        ): IO[S3Failure, PutObjectResponse] = ???

        override def deleteObject(s3Location: S3Location): IO[S3Failure, Unit] = ZIO.succeed(())
      }
    )

    assertEquals(
      default.unsafeRunSync(
        SubscriptionIdUploadHandler
          .main(
            CohortSpec(
              cohortName = "cohortName",
              brazeCampaignName = "cmp123",
              importStartDate = LocalDate.of(2020, 1, 1),
              earliestPriceMigrationStartDate = LocalDate.of(2020, 1, 1)
            )
          )
          .provideLayer(
            TestLogging.logging ++ stubConfiguration ++ stubCohortTable ++ stubS3 ++ StubClock.clock
          )
      ),
      Success(HandlerOutput(isComplete = true))
    )
    assertEquals(subscriptionsWrittenToCohortTable.size, 2)
    assertEquals(subscriptionsWrittenToCohortTable(0).subscriptionName, "A-S123456")
    assertEquals(subscriptionsWrittenToCohortTable(1).subscriptionName, "654321")
  }
}
