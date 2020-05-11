package pricemigrationengine.services

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, QueryRequest}
import zio.stream.ZStream
import zio.{IO, URIO, ZIO}

object DynamoDBZIO {
  trait Service {
    def query[A](
      query: QueryRequest,
      deserializer: java.util.Map[String, AttributeValue] => IO[DynamoDBZIOError, A]
    ): ZStream[Any, DynamoDBZIOError, A]
  }

  def query[A](
    query: QueryRequest,
    deserializer: java.util.Map[String, AttributeValue] => IO[DynamoDBZIOError, A]
  ): URIO[DynamoDBZIO, ZStream[Any, DynamoDBZIOError, A]] = {
    ZIO.access(_.get.query(query, deserializer))
  }
}
