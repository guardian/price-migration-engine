package pricemigrationengine.services

import pricemigrationengine.model.EmailSenderFailure
import pricemigrationengine.model.membershipworkflow.EmailMessage
import zio.ZIO

object EmailSender {
  trait Service {
    def sendEmail(message: EmailMessage): ZIO[Any, EmailSenderFailure, Unit]
  }

  def sendEmail(message: EmailMessage): ZIO[EmailSender, EmailSenderFailure, Unit] = {
    ZIO.environmentWithZIO(_.get.sendEmail(message))
  }
}
