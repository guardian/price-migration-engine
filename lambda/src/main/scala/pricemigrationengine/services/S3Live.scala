package pricemigrationengine.services

import java.io.InputStream

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import pricemigrationengine.model.S3Failure
import zio.{ZLayer, ZManaged}

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
    }
  }
}
