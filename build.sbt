import Dependencies._
import sbt.Keys.description

ThisBuild / scalaVersion := "2.13.2"

lazy val root = (project in file("."))
  .settings(
    name := "price-migration-engine",
    libraryDependencies ++= Seq(
      zio,
      zioStreams,
      upickle,
      awsDynamoDb,
      munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .aggregate(dynamoDb)

lazy val dynamoDb = (project in file("dynamoDb"))
  .enablePlugins(RiffRaffArtifact)
  .settings(
    name := "price-migration-engine",
    name := "price-migration-engine-dynamo-db",
    description:= "Cloudformation for price-migration-engine-dynamo-db",
    riffRaffPackageType := (baseDirectory.value / "cfn"),
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds"),
    riffRaffManifestProjectName := "MemSub::Subscriptions::DynamoDb::PriceMigrationEngine"
  )


