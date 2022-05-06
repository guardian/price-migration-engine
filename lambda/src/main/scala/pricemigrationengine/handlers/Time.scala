package pricemigrationengine.handlers

import pricemigrationengine.model.TimeFailure
import zio._

import java.time.{Instant, LocalDate, OffsetDateTime}

object Time {

  private val nowHere: IO[TimeFailure, OffsetDateTime] =
    Clock.currentDateTime

  val today: IO[TimeFailure, LocalDate] =
    nowHere.map(_.toLocalDate)

  val thisInstant: IO[TimeFailure, Instant] =
    nowHere.map(_.toInstant)
}
