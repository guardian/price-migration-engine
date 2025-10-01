package pricemigrationengine.handlers

import pricemigrationengine.TestLogging
import pricemigrationengine.model._
import pricemigrationengine.services._
import pricemigrationengine.model.Runner
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
    val stubConfiguration = ZLayer.succeed(StageConfig("DEV"))

    val subscriptionsWrittenToCohortTable = ArrayBuffer[CohortItem]()

    val stubCohortTable = ZLayer.succeed(
      new CohortTable {
        override def fetch(
            filter: CohortTableFilter,
            beforeDateInclusive: Option[LocalDate]
        ): ZStream[Any, CohortFetchFailure, CohortItem] = ???
        override def update(result: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = ???
        override def fetchAll(): ZStream[Any, CohortFetchFailure, CohortItem] = ???
        override def create(cohortItem: CohortItem): ZIO[Any, Failure, Unit] =
          ZIO
            .attempt {
              subscriptionsWrittenToCohortTable.addOne(cohortItem)
              ()
            }
            .orElseFail(CohortUpdateFailure(""))
      }
    )

    val stubS3: Layer[Nothing, S3] = ZLayer.succeed(new S3 {

      def loadTestResource(path: String): ZIO[Scope, S3Failure, InputStream] = {
        ZIO
          .fromAutoCloseable(ZIO.attempt(getClass.getResourceAsStream(path)))
          .mapError(ex => S3Failure(s"Failed to load test resource: $ex"))
      }

      override def getObject(s3Location: S3Location): ZIO[Scope, S3Failure, InputStream] =
        s3Location match {
          case S3Location("price-migration-engine-dev", "cohortName/subscription-numbers.csv") =>
            loadTestResource("/Core/cohort-files/subscription-numbers.csv")
          case _ => ZIO.fail(S3Failure(s"Unexpected location: $s3Location"))
        }

      override def putObject(
          s3Location: S3Location,
          file: File,
          cannedAccessControlList: Option[ObjectCannedACL]
      ): IO[S3Failure, PutObjectResponse] = ???

      override def deleteObject(s3Location: S3Location): IO[S3Failure, Unit] = ZIO.succeed(())
    })

    assertEquals(
      Runner.unsafeRunSync(default)(
        SubscriptionIdUploadHandler
          .main(
            CohortSpec(
              cohortName = "cohortName",
              brazeName = "cmp123",
              earliestAmendmentEffectiveDate = LocalDate.of(2020, 1, 1)
            )
          )
          .provideLayer(
            TestLogging.logging ++ stubConfiguration ++ stubCohortTable ++ stubS3
          )
      ),
      Success(HandlerOutput(isComplete = true))
    )
    assertEquals(subscriptionsWrittenToCohortTable.size, 3)
    assertEquals(subscriptionsWrittenToCohortTable(2).subscriptionName, "subscriptionId3")
  }
}
