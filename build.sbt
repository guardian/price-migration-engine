import Dependencies._

ThisBuild / scalaVersion := "2.13.2"

lazy val root = (project in file("."))
  .settings(
    name := "price-migration-engine",
    libraryDependencies ++= Seq(zio, scalaTest % Test)
  )
