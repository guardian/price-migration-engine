package pricemigrationengine.services

import com.amazonaws.services.lambda.runtime.Context
import zio.{UIO, ZIO, ZLayer}

object LambdaLogging {
  def impl(context: Context): ZLayer[Any, Nothing, Logging] = ZLayer.succeed(
    new Logging.Service {
      val logger = context.getLogger
      def info(s: String): UIO[Unit] = ZIO.succeed(logger.log(s"INFO: $s"))
      def error(s: String): UIO[Unit] = ZIO.succeed(logger.log(s"ERROR: $s"))
    }
  )
}
