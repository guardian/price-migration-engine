package pricemigrationengine.services

import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger}
import zio.{UIO, ULayer, ZIO, ZLayer}
import io.circe.generic.auto._
import io.circe.syntax._

object LambdaLogging {
  private case class InfoMessage(
      cohortName: String,
      INFO: String
  )

  private case class ErrorMessage(
      cohortName: String,
      ERROR: String
  )

  def impl(context: Context, cohortName: String): ULayer[Logging] =
    ZLayer.succeed(
      new Logging.Service {
        val logger: LambdaLogger = context.getLogger
        def info(s: String): UIO[Unit] =
          ZIO.succeed(logger.log(InfoMessage(cohortName, s).asJson.dropNullValues.noSpaces))
        def error(s: String): UIO[Unit] =
          ZIO.succeed(logger.log(ErrorMessage(cohortName, s).asJson.dropNullValues.noSpaces))
      }
    )
}
