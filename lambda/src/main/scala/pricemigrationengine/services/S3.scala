package pricemigrationengine.services

import java.io.InputStream

import pricemigrationengine.model.S3Failure
import zio.{ZIO, ZManaged}

case class S3Location(bucket: String, path: String)

object S3 {
  trait Service {
    def getObject(s3Location: S3Location) : ZManaged[Any, S3Failure, InputStream]
  }

  def getObject(s3Location: S3Location): ZIO[S3, S3Failure, ZManaged[Any, S3Failure, InputStream]] =
    ZIO.access(_.get.getObject(s3Location))
}
