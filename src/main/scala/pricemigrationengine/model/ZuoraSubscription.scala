package pricemigrationengine.model

import java.time.LocalDate

import ujson.{Null, Value}
import upickle.default._

case class ZuoraSubscription(
    subscriptionNumber: String,
    customerAcceptanceDate: LocalDate,
    contractEffectiveDate: LocalDate,
    ratePlans: List[ZuoraRatePlan] = Nil,
    accountNumber: String
)

object ZuoraSubscription {

  implicit val rwLocalDate: ReadWriter[LocalDate] = readwriter[String].bimap[LocalDate](
    date => s"${date.toString}",
    str => LocalDate.parse(str)
  )

  implicit def OptionReader[T: Reader]: Reader[Option[T]] = reader[Value].map[Option[T]] {
    case Null    => None
    case jsValue => Some(read[T](jsValue))
  }
  implicit def OptionWriter[T: Writer]: Writer[Option[T]] = writer[Value].comap {
    case Some(value) => write(value)
    case None        => Null
  }

  implicit val rwSubscription: ReadWriter[ZuoraSubscription] = macroRW
  implicit val rwRatePlan: ReadWriter[ZuoraRatePlan] = macroRW
  implicit val rwRatePlanCharge: ReadWriter[ZuoraRatePlanCharge] = macroRW
}
