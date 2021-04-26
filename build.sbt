import Dependencies._
import sbt.Keys.{description, name}

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion := "2.13.5"

lazy val root = (project in file("."))
  .aggregate(dynamoDb, lambda, stateMachine)

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
      awsStateMachine,
      http,
      commonsCsv,
      munit % Test,
      zioTest % Test,
      zioTestSbt % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    description := "Lambda jar for the Price Migration Engine",
    assemblyJarName := "price-migration-engine-lambda.jar",
    riffRaffPackageType := assembly.value,
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds"),
    riffRaffManifestProjectName := "MemSub::Subscriptions::Lambda::PriceMigrationEngine",
    riffRaffArtifactResources += ((project.base / "cfn.yaml", "cfn/cfn.yaml"))
  )

lazy val stateMachine = (project in file("stateMachine"))
  .enablePlugins(RiffRaffArtifact)
  .settings(
    name := "price-migration-engine-state-machine",
    description := "Cloudformation for price migration state machine.",
    riffRaffPackageType := (baseDirectory.value / "cfn"),
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds"),
    riffRaffManifestProjectName := "MemSub::Subscriptions::StateMachine::PriceMigrationEngine"
  )
