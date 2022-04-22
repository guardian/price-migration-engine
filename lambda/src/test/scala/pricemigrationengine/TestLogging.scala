package pricemigrationengine

import pricemigrationengine.services.ConsoleLogging
import zio.Console

object TestLogging {
  val logging = Console.live >>> ConsoleLogging.impl("TestCohort")
}
