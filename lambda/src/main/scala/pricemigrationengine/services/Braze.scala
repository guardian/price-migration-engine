package pricemigrationengine.services

import pricemigrationengine.model.EmailSenderFailure
import pricemigrationengine.model.membershipworkflow.BrazeMessage
import zio.ZIO

trait Braze {
  def sendMessage(message: BrazeMessage): ZIO[Any, EmailSenderFailure, Unit]
}

object Braze {
  def sendMessage(message: BrazeMessage): ZIO[Braze, EmailSenderFailure, Unit] = {
    ZIO.environmentWithZIO(_.get.sendMessage(message))
  }
}
