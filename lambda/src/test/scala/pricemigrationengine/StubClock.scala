package pricemigrationengine

import zio.{Clock, IO, Scheduler, UIO, ZLayer, ZTraceElement}

import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit

object StubClock {
  val expectedCurrentTime = Instant.parse("2020-05-21T15:16:37Z")
  val clock = ZLayer.succeed(
    new Clock {

      override def currentTime(unit: => TimeUnit)(implicit trace: ZTraceElement): UIO[Long] = ???

      override def currentDateTime(implicit trace: ZTraceElement): UIO[OffsetDateTime] =
        IO.succeed(expectedCurrentTime.atOffset(ZoneOffset.of("-08:00")))

      override def instant(implicit trace: ZTraceElement): UIO[Instant] = ???

      override def localDateTime(implicit trace: ZTraceElement): UIO[LocalDateTime] = ???

      override def nanoTime(implicit trace: ZTraceElement): UIO[Long] = ???

      override def scheduler(implicit trace: ZTraceElement): UIO[Scheduler] = ???

      override def sleep(duration: => zio.Duration)(implicit trace: ZTraceElement): UIO[Unit] = ???
    }
  )
}
