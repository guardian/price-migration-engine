package pricemigrationengine.services

import pricemigrationengine.model.S3Failure
import software.amazon.awssdk.services.s3.model.{ObjectCannedACL, PutObjectResponse}
import zio.{IO, Scope, ZIO}

import java.io.{File, InputStream}

case class S3Location(bucket: String, key: String)

trait S3 {
  def getObject(s3Location: S3Location): ZIO[Scope, S3Failure, InputStream]
  def putObject(
      s3Location: S3Location,
      localFile: File,
      cannedAcl: Option[ObjectCannedACL]
  ): IO[S3Failure, PutObjectResponse]
  def deleteObject(s3Location: S3Location): IO[S3Failure, Unit]
}

object S3 {

  def getObject(s3Location: S3Location): ZIO[S3, S3Failure, ZIO[Scope, S3Failure, InputStream]] =
    ZIO.environmentWith(_.get.getObject(s3Location))

  def putObject(
      s3Location: S3Location,
      localFile: File,
      cannedAcl: Option[ObjectCannedACL]
  ): ZIO[S3, S3Failure, PutObjectResponse] =
    ZIO.environmentWithZIO(_.get.putObject(s3Location, localFile, cannedAcl: Option[ObjectCannedACL]))

  def deleteObject(s3Location: S3Location): ZIO[S3, S3Failure, Unit] =
    ZIO.environmentWithZIO(_.get.deleteObject(s3Location))
}
