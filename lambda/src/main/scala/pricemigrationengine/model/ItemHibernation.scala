package pricemigrationengine.model

import java.time.LocalDate

object ItemHibernation {

  def isProcessable(item: CohortItem, today: LocalDate): Boolean = {
    item.doNotProcessUntil match {
      case None =>
        throw new Exception(
          s"(error: 588b7698) cohort item: ${item} is in DoNotProcessUntil stage but doesn't have a doNotProcessUntil attribute"
        )
      case Some(date) => date == today || today.isAfter(date)
    }
  }
}
