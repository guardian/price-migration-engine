package pricemigrationengine.model
import java.time.LocalDate

object Date {
  def datesMax(date1: LocalDate, date2: LocalDate): LocalDate = if (date1.isBefore(date2)) date2 else date1
  def equalOrInOrder(date1: LocalDate, date2: LocalDate): Boolean = {
    // return date1 <= date2
    date1.isBefore(date2) || date1 == date2
  }
}
