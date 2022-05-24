package pricemigrationengine.services

import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger}
import zio.{UIO, ULayer, ZIO, ZLayer}
import upickle.default.{ReadWriter, macroRW, write}

object LambdaLogging {
  private case class InfoMessage(
      CohortName: String,
      INFO: String
  )

  private case class ErrorMessage(
      CohortName: String,
      ERROR: String
  )

  private implicit val rwInfo: ReadWriter[InfoMessage] = macroRW
  private implicit val rwError: ReadWriter[ErrorMessage] = macroRW

  def impl(context: Context, cohortName: String): ULayer[Logging] =
    ZLayer.succeed(
      new Logging {
        val logger: LambdaLogger = context.getLogger
        override def info(s: String): UIO[Unit] =
          ZIO.succeed(logger.log(write(InfoMessage(cohortName, s))))
        override def error(s: String): UIO[Unit] =
          ZIO.succeed(logger.log(write(ErrorMessage(cohortName, s))))
      }
    )
}
