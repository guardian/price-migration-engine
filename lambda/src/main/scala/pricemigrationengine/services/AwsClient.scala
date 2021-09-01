package pricemigrationengine.services

import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region.EU_WEST_1
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.sfn.SfnClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient

object AwsClient {

  private val region = EU_WEST_1

  /** By explicitly specifying which HTTP client to use, we save an expensive operation looking for a suitable HTTP
    * client on the classpath.
    */
  private def httpSyncClientBuilder() = ApacheHttpClient.builder()
  private def httpAsyncClientBuilder() = NettyNioAsyncHttpClient.builder()

  lazy val sfn: SfnClient = SfnClient.builder.httpClientBuilder(httpSyncClientBuilder()).region(region).build()

  lazy val s3: S3Client = S3Client.builder.httpClientBuilder(httpSyncClientBuilder()).region(region).build()

  lazy val dynamoDb: DynamoDbClient =
    DynamoDbClient.builder.httpClientBuilder(httpSyncClientBuilder()).region(region).build()

  lazy val sqsAsync: SqsAsyncClient =
    SqsAsyncClient.builder.httpClientBuilder(httpAsyncClientBuilder()).region(region).build()
}
