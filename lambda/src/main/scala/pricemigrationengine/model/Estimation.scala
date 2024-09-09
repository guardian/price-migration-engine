package pricemigrationengine.model

import java.time.LocalDate

object Estimation {
  def isProcessable(item: CohortItem, today: LocalDate): Boolean = {
    // This function looks at the `doNotProcessUntil` attribute and returns whether the item
    // should go through the Estimation step. See comment group: 6157ec78
    // Note that LocalDate.isAfter is strict (see EstimationTest for details)
    item.doNotProcessUntil match {
      case None       => true
      case Some(date) => (today == date) || today.isAfter(date)
    }
  }
}
