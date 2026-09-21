package pricemigrationengine.model

import java.time.LocalDate

/*

Newspaper - National Delivery:

               Monday  | Tuesday | Wednesday | Thursday | Friday   | Saturday | Sunday  | DigiPack | Total |
Everyday    |  13.2 %  |  13.2 % |  13.2 %   |  13.2 %  |  13.2 %  |  17.0 %  | 17.0 %  |          | 100 % |
Sixday      |  15.9 %  |  15.9 % |  15.9 %   |  15.9 %  |  15.9 %  |  20.5 %                       | 100 % |
Weekend     |          |                     |                        50.0 %  | 50.5 %  |                  |
Everyday+   |  11.4 %  |  11.4 % |  11.4 %   |  11.4 %  |  11.4 %  |  14.7 %  | 14.7 %  | 13.6 %   | 100 % |
Sixday+     |  13.4 %  |  13.4 % |  13.4 %   |  13.4 %  |  13.4 %  |  17.2 %  |         | 15.8 %   | 100 % |
Weekend+    |          |         |                                    34.2 %  | 34.2 %  | 31.6 %   | 100 % |

Newspaper delivery:

               Monday  | Tuesday | Wednesday | Thursday | Friday   | Saturday | Sunday  | DigiPack | Total |
Sixday      |  15.9 %  |  15.9 % |  15.9 %   |  15.9 %  |  15.9 %  |  20.5 %  |         |          | 100 % |
Weekend                                                            |  50.0 %  |  50.0 % |
Sunday      |                                                                   100.0 %
Fiveday     |  20.0 %  |  20.0 % |  20.0 %   |  20.0 %  |  20.0 %  |
Everyday    |  13.2 %  |  13.2 % |  13.2 %   |  13.2 %  |  13.2 %  |  17.0 %  |  17.2 % |
Everyday+   |  11.4 %  |  11.4 % |  11.4 %   |  11.4 %  |  11.4 %  |  14.7 %  |  14.7 % |  13.6 % | 100 % |
Sixday+     |  13.4 %  |  13.4 % |  13.4 %   |  13.4 %  |  13.4 %  |  17.2 %  |         |  15.8 % | 100 % |
Weekend+    |                                                      |  34.2 %  |  34.2 % |  31.6 % | 100 % |
Sunday+     |                                                                 |  52.0 % |  48.0 % | 100 % |

Newspaper Digital Voucher:

               Monday  | Tuesday | Wednesday | Thursday | Friday   | Saturday |  Sunday  | DigiPack | Total |
Everyday    |  15.9 %  |  15.9 % |  15.9 %   |  15.9 %  |  15.9 %  |  20.5 %  |          |          | 100 % |
Saturday    |                                                      | 100.0 %  |          |          | 100 % |
Sixday      |  15.9 %  |  15.9 % |  15.9 %   |  15.9 %  |  15.9 %  |  20.5 %  |
Weekend     |                                                         50.0 %  |  50.0 %  |
Sunday      |                                                                   100.0 %  |          | 100 % |
Everyday+   |  11.4 %  |  11.4 % |  11.4 %   |  11.4 %  |  11.4 %  |  14.7 %  |  14.7 %  |  13.6 %  | 100 % |
Saturday+   |                                                         52.0 %  |          |  48.0 %  | 100 % |
Sixday+     |  13.4 %  |  13.4 % |  13.4 %   |  13.4 %  |  13.4 %  |  17.2 %  |          |  15.8 %  | 100 % |
Weekend+    |                                                      |  43.2 %  |  34.2 %  |  31.6 %  | 100 % |
Sunday+     |                                                                    52.0 %  |  48.0 %  | 100 % |

Newspaper Voucher:

               Monday  | Tuesday | Wednesday | Thursday | Friday   | Saturday |  Sunday  | DigiPack | Total |
Everyday    |  13.2 %  |  13.2 % |  13.2 %   |  13.2 %  |  13.2 %  |  17.5 %  |  17.5 %  |          | 100 % |
Saturday    |                                                      | 100.0 %  |          |          | 100 % |
Sixday      |  15.9 %  |  15.9 % |  15.9 %   |  15.9 %  |  15.9 %  |  20.5 %  |          |          | 100 % |
Weekend     |                                                      |  50.0 %  |  50.0 %  |          | 100 % |
Sunday      |                                                      |          | 100.0 %  |          | 100 % |
Everyday+   |  11.4 %  |  11.4 % |  11.4 %   |  11.4 %  |  11.4 %  |  14.7 %  |  14.7 %  |  13.6 %  | 100 % |
Saturday+   |  11.4 %  |  11.4 % |  11.4 %   |  11.4 %  |  11.4 %  |  14.7 %  |  14.7 %  |  13.6 %  | 100 % |
 */

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
object T2xEveryday extends T2xNewspaperPackage // aka Multiday
object T2xSixDayPlus extends T2xNewspaperPackage
object T2xWeekendPlus extends T2xNewspaperPackage
object T2xSundayPlus extends T2xNewspaperPackage
object T2xEverydayPlus extends T2xNewspaperPackage
object T2xSaturday extends T2xNewspaperPackage
object T2xSaturdayPlus extends T2xNewspaperPackage

