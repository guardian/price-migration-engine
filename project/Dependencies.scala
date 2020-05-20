import sbt._

object Dependencies {
  private val zioVersion = "1.0.0-RC19-2"

  lazy val awsDynamoDb = "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.787"
  lazy val zio = "dev.zio" %% "zio" % zioVersion
  lazy val zioStreams = "dev.zio" %% "zio-streams" % zioVersion
  lazy val upickle = "com.lihaoyi" %% "upickle" % "1.1.0"
  lazy val awsLambda = "com.amazonaws" % "aws-lambda-java-core" % "1.2.1"
  lazy val http = "org.scalaj" %% "scalaj-http" % "2.4.2"
  lazy val munit = "org.scalameta" %% "munit" % "0.7.7"
}
