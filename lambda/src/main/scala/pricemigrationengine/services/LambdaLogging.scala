package pricemigrationengine.services

import build.BuildInfo.buildNumber
import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger}
import zio.{UIO, ULayer, ZIO, ZLayer}

object LambdaLogging {
  def impl(context: Context, cohortName: String): ULayer[Logging] =
    ZLayer.succeed(
      new Logging {
        val logger: LambdaLogger = context.getLogger
        override def info(message: String): UIO[Unit] =
          ZIO.succeed(logger.log(s"(buildNumber: ${buildNumber}, cohortName: ${cohortName}) INFO: ${message}"))
        override def error(message: String): UIO[Unit] =
          ZIO.succeed(logger.log(s"(buildNumber: ${buildNumber}, cohortName: ${cohortName}) ERROR: ${message}"))
      }
    )
}
