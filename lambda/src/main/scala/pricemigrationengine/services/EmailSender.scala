package pricemigrationengine.services

import pricemigrationengine.model.EmailSenderFailure
import pricemigrationengine.model.membershipworkflow.EmailMessage
import zio.ZIO

trait EmailSender {
  def sendEmail(message: EmailMessage): ZIO[Any, EmailSenderFailure, Unit]
}

object EmailSender {
  def sendEmail(message: EmailMessage): ZIO[EmailSender, EmailSenderFailure, Unit] = {
    ZIO.environmentWithZIO(_.get.sendEmail(message))
  }
}
