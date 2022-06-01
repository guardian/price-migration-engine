package pricemigrationengine.services

import build.BuildInfo.buildNumber
import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger}
import upickle.default.{ReadWriter, macroRW, write}
import zio.{UIO, ULayer, ZIO, ZLayer}

object LambdaLogging {
  private case class InfoMessage(
      CohortName: String,
      BuildNumber: String,
      INFO: String
  )

  private case class ErrorMessage(
      CohortName: String,
      BuildNumber: String,
      ERROR: String
  )

  private implicit val rwInfo: ReadWriter[InfoMessage] = macroRW
  private implicit val rwError: ReadWriter[ErrorMessage] = macroRW

  def impl(context: Context, cohortName: String): ULayer[Logging] =
    ZLayer.succeed(
      new Logging {
        val logger: LambdaLogger = context.getLogger
        override def info(s: String): UIO[Unit] =
          ZIO.succeed(logger.log(write(InfoMessage(cohortName, buildNumber, s))))
        override def error(s: String): UIO[Unit] =
          ZIO.succeed(logger.log(write(ErrorMessage(cohortName, buildNumber, s))))
      }
    )
}
