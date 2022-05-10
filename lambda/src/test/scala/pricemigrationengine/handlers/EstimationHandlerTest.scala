package pricemigrationengine.handlers

import pricemigrationengine.Fixtures
import zio.{Chunk, Random, Runtime, Trace, UIO, ZEnv, ZIO}

import java.time.LocalDate
import java.util.UUID

class EstimationHandlerTest extends munit.FunSuite {

  private val absoluteEarliestStartDate = LocalDate.of(2020, 6, 2)

  private val random =
    new Random {

      override def nextBoolean(implicit trace: Trace): UIO[Boolean] = ???

      override def nextBytes(length: => Int)(implicit trace: Trace): UIO[Chunk[Byte]] = ???

      override def nextDouble(implicit trace: Trace): UIO[Double] = ???

      override def nextDoubleBetween(minInclusive: => Double, maxExclusive: => Double)(implicit
          trace: Trace
      ): UIO[Double] = ???

      override def nextFloat(implicit trace: Trace): UIO[Float] = ???

      override def nextFloatBetween(minInclusive: => Float, maxExclusive: => Float)(implicit
          trace: Trace
      ): UIO[Float] = ???

      override def nextGaussian(implicit trace: Trace): UIO[Double] = ???

      override def nextInt(implicit trace: Trace): UIO[Int] = ???

      override def nextIntBetween(minInclusive: => Int, maxExclusive: => Int)(implicit trace: Trace): UIO[Int] =
        ZIO.succeed(1)

      override def nextIntBounded(n: => Int)(implicit trace: Trace): UIO[Int] = ???

      override def nextLong(implicit trace: Trace): UIO[Long] = ???

      override def nextLongBetween(minInclusive: => Long, maxExclusive: => Long)(implicit
          trace: Trace
      ): UIO[Long] = ???

      override def nextLongBounded(n: => Long)(implicit trace: Trace): UIO[Long] = ???

      override def nextPrintableChar(implicit trace: Trace): UIO[Char] = ???

      override def nextString(length: => Int)(implicit trace: Trace): UIO[String] = ???

      override def nextUUID(implicit trace: Trace): UIO[UUID] = ???

      override def setSeed(seed: => Long)(implicit trace: Trace): UIO[Unit] = ???

      override def shuffle[A, Collection[+Element] <: Iterable[Element]](
          collection: => Collection[A]
      )(implicit bf: zio.BuildFrom[Collection[A], A, Collection[A]], trace: Trace): UIO[Collection[A]] = ???
    }

  private def withStubRandom[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
    ZEnv.services.locallyWith(_.add(random))(zio)

  private val runtime = Runtime.default

  test("spreadEarliestStartDate: gives default value for a quarterly subscription") {
    val earliestStartDateCalc = withStubRandom(
      EstimationHandler.spreadEarliestStartDate(
        subscription = Fixtures.subscriptionFromJson("NewspaperVoucher/QuarterlyVoucher/Subscription.json"),
        invoicePreview = Fixtures.invoiceListFromJson("NewspaperVoucher/QuarterlyVoucher/InvoicePreview.json"),
        absoluteEarliestStartDate
      )
    )
    val earliestStartDate = runtime.unsafeRun(earliestStartDateCalc)
    assertEquals(earliestStartDate, LocalDate.of(2020, 6, 2))
  }

  test("spreadEarliestStartDate: gives randomised value for a monthly subscription") {
    val earliestStartDateCalc = withStubRandom(
      EstimationHandler.spreadEarliestStartDate(
        subscription = Fixtures.subscriptionFromJson("NewspaperVoucher/Monthly/Subscription.json"),
        invoicePreview = Fixtures.invoiceListFromJson("NewspaperVoucher/Monthly/InvoicePreview.json"),
        absoluteEarliestStartDate
      )
    )
    val earliestStartDate = runtime.unsafeRun(earliestStartDateCalc)
    assertEquals(earliestStartDate, LocalDate.of(2020, 7, 2))
  }
}
