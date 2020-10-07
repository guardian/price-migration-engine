package pricemigrationengine.services

import java.io.{File, InputStream}

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.{CannedAccessControlList, PutObjectRequest, PutObjectResult}
import pricemigrationengine.model.S3Failure
import zio.{IO, ZLayer, ZManaged}

import scala.jdk.CollectionConverters._

object S3Live {
  val impl: ZLayer[Logging, Nothing, S3] = ZLayer.fromService { logging =>
    val s3 = AmazonS3ClientBuilder.standard.withRegion(Regions.EU_WEST_1).build

    //noinspection ConvertExpressionToSAM
    new S3.Service {
      override def getObject(s3Location: S3Location): ZManaged[Any, S3Failure, InputStream] = {
        ZManaged
          .makeEffect(
            s3.getObject(s3Location.bucket, s3Location.key).getObjectContent
          ) { objectContent: InputStream => objectContent.close() }
          .mapError(ex => S3Failure(s"Failed to get $s3Location: $ex"))
      }

      override def putObject(
          s3Location: S3Location,
          localFile: File,
          cannedAcl: Option[CannedAccessControlList]
      ): IO[S3Failure, PutObjectResult] =
        IO.effect {
          s3.putObject(
            cannedAcl.foldLeft(
              new PutObjectRequest(
                s3Location.bucket,
                s3Location.key,
                localFile
              )
            ) { (putRequest, cal) =>
              putRequest.withCannedAcl(cal)
            }
          )
        }.mapError(ex => S3Failure(s"Failed to write s3 object $s3Location: ${ex.getMessage}"))

      override def deleteObject(s3Location: S3Location): IO[S3Failure, Unit] =
        (for {
          listing <- IO.effect(s3.listObjects(s3Location.bucket, s3Location.key))
          _ <- IO.foreach_(listing.getObjectSummaries.asScala)(summary =>
            IO.effect(s3.deleteObject(summary.getBucketName, summary.getKey))
              .tap(_ => logging.info(s"Deleted $summary"))
          )
        } yield ()).mapError(ex => S3Failure(s"Failed to delete s3 object $s3Location: ${ex.getMessage}"))
    }
  }
}
