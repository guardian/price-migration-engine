package pricemigrationengine.services

import pricemigrationengine.model.{ConfigurationFailure, EmailSenderFailure, Failure}
import pricemigrationengine.model.membershipworkflow.EmailMessage
import zio.{ZIO, ZLayer}

object EmailSenderLive {
  val impl: ZLayer[Logging with EmailSenderConfiguration, EmailSenderFailure, EmailSender] = ZLayer.fromFunction { dependencies =>
    new EmailSender.Service {
      override def sendEmail(message: EmailMessage): ZIO[Any, EmailSenderFailure, Unit] = {
        for {
          config <- EmailSenderConfiguration
            .emailSenderConfig
            .mapError { error: ConfigurationFailure =>
              EmailSenderFailure(s"Failed to get email sender configuration: $error")
            }
          _ <- Logging.info(s"Sending email for ${message.SfContactId}")
        } yield ()
      }.provide(dependencies)
    }
  }
}