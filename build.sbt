import Dependencies._
import com.gu.riffraff.artifact.BuildInfo
import sbt.Keys.{description, name}

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion := "2.13.16"

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-Xfatal-warnings"
)

ThisBuild / riffRaffUploadArtifactBucket := Option("riffraff-artifact")
ThisBuild / riffRaffUploadManifestBucket := Option("riffraff-builds")

val buildInfo = Seq(
  buildInfoPackage := "build",
  buildInfoKeys ++= {
    val buildInfo = BuildInfo(baseDirectory.value)
    Seq[BuildInfoKey](
      "buildNumber" -> buildInfo.buildIdentifier
    )
  }
)

lazy val priceMigrationEngine = (project in file("."))
  .aggregate(dynamoDb, lambda, stateMachine)

lazy val dynamoDb = (project in file("dynamoDb"))
  .enablePlugins(RiffRaffArtifact, BuildInfoPlugin)
  .settings(
    name := "price-migration-engine-dynamo-db",
    description := "Cloudformation for price-migration-engine-dynamo-db",
    riffRaffPackageType := (baseDirectory.value / "cfn"),
    riffRaffManifestProjectName := "Retention::PriceMigrationEngine::DynamoDb",
    buildInfo,
  )

lazy val lambda = (project in file("lambda"))
  .enablePlugins(RiffRaffArtifact, BuildInfoPlugin)
  .settings(
    name := "price-migration-engine-lambda",
    dependencyOverrides ++= Seq(
      "io.netty" % "netty-handler" % "4.2.4.Final",
      "io.netty" % "netty-codec-base" % "4.2.4.Final",
      "io.netty" % "netty-codec" % "4.2.4.Final"
    ),
    libraryDependencies ++= Seq(
      zio,
      zioStreams,
      upickle,
      awsDynamoDb,
      awsLambda,
      awsS3,
      awsSQS,
      awsStateMachine,
      awsSecretsManager,
      http,
      commonsCsv,
      slf4jNop % Runtime,
      munit % Test,
      zioTest % Test,
      zioTestSbt % Test,
      zioMock % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    description := "Lambda jar for the Price Migration Engine",
    assemblyJarName := "price-migration-engine-lambda.jar",
    riffRaffPackageType := assembly.value,
    riffRaffManifestProjectName := "Retention::PriceMigrationEngine::Lambda",
    riffRaffArtifactResources += ((project.base / "cfn.yaml", "cfn/cfn.yaml")),
    buildInfo,
    assembly / assemblyMergeStrategy := {
      /*
       * AWS SDK v2 includes a codegen-resources directory in each jar, with conflicting names.
       * This appears to be for generating clients from HTTP services.
       * So it's redundant in a binary artefact.
       */
      case PathList("codegen-resources", _*)                        => MergeStrategy.discard
      case PathList(ps @ _*) if ps.last == "module-info.class"      => MergeStrategy.discard
      case PathList(ps @ _*) if ps.last == "execution.interceptors" => MergeStrategy.filterDistinctLines
      case PathList("META-INF", "io.netty.versions.properties")     => MergeStrategy.discard
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )

lazy val stateMachine = (project in file("stateMachine"))
  .enablePlugins(RiffRaffArtifact, BuildInfoPlugin)
  .settings(
    name := "price-migration-engine-state-machine",
    description := "Cloudformation for price migration state machine.",
    riffRaffPackageType := (baseDirectory.value / "cfn"),
    riffRaffManifestProjectName := "Retention::PriceMigrationEngine::StateMachine",
    buildInfo,
  )
