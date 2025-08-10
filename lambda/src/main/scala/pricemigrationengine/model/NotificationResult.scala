package pricemigrationengine.model

import java.time.{Instant, LocalDate}

trait NotificationResult

case class CancelledNotificationResult(
    subscriptionNumber: String
) extends NotificationResult
