package pricemigrationengine

import pricemigrationengine.services.ConsoleLogging

object TestLogging {
  val logging = ConsoleLogging.impl("TestCohort")
}
