package pricemigrationengine.services

import zio.console.Console
import zio.{UIO, ZLayer}

object ConsoleLogging {
  val impl: ZLayer[Console, Nothing, Logging] = ZLayer.fromService(
    console =>
      new Logging.Service {
        def info(s: String): UIO[Unit] = console.putStrLn(s"INFO: $s")
        def error(s: String): UIO[Unit] = console.putStrLn(s"ERROR: $s")
    }
  )
}
