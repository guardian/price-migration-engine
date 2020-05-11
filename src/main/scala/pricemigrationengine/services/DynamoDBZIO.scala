package pricemigrationengine.services

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, AttributeValueUpdate, QueryRequest}
import zio.stream.ZStream
import zio.{IO, URIO, ZIO}

case class DynamoDBZIOError(message: String)

trait DynamoDBSerialiser[A] { def serialise(value: A): java.util.Map[String, AttributeValue] }
trait DynamoDBUpdateSerialiser[A] { def serialise(value: A): java.util.Map[String, AttributeValueUpdate] }
trait DynamoDBDeserialiser[A] { def deserialise(value: java.util.Map[String, AttributeValue]): IO[DynamoDBZIOError, A]  }

object DynamoDBZIO {
  trait Service {
    def query[A](query: QueryRequest)(implicit deserializer: DynamoDBDeserialiser[A]): ZStream[Any, DynamoDBZIOError, A]
    def update[A, B](table: String, key: A, value: B)
                    (implicit keySerializer: DynamoDBSerialiser[A],
                     valueSerializer: DynamoDBUpdateSerialiser[B]): IO[DynamoDBZIOError, Unit]
  }

  def query[A](
    query: QueryRequest
  )(implicit deserializer: DynamoDBDeserialiser[A]): URIO[DynamoDBZIO, ZStream[Any, DynamoDBZIOError, A]] = {
    ZIO.access(_.get.query(query))
  }

  def update[A, B](table: String, key: A, value: B)
                  (implicit keySerializer: DynamoDBSerialiser[A],
                   valueSerializer: DynamoDBUpdateSerialiser[B]): URIO[DynamoDBZIO, Unit] = {
    ZIO.access(_.get.update(table, key, value))
  }
}
