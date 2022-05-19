package pricemigrationengine

import zio.{Clock, Scheduler, Trace, UIO, ZEnv, ZIO}

import java.time
import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit

object StubClock {
  val expectedCurrentTime: Instant = Instant.parse("2020-05-21T15:16:37Z")
  private val clock: Clock = new Clock {

    override def currentTime(unit: => TimeUnit)(implicit trace: Trace): UIO[Long] = ???

    override def currentDateTime(implicit trace: Trace): UIO[OffsetDateTime] =
      ZIO.succeed(expectedCurrentTime.atOffset(ZoneOffset.of("-08:00")))

    override def instant(implicit trace: Trace): UIO[Instant] =
      ZIO.succeed(expectedCurrentTime)

    override def localDateTime(implicit trace: Trace): UIO[LocalDateTime] = ???

    override def nanoTime(implicit trace: Trace): UIO[Long] = ???

    override def scheduler(implicit trace: Trace): UIO[Scheduler] = ???

    override def sleep(duration: => zio.Duration)(implicit trace: Trace): UIO[Unit] = ???

    override def javaClock(implicit trace: Trace): UIO[time.Clock] = ???
  }

  private def withClock[R, E, A](clock: Clock)(zio: ZIO[R, E, A]) =
    ZEnv.services.locallyWith(_.add(clock))(zio)

  def withStubClock[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
    withClock(clock)(zio)
}
