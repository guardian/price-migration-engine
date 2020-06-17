package pricemigrationengine.services

import zio.console.Console
import zio.{UIO, ZLayer}

object ConsoleLogging {

  def service(consoleService: Console.Service): Logging.Service = new Logging.Service {
    def info(s: String): UIO[Unit] = consoleService.putStrLn(s"INFO: $s")
    def error(s: String): UIO[Unit] = consoleService.putStrLn(s"ERROR: $s")
  }

  val impl: ZLayer[Console, Nothing, Logging] = ZLayer.fromService(service)
}
