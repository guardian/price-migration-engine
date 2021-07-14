package pricemigrationengine.services

import zio.console.Console
import zio.{UIO, URLayer, ZLayer}

object ConsoleLogging {

  val impl: URLayer[Console, Logging] =
    ZLayer.fromService(consoleService =>
      new Logging.Service {
        /*
         * If putStrLn fails, an exception will be thrown.
         * TODO: Fail with a LoggingFailure instead.
         *  Then all services that rely on logging will need changes to their return type
         *  so that they return a Failure on error rather than a more specific Failure type.
         */
        def info(s: String): UIO[Unit] = consoleService.putStrLn(s"INFO: $s").orDie
        def error(s: String): UIO[Unit] = consoleService.putStrLn(s"ERROR: $s").orDie
      }
    )
}
