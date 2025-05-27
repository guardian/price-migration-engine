package pricemigrationengine.libs
import java.time.LocalDate

object Date {
  def datesMax(date1: LocalDate, date2: LocalDate): LocalDate = if (date1.isBefore(date2)) date2 else date1
}
