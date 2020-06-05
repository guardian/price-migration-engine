package pricemigrationengine.services

import pricemigrationengine.model.EmailSenderFailure
import pricemigrationengine.model.membershipworkflow.EmailMessage
import zio.{ZIO, ZLayer}

object EmailSenderLive {
  val impl: ZLayer[Logging, Nothing, EmailSender] = ZLayer.fromFunction { logging: Logging =>
    new EmailSender.Service {
      override def sendEmail(message: EmailMessage): ZIO[Any, EmailSenderFailure, Unit] = {
        Logging.info(s"Sending email for ${message.SfContactId}")
      }.provide(logging)
    }
  }
}