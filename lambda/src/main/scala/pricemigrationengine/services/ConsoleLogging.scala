package pricemigrationengine.services

import zio.{Console, UIO, URLayer, ZIO, ZLayer}

object ConsoleLogging {

  val impl: URLayer[Console, Logging] =
    ZLayer.fromZIO(
      for {
        console <- ZIO.service[Console]
      } yield new Logging.Service {
        /*
         * If putStrLn fails, an exception will be thrown.
         * TODO: Fail with a LoggingFailure instead.
         *  Then all services that rely on logging will need changes to their return type
         *  so that they return a Failure on error rather than a more specific Failure type.
         */
        def info(s: String): UIO[Unit] = console.printLine(s"INFO: $s").orDie
        def error(s: String): UIO[Unit] = console.printLine(s"ERROR: $s").orDie
      }
    )
}
