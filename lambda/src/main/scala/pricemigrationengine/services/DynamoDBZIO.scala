package pricemigrationengine.services

import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, AttributeValueUpdate, QueryRequest, ScanRequest}
import zio.stream.ZStream
import zio.{IO, URIO, ZIO}

case class DynamoDBZIOError(message: String, cause: Option[Throwable] = None)

trait DynamoDBSerialiser[A] { def serialise(value: A): java.util.Map[String, AttributeValue] }
trait DynamoDBUpdateSerialiser[A] { def serialise(value: A): java.util.Map[String, AttributeValueUpdate] }
trait DynamoDBDeserialiser[A] { def deserialise(value: java.util.Map[String, AttributeValue]): IO[DynamoDBZIOError, A] }

trait DynamoDBZIO {
  def query[A](query: QueryRequest)(implicit deserializer: DynamoDBDeserialiser[A]): ZStream[Any, DynamoDBZIOError, A]
  def scan[A](query: ScanRequest)(implicit deserializer: DynamoDBDeserialiser[A]): ZStream[Any, DynamoDBZIOError, A]
  def update[A, B](table: String, key: A, value: B)(implicit
      keySerializer: DynamoDBSerialiser[A],
      valueSerializer: DynamoDBUpdateSerialiser[B]
  ): IO[DynamoDBZIOError, Unit]
  def create[A](table: String, keyName: String, value: A)(implicit
      valueSerializer: DynamoDBSerialiser[A]
  ): IO[DynamoDBZIOError, Unit]
}

object DynamoDBZIO {

  def query[A](
      query: QueryRequest
  )(implicit deserializer: DynamoDBDeserialiser[A]): URIO[DynamoDBZIO, ZStream[Any, DynamoDBZIOError, A]] = {
    ZIO.environmentWith(_.get.query(query))
  }

  def scan[A](
      query: ScanRequest
  )(implicit deserializer: DynamoDBDeserialiser[A]): URIO[DynamoDBZIO, ZStream[Any, DynamoDBZIOError, A]] = {
    ZIO.environmentWith(_.get.scan(query))
  }

  def update[A, B](table: String, key: A, value: B)(implicit
      keySerializer: DynamoDBSerialiser[A],
      valueSerializer: DynamoDBUpdateSerialiser[B]
  ): ZIO[DynamoDBZIO, DynamoDBZIOError, Unit] = {
    ZIO.environmentWithZIO(_.get.update(table, key, value))
  }

  def create[A](table: String, keyName: String, value: A)(implicit
      keySerializer: DynamoDBSerialiser[A]
  ): ZIO[DynamoDBZIO, DynamoDBZIOError, Unit] = {
    ZIO.environmentWithZIO(_.get.create(table, keyName, value))
  }
}
