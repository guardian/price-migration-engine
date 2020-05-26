package pricemigrationengine.services

import java.io.InputStream

import pricemigrationengine.model.S3ZIOFailure
import zio.{ZIO, ZManaged}

case class S3Location(bucket: String, path: String)

object S3ZIO {
  trait Service {
    def getObject(s3Location: S3Location) : ZManaged[Any, S3ZIOFailure, InputStream]
  }

  def getObject(s3Location: S3Location): ZIO[S3ZIO, S3ZIOFailure, ZManaged[Any, S3ZIOFailure, InputStream]] =
    ZIO.access(_.get.getObject(s3Location))
}
