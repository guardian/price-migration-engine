package pricemigrationengine

import pricemigrationengine.services.ConsoleLogging
import zio.console

object TestLogging {
  val logging = console.Console.live >>> ConsoleLogging.impl
}