package pricemigrationengine.services

import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import com.amazonaws.services.sqs.model.{SendMessageRequest, SendMessageResult}
import pricemigrationengine.model.{ConfigurationFailure, EmailSenderFailure, Failure}
import pricemigrationengine.model.membershipworkflow.EmailMessage
import zio.{ZIO, ZLayer}
import upickle.default.write

object EmailSenderLive {
  val impl: ZLayer[Logging with EmailSenderConfiguration, EmailSenderFailure, EmailSender] = ZLayer.fromFunctionM { dependencies =>
    (
      for {
        config <- EmailSenderConfiguration
          .emailSenderConfig
          .mapError { error: ConfigurationFailure =>
            EmailSenderFailure(s"Failed to get email sender configuration: $error")
          }
        sqsClient <- ZIO.effect {
          AmazonSQSAsyncClientBuilder.standard().build()
        }.mapError { ex =>
          EmailSenderFailure(s"Failed to create sqs client: ${ex.getMessage}")
        }
        queueUrl <- ZIO.effect {
          sqsClient.getQueueUrl(config.sqsEmailQueueName).getQueueUrl
        }.mapError { ex =>
          EmailSenderFailure(s"Failed to get sqs queue url: ${ex.getMessage}")
        }
      } yield new EmailSender.Service {
        override def sendEmail(message: EmailMessage): ZIO[Any, EmailSenderFailure, Unit] = {
          sendMessage(sqsClient, queueUrl, message)
        }.provide(dependencies)
      }
    ).provide(dependencies)
  }

  private def sendMessage(sqsClient: AmazonSQSAsync, queueUrl: String, message: EmailMessage) = {
    for {
      result <- ZIO.effect {
        sqsClient.sendMessage(
          new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageBody(serialiseMessage(message))
        )
      }.mapError { ex =>
        EmailSenderFailure(s"Failed to send sqs email message for sfContactId ${message.SfContactId}: ${ex.getMessage}")
      }
      _ <- Logging.info(
        s"Successfully sent email for sfContactId ${message.SfContactId} message id: ${result.getMessageId}"
      )
    } yield ()
  }

  def serialiseMessage(message: EmailMessage): String = {
    write(message, indent = 2)
  }
}