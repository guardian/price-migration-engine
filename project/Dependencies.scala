import sbt._

object Dependencies {
  lazy val zio = "dev.zio" %% "zio" % "1.0.0-RC18-2"
  lazy val upickle = "com.lihaoyi" %% "upickle" % "1.1.0"
  lazy val lambda = "com.amazonaws" % "aws-lambda-java-core" % "1.2.1"
  lazy val munit = "org.scalameta" %% "munit" % "0.7.5"
}
