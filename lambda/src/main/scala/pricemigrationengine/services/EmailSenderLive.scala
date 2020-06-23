package pricemigrationengine.services

import com.amazonaws.services.sqs.model.SendMessageRequest
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import pricemigrationengine.model.EmailSenderFailure
import pricemigrationengine.model.membershipworkflow.EmailMessage
import upickle.default.write
import zio.{ZIO, ZLayer}

/**
 * The email sender takes the information in the supplied EmailMessage object and sends it to the membership-workflow
 * app via the contribution-thanks sqs queue.
 *
 * Membership workflow will then trigger the braze campaign associated with the DataExtensionName in the sqs message.
 *
 * In the case of the notifications sent by the price migration engine braze is configured to trigger a 'web-hook'.
 *
 * The web hook is essentially an api call to Latcham our direct mail partner, who will use the information in the
 * web hook to print a physical letter notifying the customer of the price rise and send it to the customer.
 */
object EmailSenderLive {
  val impl: ZLayer[Logging with EmailSenderConfiguration, EmailSenderFailure, EmailSender] = ZLayer.fromFunctionM { dependencies =>
    (
      for {
        config <- EmailSenderConfiguration
          .emailSenderConfig
          .mapError { error =>
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