package pricemigrationengine.migrations
import pricemigrationengine.model._

import java.time.LocalDate

object LegacyMigrations {

  /*
    The priceCap implements a policy of not increasing subscription prices, and
    therefore what our customers pay, by more then 20% during a single price rise.

    It was introduced in late 2022, but not applied to the digital subscriptions price rises
    of 2023 and the Newspaper2024 (which is partially capped) implemented it's own logic, because the
    basic implementation wasn't compatible.

    LegacyMigrations was introduced to encapsulate this function away from the general model, as
    a first step towards its eventual decommission.
   */

  private val priceCappingMultiplier = 1.2 // old price + 20%
  def priceCap(oldPrice: BigDecimal, estimatedNewPrice: BigDecimal, forceEstimated: Boolean = false): BigDecimal = {
    if (forceEstimated) {
      estimatedNewPrice
    } else {
      List(estimatedNewPrice, oldPrice * priceCappingMultiplier).min
    }
  }

}
