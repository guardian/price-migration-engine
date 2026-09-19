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

sealed trait NewspaperPackage
object NPPxSixDay extends NewspaperPackage
object NPPxWeekend extends NewspaperPackage
object NPPxSunday extends NewspaperPackage
object NPPxFiveday extends NewspaperPackage
object NPPxEveryday extends NewspaperPackage // aka as Multiday
object NPPxSixDayPlus extends NewspaperPackage
object NPPxWeekendPlus extends NewspaperPackage
object NPPxSundayPlus extends NewspaperPackage
object NPPxFivedayPlus extends NewspaperPackage
object NPPxEverydayPlus extends NewspaperPackage

sealed trait NPDeliveryCategory
object NewspaperDelivery extends NPDeliveryCategory
object NewspaperNationalDelivery extends NPDeliveryCategory
object NewspaperVoucher extends NPDeliveryCategory
object NewspaperDigitalVoucher extends NPDeliveryCategory

case class NNPLegPercentage(leg: T1xNewspaperPackageLeg, percentage: BigDecimal)

object NewspaperLegPercentageDistribution {
  val newspaperNationalDeliveryLegPercentageMapping
      : Map[(NPDeliveryCategory, NewspaperPackage), List[NNPLegPercentage]] = Map(
    (NewspaperNationalDelivery, NPPxEveryday) -> List(
      NNPLegPercentage(T1xMonday, BigDecimal(13.2)),
      NNPLegPercentage(T1xTuesday, BigDecimal(13.2)),
      NNPLegPercentage(T1xWednesday, BigDecimal(13.2)),
      NNPLegPercentage(T1xThursday, BigDecimal(13.2)),
      NNPLegPercentage(T1xFriday, BigDecimal(13.2)),
      NNPLegPercentage(T1xSaturday, BigDecimal(17.0)),
      NNPLegPercentage(T1xSunday, BigDecimal(17.0))
    )
  )
}
