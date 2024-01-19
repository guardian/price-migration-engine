package pricemigrationengine.migrations
import pricemigrationengine.migrations.Newspaper2024MigrationEstimation.SubscriptionData2024
import pricemigrationengine.migrations.Newspaper2024MigrationStaticData._
import pricemigrationengine.model._

import java.time.LocalDate

object Newspaper2024MigrationPriceCapping {
  def priceCorrectionFactor(subscription: ZuoraSubscription): BigDecimal = {
    BigDecimal(1)
  }
}
