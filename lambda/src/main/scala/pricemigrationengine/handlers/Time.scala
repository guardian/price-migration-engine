package pricemigrationengine.handlers

import java.time.{Instant, LocalDate, OffsetDateTime}

import pricemigrationengine.model.TimeFailure
import zio.clock.Clock
import zio.{ZIO, clock}

object Time {

  private val nowHere: ZIO[Clock, TimeFailure, OffsetDateTime] =
    clock.currentDateTime.mapError(e => TimeFailure(s"Extremely unlikely failure of time: $e"))

  val today: ZIO[Clock, TimeFailure, LocalDate] =
    nowHere.map(_.toLocalDate)

  val thisInstant: ZIO[Clock, TimeFailure, Instant] =
    nowHere.map(_.toInstant)
}
