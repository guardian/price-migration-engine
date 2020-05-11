import sbt._

object Dependencies {
  private val zioVersion = "1.0.0-RC18-2"

  lazy val awsDynamoDb = "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.774"
  lazy val zio = "dev.zio" %% "zio" % zioVersion
  lazy val zioStreams =  "dev.zio" %% "zio-streams" % zioVersion
  lazy val upickle = "com.lihaoyi" %% "upickle" % "1.1.0"
  lazy val lambda = "com.amazonaws" % "aws-lambda-java-core" % "1.2.1"
  lazy val munit = "org.scalameta" %% "munit" % "0.7.5"
}
