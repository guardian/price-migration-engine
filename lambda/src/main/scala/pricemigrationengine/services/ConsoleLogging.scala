package pricemigrationengine.services

import zio.{Console, UIO, ULayer, ZLayer}

object ConsoleLogging {

  def impl(cohortName: String): ULayer[Logging] =
    ZLayer.succeed(new Logging {
      override def info(s: String): UIO[Unit] = Console.printLine(s"cohortName: $cohortName, INFO: $s").orDie
      override def error(s: String): UIO[Unit] = Console.printLine(s"cohortName: $cohortName, ERROR: $s").orDie
    })
}
