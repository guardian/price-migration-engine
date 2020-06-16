import Dependencies._
import sbt.Keys.{description, name}

ThisBuild / scalaVersion := "2.13.2"

lazy val root = (project in file("."))
  .aggregate(dynamoDb, lambda)

lazy val dynamoDb = (project in file("dynamoDb"))
  .enablePlugins(RiffRaffArtifact)
  .settings(
    name := "price-migration-engine-dynamo-db",
    description := "Cloudformation for price-migration-engine-dynamo-db",
    riffRaffPackageType := (baseDirectory.value / "cfn"),
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds"),
    riffRaffManifestProjectName := "MemSub::Subscriptions::DynamoDb::PriceMigrationEngine"
  )

lazy val lambda = (project in file("lambda"))
  .enablePlugins(RiffRaffArtifact)
  .settings(
    name := "price-migration-engine-lambda",
    libraryDependencies ++= Seq(
      zio,
      zioStreams,
      upickle,
      awsDynamoDb,
      awsLambda,
      awsS3,
      awsSQS,
      http,
      commonsCsv,
      munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    description := "Lambda jar for the Price Migration Engine",
    assemblyJarName := "price-migration-engine-lambda.jar",
    riffRaffPackageType := assembly.value,
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds"),
    riffRaffManifestProjectName := "MemSub::Subscriptions::Lambda::PriceMigrationEngine",
    riffRaffArtifactResources += (project.base / "cfn.yaml", "cfn/cfn.yaml")
  )
