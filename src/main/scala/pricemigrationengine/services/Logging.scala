package pricemigrationengine.services

import zio.{UIO, ZIO}

object Logging {

  trait Service {
    def info(s: String): UIO[Unit]
    def error(s: String): UIO[Unit]
  }

  def info(s: String): ZIO[Logging, Nothing, Unit] =
    ZIO.accessM(_.get.info(s))

  def error(s: String): ZIO[Logging, Nothing, Unit] =
    ZIO.accessM(_.get.error(s))
}
