package pricemigrationengine.services

import pricemigrationengine.model.{EmailSenderConfig, EmailSenderFailure}
import pricemigrationengine.model.membershipworkflow.BrazeMessage
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{GetQueueUrlRequest, SendMessageRequest}
import upickle.default.write
import zio.{ZIO, ZLayer}

/*
  The Braze (client) takes the information in the supplied BrazeMessage object
  and sends it to the membership-workflow app via the contribution-thanks sqs queue.

  Membership workflow will then trigger the Braze campaign or Braze Canvas associated
  with the DataExtensionName in the sqs message.

  If the notification is meant to result in a letter being sent, then Braze will be configured to
  trigger a 'web-hook'. The web hook is essentially an api call to Latcham our direct mail partner,
  who will use the information in the web hook to print a physical letter notifying the customer
  of the price rise and send it to the customer.

  In other migrations, for instance the membership migration, an email is sent to the customer.
 */

object BrazeLive {

  val impl: ZLayer[Logging with EmailSenderConfig, EmailSenderFailure, Braze] =
    ZLayer.fromZIO(
      for {
        logging <- ZIO.service[Logging]
        config <- ZIO.service[EmailSenderConfig]
        sqsClient <- ZIO.attempt(AwsClient.sqsAsync).mapError { ex =>
          EmailSenderFailure(s"Failed to create sqs client: ${ex.getMessage}")
        }
        queueUrlResponse <- ZIO
          .fromCompletableFuture(
            sqsClient.getQueueUrl(GetQueueUrlRequest.builder.queueName(config.sqsEmailQueueName).build())
          )
          .mapError { ex => EmailSenderFailure(s"Failed to get sqs queue url: ${ex.getMessage}") }
      } yield new Braze {
        override def sendMessage(message: BrazeMessage): ZIO[Any, EmailSenderFailure, Unit] =
          sendMessageToBraze(sqsClient, queueUrlResponse.queueUrl, message, logging)
      }
    )

  private def sendMessageToBraze(
      sqsClient: SqsAsyncClient,
      queueUrl: String,
      message: BrazeMessage,
      logging: Logging
  ) = {
    val messageSerialised = serialiseMessage(message)
    for {
      _ <- logging.info(
        s"[65956541] sending email for sfContactId ${message.SfContactId}, message serialised: ${messageSerialised}"
      )
      result <- ZIO
        .fromCompletableFuture {
          sqsClient.sendMessage(
            SendMessageRequest.builder
              .queueUrl(queueUrl)
              .messageBody(messageSerialised)
              .build()
          )
        }
        .mapError { ex =>
          EmailSenderFailure(
            s"Failed to send sqs email message for sfContactId ${message.SfContactId}: ${ex.getMessage}"
          )
        }
      _ <- logging.info(
        s"Successfully sent email for sfContactId ${message.SfContactId} message id: ${result.messageId}, message: ${message}"
      )
    } yield ()
  }

  private[pricemigrationengine] def serialiseMessage(message: BrazeMessage): String = {
    write(message, indent = 2)
  }
}
