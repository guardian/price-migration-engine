package pricemigrationengine.services

import pricemigrationengine.model.EmailSenderFailure
import pricemigrationengine.model.membershipworkflow.BrazeMessage
import zio.ZIO

trait EmailSender {
  def sendEmail(message: BrazeMessage): ZIO[Any, EmailSenderFailure, Unit]
}

object EmailSender {
  def sendEmail(message: BrazeMessage): ZIO[EmailSender, EmailSenderFailure, Unit] = {
    ZIO.environmentWithZIO(_.get.sendEmail(message))
  }
}
