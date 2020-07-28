package pricemigrationengine.services

import java.io.{File, InputStream}

import com.amazonaws.services.s3.model.{CannedAccessControlList, PutObjectResult}
import pricemigrationengine.model.S3Failure
import zio.{IO, ZIO, ZManaged}
case class S3Location(bucket: String, key: String)

object S3 {
  trait Service {
    def getObject(s3Location: S3Location): ZManaged[Any, S3Failure, InputStream]
    def putObject(s3Location: S3Location, localFile: File, cannedAcl: Option[CannedAccessControlList]): IO[S3Failure, PutObjectResult]
  }

  def getObject(s3Location: S3Location): ZIO[S3, S3Failure, ZManaged[Any, S3Failure, InputStream]] =
    ZIO.access(_.get.getObject(s3Location))

  def putObject(s3Location: S3Location, localFile: File, cannedAcl: Option[CannedAccessControlList]): ZIO[S3, S3Failure, PutObjectResult] =
    ZIO.accessM(_.get.putObject(s3Location, localFile, cannedAcl: Option[CannedAccessControlList]))
}
