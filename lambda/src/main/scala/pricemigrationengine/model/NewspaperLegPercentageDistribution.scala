package pricemigrationengine.model

import java.time.LocalDate

sealed trait T1xNewspaperPackageLeg
object T1xMonday extends T1xNewspaperPackageLeg
object T1xTuesday extends T1xNewspaperPackageLeg
object T1xWednesday extends T1xNewspaperPackageLeg
object T1xThursday extends T1xNewspaperPackageLeg
object T1xFriday extends T1xNewspaperPackageLeg
object T1xSaturday extends T1xNewspaperPackageLeg
object T1xSunday extends T1xNewspaperPackageLeg
object T1xDigitalPack extends T1xNewspaperPackageLeg

sealed trait T2xNewspaperPackage
object T2xSixDay extends T2xNewspaperPackage
object T2xWeekend extends T2xNewspaperPackage
object T2xSunday extends T2xNewspaperPackage
object T2xFiveday extends T2xNewspaperPackage
object T2xEveryday extends T2xNewspaperPackage // aka as Multiday
object T2xSixDayPlus extends T2xNewspaperPackage
object T2xWeekendPlus extends T2xNewspaperPackage
object T2xSundayPlus extends T2xNewspaperPackage
object T2xFivedayPlus extends T2xNewspaperPackage
object T2xEverydayPlus extends T2xNewspaperPackage

sealed trait T3xDeliveryCategory
object T3xNewspaperDelivery extends T3xDeliveryCategory
object T3xNewspaperNationalDelivery extends T3xDeliveryCategory
object T3xNewspaperVoucher extends T3xDeliveryCategory
object T3xNewspaperDigitalVoucher extends T3xDeliveryCategory

case class T4xLegPercentage(leg: T1xNewspaperPackageLeg, percentage: BigDecimal)

object NewspaperLegPercentageDistribution {
  val newspaperNationalDeliveryLegPercentageMapping
      : Map[(T3xDeliveryCategory, T2xNewspaperPackage), List[T4xLegPercentage]] = Map(
    (T3xNewspaperNationalDelivery, T2xEveryday) -> List(
      T4xLegPercentage(T1xMonday, BigDecimal(13.2)),
      T4xLegPercentage(T1xTuesday, BigDecimal(13.2)),
      T4xLegPercentage(T1xWednesday, BigDecimal(13.2)),
      T4xLegPercentage(T1xThursday, BigDecimal(13.2)),
      T4xLegPercentage(T1xFriday, BigDecimal(13.2)),
      T4xLegPercentage(T1xSaturday, BigDecimal(17.0)),
      T4xLegPercentage(T1xSunday, BigDecimal(17.0))
    )
  )
}
