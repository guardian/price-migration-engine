package pricemigrationengine.handlers

import pricemigrationengine.model.TimeFailure
import zio._

import java.time.{Instant, LocalDate, OffsetDateTime}

object Time {

  private val nowHere: ZIO[Clock, TimeFailure, OffsetDateTime] =
    Clock.currentDateTime

  val today: ZIO[Clock, TimeFailure, LocalDate] =
    nowHere.map(_.toLocalDate)

  val thisInstant: ZIO[Clock, TimeFailure, Instant] =
    nowHere.map(_.toInstant)
}
