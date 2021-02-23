package pricemigrationengine

import java.time.{DateTimeException, Instant, OffsetDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit

import zio.{IO, UIO, ZLayer}
import zio.clock.Clock
import zio.duration.Duration

object StubClock {
  val expectedCurrentTime = Instant.parse("2020-05-21T15:16:37Z")
  val clock = ZLayer.succeed(
    new Clock.Service {
      override def currentTime(unit: TimeUnit): UIO[Long] = ???
      override def currentDateTime: IO[DateTimeException, OffsetDateTime] =
        IO.succeed(expectedCurrentTime.atOffset(ZoneOffset.of("-08:00")))
      override def nanoTime: UIO[Long] = ???
      override def sleep(duration: Duration): UIO[Unit] = ???
    }
  )
}
