package pricemigrationengine.libs

import zio._

object Runner {

  def unsafeRun[R, E, A](runtime: Runtime[R])(zio: ZIO[R, E, A]): A =
    Unsafe.unsafe(implicit u => runtime.unsafe.run(zio).getOrThrowFiberFailure())

  def unsafeRunSync[R, E, A](runtime: Runtime[R])(zio: ZIO[R, E, A]): Exit[E, A] =
    Unsafe.unsafe(implicit u => runtime.unsafe.run(zio))
}
