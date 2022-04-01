package pricemigrationengine.handlers

import java.time.LocalDate
import pricemigrationengine.Fixtures
import zio.{BuildFrom, Chunk, Random, Runtime, UIO, ULayer, ZLayer, ZTraceElement}

import java.util.UUID

class EstimationHandlerTest extends munit.FunSuite {

  private val absoluteEarliestStartDate = LocalDate.of(2020, 6, 2)

  private val random: ULayer[Random] = ZLayer.succeed(
    new Random {

      override def nextBoolean(implicit trace: ZTraceElement): UIO[Boolean] = ???

      override def nextBytes(length: => Int)(implicit trace: ZTraceElement): UIO[Chunk[Byte]] = ???

      override def nextDouble(implicit trace: ZTraceElement): UIO[Double] = ???

      override def nextDoubleBetween(minInclusive: => Double, maxExclusive: => Double)(implicit
          trace: ZTraceElement
      ): UIO[Double] = ???

      override def nextFloat(implicit trace: ZTraceElement): UIO[Float] = ???

      override def nextFloatBetween(minInclusive: => Float, maxExclusive: => Float)(implicit
          trace: ZTraceElement
      ): UIO[Float] = ???

      override def nextGaussian(implicit trace: ZTraceElement): UIO[Double] = ???

      override def nextInt(implicit trace: ZTraceElement): UIO[Int] = ???

      override def nextIntBetween(minInclusive: => Int, maxExclusive: => Int)(implicit trace: ZTraceElement): UIO[Int] =
        UIO.succeed(1)

      override def nextIntBounded(n: => Int)(implicit trace: ZTraceElement): UIO[Int] = ???

      override def nextLong(implicit trace: ZTraceElement): UIO[Long] = ???

      override def nextLongBetween(minInclusive: => Long, maxExclusive: => Long)(implicit
          trace: ZTraceElement
      ): UIO[Long] = ???

      override def nextLongBounded(n: => Long)(implicit trace: ZTraceElement): UIO[Long] = ???

      override def nextPrintableChar(implicit trace: ZTraceElement): UIO[Char] = ???

      override def nextString(length: => Int)(implicit trace: ZTraceElement): UIO[String] = ???

      override def nextUUID(implicit trace: ZTraceElement): UIO[UUID] = ???

      override def setSeed(seed: => Long)(implicit trace: ZTraceElement): UIO[Unit] = ???

      override def shuffle[A, Collection[+Element] <: Iterable[Element]](collection: => Collection[A])(implicit
          bf: zio.BuildFrom[Collection[A], A, Collection[A]],
          trace: ZTraceElement
      ): UIO[Collection[A]] = ???
    }
  )

  private val env = random

  private val runtime = Runtime.default

  test("spreadEarliestStartDate: gives default value for a quarterly subscription") {
    val earliestStartDateCalc = EstimationHandler.spreadEarliestStartDate(
      subscription = Fixtures.subscriptionFromJson("NewspaperVoucher/QuarterlyVoucher/Subscription.json"),
      invoicePreview = Fixtures.invoiceListFromJson("NewspaperVoucher/QuarterlyVoucher/InvoicePreview.json"),
      absoluteEarliestStartDate
    )
    val earliestStartDate = runtime.unsafeRun(earliestStartDateCalc.provideLayer(env))
    assertEquals(earliestStartDate, LocalDate.of(2020, 6, 2))
  }

  test("spreadEarliestStartDate: gives randomised value for a monthly subscription") {
    val earliestStartDateCalc = EstimationHandler.spreadEarliestStartDate(
      subscription = Fixtures.subscriptionFromJson("NewspaperVoucher/Monthly/Subscription.json"),
      invoicePreview = Fixtures.invoiceListFromJson("NewspaperVoucher/Monthly/InvoicePreview.json"),
      absoluteEarliestStartDate
    )
    val earliestStartDate = runtime.unsafeRun(earliestStartDateCalc.provideLayer(env))
    assertEquals(earliestStartDate, LocalDate.of(2020, 7, 2))
  }
}
