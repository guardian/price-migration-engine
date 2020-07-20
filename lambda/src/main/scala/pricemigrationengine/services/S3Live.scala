package pricemigrationengine.services

import java.io.{File, InputStream}

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.PutObjectResult
import pricemigrationengine.model.S3Failure
import zio.{IO, ZLayer, ZManaged}

object S3Live {
  val impl: ZLayer[Logging, Nothing, S3] = ZLayer.fromFunction { dependencies =>
    val s3 = AmazonS3ClientBuilder.standard.withRegion(Regions.EU_WEST_1).build

    new S3.Service {
      override def getObject(s3Location: S3Location): ZManaged[Any, S3Failure, InputStream] = {
        ZManaged.makeEffect(
          s3
            .getObject(s3Location.bucket, s3Location.path)
            .getObjectContent()
        ) { objectContent: InputStream =>
          objectContent.close()
        }.mapError(ex => S3Failure(s"Failed to get $s3Location: $ex" ))
      }

      override def putObject(s3Location: S3Location, localFile: File): IO[S3Failure, PutObjectResult] =
        IO.effect(
          s3.putObject(s3Location.bucket, s3Location.path, localFile)
        ).mapError(
          ex => S3Failure(s"Failed to write s3 object $s3Location: ${ex.getMessage}")
        )
    }
  }
}