sealed trait T3xDeliveryCategory
object T3xNewspaperNationalDelivery extends T3xDeliveryCategory
object T3xNewspaperDelivery extends T3xDeliveryCategory
object T3xNewspaperDigitalVoucher extends T3xDeliveryCategory
object T3xNewspaperVoucher extends T3xDeliveryCategory

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
    ),
    (T3xNewspaperNationalDelivery, T2xSixDay) -> List(
      T4xLegPercentage(T1xMonday, BigDecimal(15.9)),
      T4xLegPercentage(T1xTuesday, BigDecimal(15.9)),
      T4xLegPercentage(T1xWednesday, BigDecimal(15.9)),
      T4xLegPercentage(T1xThursday, BigDecimal(15.9)),
      T4xLegPercentage(T1xFriday, BigDecimal(15.9)),
      T4xLegPercentage(T1xSaturday, BigDecimal(20.5)),
    ),
    (T3xNewspaperNationalDelivery, T2xWeekend) -> List(
      T4xLegPercentage(T1xSaturday, BigDecimal(50.0)),
      T4xLegPercentage(T1xSunday, BigDecimal(50.0))
    ),
    (T3xNewspaperNationalDelivery, T2xEverydayPlus) -> List(
      T4xLegPercentage(T1xMonday, BigDecimal(11.4)),
      T4xLegPercentage(T1xTuesday, BigDecimal(11.4)),
      T4xLegPercentage(T1xWednesday, BigDecimal(11.4)),
      T4xLegPercentage(T1xThursday, BigDecimal(11.4)),
      T4xLegPercentage(T1xFriday, BigDecimal(11.4)),
      T4xLegPercentage(T1xSaturday, BigDecimal(14.7)),
      T4xLegPercentage(T1xSunday, BigDecimal(14.7)),
      T4xLegPercentage(T1xDigitalPack, BigDecimal(13.6)),
    ),
    (T3xNewspaperNationalDelivery, T2xSixDayPlus) -> List(
      T4xLegPercentage(T1xMonday, BigDecimal(13.4)),
      T4xLegPercentage(T1xTuesday, BigDecimal(13.4)),
      T4xLegPercentage(T1xWednesday, BigDecimal(13.4)),
      T4xLegPercentage(T1xThursday, BigDecimal(13.4)),
      T4xLegPercentage(T1xFriday, BigDecimal(13.4)),
      T4xLegPercentage(T1xSaturday, BigDecimal(17.2)),
      T4xLegPercentage(T1xDigitalPack, BigDecimal(15.8)),
    ),
    (T3xNewspaperNationalDelivery, T2xWeekendPlus) -> List(
      T4xLegPercentage(T1xSaturday, BigDecimal(34.2)),
      T4xLegPercentage(T1xSunday, BigDecimal(34.2)),
      T4xLegPercentage(T1xDigitalPack, BigDecimal(31.6)),
    )
  )
  val newspaperDeliveryLegPercentageMapping: Map[(T3xDeliveryCategory, T2xNewspaperPackage), List[T4xLegPercentage]] =
    Map(
      (T3xNewspaperDelivery, T2xSixDay) -> List(
        T4xLegPercentage(T1xMonday, BigDecimal(15.9)),
        T4xLegPercentage(T1xTuesday, BigDecimal(15.9)),
        T4xLegPercentage(T1xWednesday, BigDecimal(15.9)),
        T4xLegPercentage(T1xThursday, BigDecimal(15.9)),
        T4xLegPercentage(T1xFriday, BigDecimal(15.9)),
        T4xLegPercentage(T1xSaturday, BigDecimal(20.5)),
      ),
      (T3xNewspaperDelivery, T2xWeekend) -> List(
        T4xLegPercentage(T1xSaturday, BigDecimal(50.0)),
        T4xLegPercentage(T1xSunday, BigDecimal(50.0))
      ),
      (T3xNewspaperDelivery, T2xSunday) -> List(
        T4xLegPercentage(T1xSunday, BigDecimal(100.0))
      ),
      (T3xNewspaperDelivery, T2xFiveday) -> List(
        T4xLegPercentage(T1xMonday, BigDecimal(20.0)),
        T4xLegPercentage(T1xTuesday, BigDecimal(20.0)),
        T4xLegPercentage(T1xWednesday, BigDecimal(20.0)),
        T4xLegPercentage(T1xThursday, BigDecimal(20.0)),
        T4xLegPercentage(T1xFriday, BigDecimal(20.0)),
      ),
      (T3xNewspaperDelivery, T2xEveryday) -> List(
        T4xLegPercentage(T1xMonday, BigDecimal(13.2)),
        T4xLegPercentage(T1xTuesday, BigDecimal(13.2)),
        T4xLegPercentage(T1xWednesday, BigDecimal(13.2)),
        T4xLegPercentage(T1xThursday, BigDecimal(13.2)),
        T4xLegPercentage(T1xFriday, BigDecimal(13.2)),
        T4xLegPercentage(T1xSaturday, BigDecimal(17.0)),
        T4xLegPercentage(T1xSunday, BigDecimal(17.0))
      ),
      (T3xNewspaperDelivery, T2xEverydayPlus) -> List(
        T4xLegPercentage(T1xMonday, BigDecimal(11.4)),
        T4xLegPercentage(T1xTuesday, BigDecimal(11.4)),
        T4xLegPercentage(T1xWednesday, BigDecimal(11.4)),
        T4xLegPercentage(T1xThursday, BigDecimal(11.4)),
        T4xLegPercentage(T1xFriday, BigDecimal(11.4)),
        T4xLegPercentage(T1xSaturday, BigDecimal(14.7)),
        T4xLegPercentage(T1xSunday, BigDecimal(14.7)),
        T4xLegPercentage(T1xDigitalPack, BigDecimal(13.6)),
      ),
      (T3xNewspaperDelivery, T2xWeekendPlus) -> List(
        T4xLegPercentage(T1xSaturday, BigDecimal(52.0)),
        T4xLegPercentage(T1xDigitalPack, BigDecimal(48.0)),
      ),
      (T3xNewspaperDelivery, T2xSixDayPlus) -> List(
        T4xLegPercentage(T1xMonday, BigDecimal(13.4)),
        T4xLegPercentage(T1xTuesday, BigDecimal(13.4)),
        T4xLegPercentage(T1xWednesday, BigDecimal(13.4)),
        T4xLegPercentage(T1xThursday, BigDecimal(13.4)),
        T4xLegPercentage(T1xFriday, BigDecimal(13.4)),
        T4xLegPercentage(T1xSaturday, BigDecimal(17.2)),
        T4xLegPercentage(T1xDigitalPack, BigDecimal(15.8)),
      ),
      (T3xNewspaperDelivery, T2xWeekendPlus) -> List(
        T4xLegPercentage(T1xSaturday, BigDecimal(34.2)),
        T4xLegPercentage(T1xSunday, BigDecimal(34.2)),
        T4xLegPercentage(T1xDigitalPack, BigDecimal(31.6)),
      ),
      (T3xNewspaperDelivery, T2xSundayPlus) -> List(
        T4xLegPercentage(T1xSunday, BigDecimal(52.0)),
        T4xLegPercentage(T1xDigitalPack, BigDecimal(48.0)),
      )
    )
  val newspaperDigitalVoucherLegPercentageMapping
      : Map[(T3xDeliveryCategory, T2xNewspaperPackage), List[T4xLegPercentage]] =
    Map(
      (T3xNewspaperDigitalVoucher, T2xEveryday) -> List(
        T4xLegPercentage(T1xMonday, BigDecimal(13.2)),
        T4xLegPercentage(T1xTuesday, BigDecimal(13.2)),
        T4xLegPercentage(T1xWednesday, BigDecimal(13.2)),
        T4xLegPercentage(T1xThursday, BigDecimal(13.2)),
        T4xLegPercentage(T1xFriday, BigDecimal(13.2)),
        T4xLegPercentage(T1xSaturday, BigDecimal(17.0)),
        T4xLegPercentage(T1xSunday, BigDecimal(17.0))
      ),
      (T3xNewspaperDigitalVoucher, T2xSaturday) -> List(
        T4xLegPercentage(T1xSaturday, BigDecimal(100.0))
      ),
      (T3xNewspaperDigitalVoucher, T2xSixDay) -> List(
        T4xLegPercentage(T1xMonday, BigDecimal(15.9)),
        T4xLegPercentage(T1xTuesday, BigDecimal(15.9)),
        T4xLegPercentage(T1xWednesday, BigDecimal(15.9)),
        T4xLegPercentage(T1xThursday, BigDecimal(15.9)),
        T4xLegPercentage(T1xFriday, BigDecimal(15.9)),
        T4xLegPercentage(T1xSaturday, BigDecimal(20.5)),
      ),
      (T3xNewspaperDigitalVoucher, T2xWeekend) -> List(
        T4xLegPercentage(T1xSaturday, BigDecimal(50.0)),
        T4xLegPercentage(T1xSunday, BigDecimal(50.0))
      ),
      (T3xNewspaperDigitalVoucher, T2xSunday) -> List(
        T4xLegPercentage(T1xSunday, BigDecimal(100.0))
      ),
      (T3xNewspaperDigitalVoucher, T2xEverydayPlus) -> List(
        T4xLegPercentage(T1xMonday, BigDecimal(11.4)),
        T4xLegPercentage(T1xTuesday, BigDecimal(11.4)),
        T4xLegPercentage(T1xWednesday, BigDecimal(11.4)),
        T4xLegPercentage(T1xThursday, BigDecimal(11.4)),
        T4xLegPercentage(T1xFriday, BigDecimal(11.4)),
        T4xLegPercentage(T1xSaturday, BigDecimal(14.7)),
        T4xLegPercentage(T1xSunday, BigDecimal(14.7)),
        T4xLegPercentage(T1xDigitalPack, BigDecimal(13.6)),
      ),
      (T3xNewspaperDigitalVoucher, T2xSaturdayPlus) -> List(
        T4xLegPercentage(T1xSaturday, BigDecimal(52.0)),
        T4xLegPercentage(T1xDigitalPack, BigDecimal(48.0)),
      ),
      (T3xNewspaperDigitalVoucher, T2xSixDayPlus) -> List(
        T4xLegPercentage(T1xMonday, BigDecimal(13.4)),
        T4xLegPercentage(T1xTuesday, BigDecimal(13.4)),
        T4xLegPercentage(T1xWednesday, BigDecimal(13.4)),
        T4xLegPercentage(T1xThursday, BigDecimal(13.4)),
        T4xLegPercentage(T1xFriday, BigDecimal(13.4)),
        T4xLegPercentage(T1xSaturday, BigDecimal(17.2)),
        T4xLegPercentage(T1xDigitalPack, BigDecimal(15.8)),
      ),
      (T3xNewspaperDigitalVoucher, T2xWeekendPlus) -> List(
        T4xLegPercentage(T1xSaturday, BigDecimal(34.2)),
        T4xLegPercentage(T1xSunday, BigDecimal(34.2)),
        T4xLegPercentage(T1xDigitalPack, BigDecimal(31.6)),
      ),
      (T3xNewspaperDigitalVoucher, T2xSundayPlus) -> List(
        T4xLegPercentage(T1xSunday, BigDecimal(52.0)),
        T4xLegPercentage(T1xDigitalPack, BigDecimal(48.0)),
      ),
    )
  val newspaperVoucherLegPercentageMapping: Map[(T3xDeliveryCategory, T2xNewspaperPackage), List[T4xLegPercentage]] =
    Map(
      (T3xNewspaperVoucher, T2xEveryday) -> List(
        T4xLegPercentage(T1xMonday, BigDecimal(13.2)),
        T4xLegPercentage(T1xTuesday, BigDecimal(13.2)),
        T4xLegPercentage(T1xWednesday, BigDecimal(13.2)),
        T4xLegPercentage(T1xThursday, BigDecimal(13.2)),
        T4xLegPercentage(T1xFriday, BigDecimal(13.2)),
        T4xLegPercentage(T1xSaturday, BigDecimal(17.0)),
        T4xLegPercentage(T1xSunday, BigDecimal(17.0))
      ),
      (T3xNewspaperVoucher, T2xSaturday) -> List(
        T4xLegPercentage(T1xSaturday, BigDecimal(100.0))
      ),
      (T3xNewspaperVoucher, T2xSixDay) -> List(
        T4xLegPercentage(T1xMonday, BigDecimal(15.9)),
        T4xLegPercentage(T1xTuesday, BigDecimal(15.9)),
        T4xLegPercentage(T1xWednesday, BigDecimal(15.9)),
        T4xLegPercentage(T1xThursday, BigDecimal(15.9)),
        T4xLegPercentage(T1xFriday, BigDecimal(15.9)),
        T4xLegPercentage(T1xSaturday, BigDecimal(20.5)),
      ),
      (T3xNewspaperVoucher, T2xWeekend) -> List(
        T4xLegPercentage(T1xSaturday, BigDecimal(50.0)),
        T4xLegPercentage(T1xSunday, BigDecimal(50.0))
      ),
      (T3xNewspaperVoucher, T2xSunday) -> List(
        T4xLegPercentage(T1xSunday, BigDecimal(100.0))
      ),
      (T3xNewspaperVoucher, T2xEverydayPlus) -> List(
        T4xLegPercentage(T1xMonday, BigDecimal(11.4)),
        T4xLegPercentage(T1xTuesday, BigDecimal(11.4)),
        T4xLegPercentage(T1xWednesday, BigDecimal(11.4)),
        T4xLegPercentage(T1xThursday, BigDecimal(11.4)),
        T4xLegPercentage(T1xFriday, BigDecimal(11.4)),
        T4xLegPercentage(T1xSaturday, BigDecimal(14.7)),
        T4xLegPercentage(T1xSunday, BigDecimal(14.7)),
        T4xLegPercentage(T1xDigitalPack, BigDecimal(13.6)),
      ),
      (T3xNewspaperVoucher, T2xSaturdayPlus) -> List(
        T4xLegPercentage(T1xSaturday, BigDecimal(52.0)),
        T4xLegPercentage(T1xDigitalPack, BigDecimal(48.0)),
      ),
      (T3xNewspaperVoucher, T2xSixDayPlus) -> List(
        T4xLegPercentage(T1xMonday, BigDecimal(13.4)),
        T4xLegPercentage(T1xTuesday, BigDecimal(13.4)),
        T4xLegPercentage(T1xWednesday, BigDecimal(13.4)),
        T4xLegPercentage(T1xThursday, BigDecimal(13.4)),
        T4xLegPercentage(T1xFriday, BigDecimal(13.4)),
        T4xLegPercentage(T1xSaturday, BigDecimal(17.2)),
        T4xLegPercentage(T1xDigitalPack, BigDecimal(15.8)),
      ),
      (T3xNewspaperVoucher, T2xWeekendPlus) -> List(
        T4xLegPercentage(T1xSaturday, BigDecimal(34.2)),
        T4xLegPercentage(T1xSunday, BigDecimal(34.2)),
        T4xLegPercentage(T1xDigitalPack, BigDecimal(31.6)),
      ),
      (T3xNewspaperVoucher, T2xSundayPlus) -> List(
        T4xLegPercentage(T1xSunday, BigDecimal(52.0)),
        T4xLegPercentage(T1xDigitalPack, BigDecimal(48.0)),
      ),
    )
}
