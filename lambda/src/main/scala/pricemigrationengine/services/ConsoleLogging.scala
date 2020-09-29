package pricemigrationengine.services

import zio.console.Console
import zio.{UIO, URLayer, ZLayer}

object ConsoleLogging {

  val impl: URLayer[Console, Logging] =
    ZLayer.fromService(consoleService =>
      new Logging.Service {
        def info(s: String): UIO[Unit] = consoleService.putStrLn(s"INFO: $s")
        def error(s: String): UIO[Unit] = consoleService.putStrLn(s"ERROR: $s")
      }
    )
}
