package pricemigrationengine.services

import pricemigrationengine.model.S3Failure
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.{
  DeleteObjectRequest,
  GetObjectRequest,
  ListObjectsRequest,
  ObjectCannedACL,
  PutObjectRequest,
  PutObjectResponse,
  S3Object
}
import zio.{IO, Scope, ZIO, ZLayer}

import java.io.{File, InputStream}
import scala.jdk.CollectionConverters._

object S3Live {

  val impl: ZLayer[Logging, Nothing, S3] = ZLayer.fromZIO(
    for {
      logging <- ZIO.service[Logging]
    } yield {
      val s3 = AwsClient.s3
      new S3 {

        override def getObject(s3Location: S3Location): ZIO[Scope, S3Failure, InputStream] = {
          val getObjectRequest = GetObjectRequest.builder.bucket(s3Location.bucket).key(s3Location.key).build()
          ZIO
            .fromAutoCloseable(ZIO.attempt(s3.getObject(getObjectRequest)))
            .mapError(ex => S3Failure(s"Failed to get $s3Location: $ex"))
        }

        override def putObject(
            s3Location: S3Location,
            localFile: File,
            cannedAcl: Option[ObjectCannedACL]
        ): IO[S3Failure, PutObjectResponse] =
          ZIO
            .attempt {
              val requestWithoutAcl = PutObjectRequest.builder.bucket(s3Location.bucket).key(s3Location.key)
              val putObjectRequest = cannedAcl.fold(requestWithoutAcl)(requestWithoutAcl.acl).build()
              val requestBody = RequestBody.fromFile(localFile)
              s3.putObject(putObjectRequest, requestBody)
            }
            .mapError(ex => S3Failure(s"Failed to write s3 object $s3Location: ${ex.getMessage}"))

        override def deleteObject(s3Location: S3Location): IO[S3Failure, Unit] = {
          val listObjectsRequest = ListObjectsRequest.builder.bucket(s3Location.bucket).prefix(s3Location.key).build()
          def deleteObjectRequest(s3Object: S3Object) =
            DeleteObjectRequest.builder.bucket(s3Location.bucket).key(s3Object.key).build()
          (for {
            listObjectsResponse <- ZIO.attempt(s3.listObjects(listObjectsRequest))
            _ <- ZIO.foreachDiscard(listObjectsResponse.contents.asScala)(obj =>
              ZIO.attempt(s3.deleteObject(deleteObjectRequest(obj))) <* logging.info(s"Deleted $obj")
            )
          } yield ()).mapError(ex => S3Failure(s"Failed to delete s3 object $s3Location: ${ex.getMessage}"))
        }
      }
    }
  )
}
