package pricemigrationengine.handlers

import java.time.LocalDate

import pricemigrationengine.model.TimeFailure
import zio.clock.Clock
import zio.{ZIO, clock}

object Time {

  val today: ZIO[Clock, TimeFailure, LocalDate] =
    clock.currentDateTime
      .bimap(
        e => TimeFailure(s"Extremely unlikely failure of time: $e"),
        _.toLocalDate
      )
}
