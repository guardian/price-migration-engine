package pricemigrationengine.services

import pricemigrationengine.model.EmailSenderFailure
import pricemigrationengine.model.membershipworkflow.EmailMessage
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{GetQueueUrlRequest, SendMessageRequest}
import upickle.default.write
import zio.{ZIO, ZLayer}

/** The email sender takes the information in the supplied EmailMessage object and sends it to the membership-workflow
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
  val impl: ZLayer[Logging with EmailSenderConfiguration, EmailSenderFailure, EmailSender] = ZLayer.fromFunctionM {
    dependencies =>
      (
        for {
          config <- EmailSenderConfiguration.emailSenderConfig
            .mapError { error =>
              EmailSenderFailure(s"Failed to get email sender configuration: $error")
            }
          sqsClient <- ZIO
            .effect {
              AwsClient.sqsAsync
            }
            .mapError { ex =>
              EmailSenderFailure(s"Failed to create sqs client: ${ex.getMessage}")
            }
          queueUrlResponse <- ZIO
            .fromCompletableFuture {
              sqsClient.getQueueUrl(GetQueueUrlRequest.builder.queueName(config.sqsEmailQueueName).build())
            }
            .mapError { ex => EmailSenderFailure(s"Failed to get sqs queue url: ${ex.getMessage}") }
        } yield new EmailSender.Service {
          override def sendEmail(message: EmailMessage): ZIO[Any, EmailSenderFailure, Unit] = {
            sendMessage(sqsClient, queueUrlResponse.queueUrl, message)
          }.provide(dependencies)
        }
      ).provide(dependencies)
  }

  private def sendMessage(sqsClient: SqsAsyncClient, queueUrl: String, message: EmailMessage) = {
    for {
      result <- ZIO
        .fromCompletableFuture {
          sqsClient.sendMessage(
            SendMessageRequest.builder
              .queueUrl(queueUrl)
              .messageBody(serialiseMessage(message))
              .build()
          )
        }
        .mapError { ex =>
          EmailSenderFailure(
            s"Failed to send sqs email message for sfContactId ${message.SfContactId}: ${ex.getMessage}"
          )
        }
      _ <- Logging.info(
        s"Successfully sent email for sfContactId ${message.SfContactId} message id: ${result.messageId}"
      )
    } yield ()
  }

  def serialiseMessage(message: EmailMessage): String = {
    write(message, indent = 2)
  }
}
