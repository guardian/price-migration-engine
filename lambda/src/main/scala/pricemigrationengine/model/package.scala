package pricemigrationengine

import java.time.LocalDate

import ujson.{Null, Value}
import upickle.default._

package object model {

  type ZuoraSubscriptionId = String
  type ZuoraProductRatePlanChargeId = String
  type Currency = String
  type ZuoraPricingData = Map[ZuoraProductRatePlanChargeId, ZuoraProductRatePlanCharge]

  implicit val rwLocalDate: ReadWriter[LocalDate] = readwriter[String].bimap[LocalDate](
    date => s"${date.toString}",
    str => LocalDate.parse(str)
  )

  implicit val rwBigDecimal: ReadWriter[BigDecimal] = readwriter[Double].bimap[BigDecimal](
    decimal => decimal.toDouble,
    double => BigDecimal(double)
  )

  implicit def OptionReader[T: Reader]: Reader[Option[T]] = reader[Value].map[Option[T]] {
    case Null    => None
    case jsValue => Some(read[T](jsValue))
  }

  implicit def OptionWriter[T: Writer]: Writer[Option[T]] = writer[T].comap {
    case Some(value) => value
    case None        => null.asInstanceOf[T]
  }
}
