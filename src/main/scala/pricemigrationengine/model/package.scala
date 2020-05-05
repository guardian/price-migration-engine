package pricemigrationengine

import java.time.LocalDate

import ujson.{Null, Value}
import upickle.default._

package object model {

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
}
