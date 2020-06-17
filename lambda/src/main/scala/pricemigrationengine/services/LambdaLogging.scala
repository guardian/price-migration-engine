package pricemigrationengine.services

import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger}
import zio.{UIO, ZIO}

object LambdaLogging {

  def service(context: Context): Logging.Service = new Logging.Service {
    val logger: LambdaLogger = context.getLogger
    def info(s: String): UIO[Unit] = ZIO.succeed(logger.log(s"INFO: $s"))
    def error(s: String): UIO[Unit] = ZIO.succeed(logger.log(s"ERROR: $s"))
  }
}
