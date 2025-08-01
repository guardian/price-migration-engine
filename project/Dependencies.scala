import sbt._

object Dependencies {
  private val zioVersion = "2.1.17"
  private val awsSdkVersion = "2.31.78"

  lazy val awsDynamoDb = "software.amazon.awssdk" % "dynamodb" % awsSdkVersion
  lazy val awsS3 = "software.amazon.awssdk" % "s3" % awsSdkVersion
  lazy val awsSQS = "software.amazon.awssdk" % "sqs" % awsSdkVersion
  lazy val awsStateMachine = "software.amazon.awssdk" % "sfn" % awsSdkVersion
  lazy val awsSecretsManager = "software.amazon.awssdk" % "secretsmanager" % awsSdkVersion
  lazy val zio = "dev.zio" %% "zio" % zioVersion
  lazy val zioStreams = "dev.zio" %% "zio-streams" % zioVersion
  lazy val zioTest = "dev.zio" %% "zio-test" % zioVersion
  lazy val zioTestSbt = "dev.zio" %% "zio-test-sbt" % zioVersion
  lazy val zioMock = "dev.zio" %% "zio-mock" % "1.0.0-RC12"
  lazy val upickle = "com.lihaoyi" %% "upickle" % "4.1.0"
  lazy val awsLambda = "com.amazonaws" % "aws-lambda-java-core" % "1.3.0"
  lazy val http = "org.scalaj" %% "scalaj-http" % "2.4.2"
  lazy val munit = "org.scalameta" %% "munit" % "1.1.1"
  lazy val commonsCsv = "org.apache.commons" % "commons-csv" % "1.14.0"
  lazy val slf4jNop = "org.slf4j" % "slf4j-nop" % "2.0.17"
}
