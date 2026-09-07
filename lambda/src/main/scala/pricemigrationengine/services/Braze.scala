package pricemigrationengine.services

import pricemigrationengine.model.BrazeFailure
import pricemigrationengine.model.membershipworkflow.BrazeMessage
import zio.ZIO

trait Braze {
  def sendMessage(message: BrazeMessage): ZIO[Any, BrazeFailure, Unit]
}

object Braze {
  def sendMessage(message: BrazeMessage): ZIO[Braze, BrazeFailure, Unit] = {
    ZIO.environmentWithZIO(_.get.sendMessage(message))
  }
}
