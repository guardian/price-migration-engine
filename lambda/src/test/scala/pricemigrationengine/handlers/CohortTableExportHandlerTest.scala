package pricemigrationengine.handlers

import java.io.{File, InputStream}
import java.time._

import com.amazonaws.services.s3.model.PutObjectResult
import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model._
import pricemigrationengine.services._
import pricemigrationengine.{StubClock, TestLogging}
import zio.Exit.Success
import zio.Runtime.default
import zio._
import zio.stream.ZStream

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

class CohortTableExportHandlerTest extends munit.FunSuite {

  def createStubCohortTable(cohortItems: List[CohortItem]) = {
    ZLayer.succeed(
      new CohortTable.Service {
        override def fetch(
            filter: CohortTableFilter,
            beforeDateInclusive: Option[LocalDate]
        ): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = ???

        override def put(cohortItem: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = ???

        override def update(result: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = ???

        override def fetchAll(): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = {
          IO.succeed(ZStream.fromIterable(cohortItems))
        }
      }
    )
  }

  def createStubS3(filesWrittenToS3: ArrayBuffer[(S3Location, String)]): Layer[Nothing, Has[S3.Service]] = ZLayer.succeed(
    new S3.Service {
      override def getObject(s3Location: S3Location): ZManaged[Any, S3Failure, InputStream] = ???

      override def putObject(s3Location: S3Location, file: File): IO[S3Failure, PutObjectResult] =
        for {
          fileContents <- ZIO.effectTotal(Source.fromFile(file, "UTF-8").getLines().mkString("\n"))
          _ <- ZIO.effectTotal(filesWrittenToS3.addOne((s3Location, fileContents)))
        } yield new PutObjectResult()
    }
  )

  val expectedS3ExportBucketName = "export-s3-bucket-name"

  val stubConfig = ZLayer.succeed(
    new ExportConfiguration.Service {
      override val config: IO[ConfigurationFailure, ExportConfig] =
        IO.succeed(ExportConfig(expectedS3ExportBucketName))
    }
  )

  test("CohortTableExportHandler should write cohort items to s3 as CSV") {
    val expectedCohortName = "expected cohort name"
    val cohortSpec = new CohortSpec(expectedCohortName, "", LocalDate.now(), LocalDate.now())
    val uploadedFiles = ArrayBuffer[(S3Location, String)]()
    val stubS3 = createStubS3(uploadedFiles)

    val cohortItem = CohortItem(
      subscriptionName = "subscription 1",
      processingStage = NotificationSendComplete,
      startDate = Some(LocalDate.of(2020,1,1)),
      currency = Some("USD"),
      oldPrice = Some(1.00),
      estimatedNewPrice = Some(2.00),
      billingPeriod = Some("quarter"),
      whenEstimationDone = Some(Instant.parse("2020-01-01T01:01:01Z")),
      salesforcePriceRiseId = Some("salesForcePriceRiseId1"),
      whenSfShowEstimate = Some(Instant.parse("2020-01-02T01:01:01Z")),
      newPrice = Some(3.00),
      newSubscriptionId = Some("zuoraSubId1"),
      whenAmendmentDone = Some(Instant.parse("2020-01-03T01:01:01Z")),
      whenNotificationSent = Some(Instant.parse("2020-01-04T01:01:01Z")),
      whenNotificationSentWrittenToSalesforce = Some(Instant.parse("2020-01-05T01:01:01Z")),
      whenAmendmentWrittenToSalesforce = Some(Instant.parse("2020-01-06T01:01:01Z"))
    )

    val stubCohortTable = createStubCohortTable(List(cohortItem))

    assertEquals(
      default.unsafeRunSync(
        CohortTableDatalakeExportHandler
          .main(cohortSpec)
          .provideLayer(
            TestLogging.logging ++ stubCohortTable ++ stubS3 ++ stubConfig
          )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(1, uploadedFiles.size)
    assertEquals(expectedS3ExportBucketName, uploadedFiles(0)._1.bucket)
    val (actualS3Location, actualFileContents) = uploadedFiles(0)
    assertEquals(expectedS3ExportBucketName, actualS3Location.bucket)
    assertEquals(s"/data/${expectedCohortName}.csv", actualS3Location.path)
    assertEquals(
      """"cohortName","subscriptionName","processingStage","startDate","currency","oldPrice","estimatedNewPrice","billingPeriod","whenEstimationDone","salesforcePriceRiseId","whenSfShowEstimate","newPrice","newSubscriptionId","whenAmendmentDone","whenNotificationSent","whenNotificationSentWrittenToSalesforce","whenAmendmentWrittenToSalesforce"
        |"expected cohort name","subscription 1","NotificationSendComplete","2020-01-01","USD","1.0","2.0","quarter","2020-01-01T01:01:01Z","salesForcePriceRiseId1","2020-01-02T01:01:01Z","3.0","zuoraSubId1","2020-01-03T01:01:01Z","2020-01-04T01:01:01Z","2020-01-05T01:01:01Z","2020-01-06T01:01:01Z"""".stripMargin,
      actualFileContents
    )
  }
  test("CohortTableExportHandler should write cohort items with missing optional values to s3 as CSV") {
    val expectedCohortName = "expected cohort name"
    val cohortSpec = new CohortSpec(expectedCohortName, "", LocalDate.now(), LocalDate.now())
    val uploadedFiles = ArrayBuffer[(S3Location, String)]()
    val stubS3 = createStubS3(uploadedFiles)

    val cohortItem = CohortItem(
      subscriptionName = "subscription 2",
      processingStage = ReadyForEstimation,
    )

    val stubCohortTable = createStubCohortTable(List(cohortItem))

    assertEquals(
      default.unsafeRunSync(
        CohortTableDatalakeExportHandler
          .main(cohortSpec)
          .provideLayer(
            TestLogging.logging ++ stubCohortTable ++ stubS3 ++ stubConfig
          )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(1, uploadedFiles.size)
    assertEquals(expectedS3ExportBucketName, uploadedFiles(0)._1.bucket)
    val (actualS3Location, actualFileContents) = uploadedFiles(0)
    assertEquals(expectedS3ExportBucketName, actualS3Location.bucket)
    assertEquals(s"/data/${expectedCohortName}.csv", actualS3Location.path)
    assertEquals(
      """"cohortName","subscriptionName","processingStage","startDate","currency","oldPrice","estimatedNewPrice","billingPeriod","whenEstimationDone","salesforcePriceRiseId","whenSfShowEstimate","newPrice","newSubscriptionId","whenAmendmentDone","whenNotificationSent","whenNotificationSentWrittenToSalesforce","whenAmendmentWrittenToSalesforce"
        |"expected cohort name","subscription 2","ReadyForEstimation","","","","","","","","","","","","","",""""".stripMargin,
      actualFileContents
    )
  }
}
