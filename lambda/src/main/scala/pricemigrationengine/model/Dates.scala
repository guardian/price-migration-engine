package pricemigrationengine.model

import java.time.LocalDate

object Dates {

  def isDateRangeCurrent(start: LocalDate, end: LocalDate): Boolean = {
    val now = LocalDate.now
    !start.isAfter(now) && !end.isBefore(now)
  }
}
