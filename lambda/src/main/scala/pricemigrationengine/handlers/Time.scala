package pricemigrationengine.handlers

import zio._

import java.time.{Instant, LocalDate, OffsetDateTime}

object Time {

  private val nowHere: UIO[OffsetDateTime] =
    Clock.currentDateTime

  val today: UIO[LocalDate] =
    nowHere.map(_.toLocalDate)

  val thisInstant: UIO[Instant] =
    nowHere.map(_.toInstant)
}
