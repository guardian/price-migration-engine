package pricemigrationengine.services

import build.BuildInfo.buildNumber
import zio.{Console, UIO, ULayer, ZLayer}

object ConsoleLogging {

  def impl(cohortName: String): ULayer[Logging] =
    ZLayer.succeed(new Logging {

      private val logPrefix = s"CohortName: $cohortName, BuildNumber: $buildNumber"

      override def info(s: String): UIO[Unit] = Console.printLine(s"$logPrefix, INFO: $s").orDie

      override def error(s: String): UIO[Unit] = Console.printLine(s"$logPrefix, ERROR: $s").orDie
    })
}
