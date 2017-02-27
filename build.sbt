// *****************************************************************************
// Projects
// *****************************************************************************

lazy val `reactive-flows` =
  project
    .in(file("."))
    .configs(MultiJvm)
    .enablePlugins(AutomateHeaderPlugin, GitVersioning, DockerPlugin, JavaAppPackaging)
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        library.akkaClusterSharding,
        library.akkaDistributedData,
        library.akkaHttp,
        library.akkaHttpCirce,
        library.akkaLog4j,
        library.akkaPersistenceCassandra,
        library.akkaSse,
        library.circeGeneric,
        library.circeJava8,
        library.constructr,
        library.constructrCoordinationEtcd,
        library.log4jCore,
        library.log4jSlf4jImpl,
        library.akkaHttpTestkit      % Test,
        library.akkaMultiNodeTestkit % Test,
        library.akkaTestkit          % Test,
        library.scalaTest            % Test
      )
    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val akka                     = "2.4.17"
      val akkaHttp                 = "10.0.4"
      val akkaHttpJson             = "1.12.0"
      val akkaLog4j                = "1.3.0"
      val akkaPersistenceCassandra = "0.23"
      val akkaSse                  = "2.0.0"
      val circe                    = "0.7.0"
      val constructr               = "0.16.1"
      val log4j                    = "2.8"
      val scala                    = "2.12.1"
      val scalaTest                = "3.0.1"
    }
    val akkaClusterSharding        = "com.typesafe.akka"        %% "akka-cluster-sharding"              % Version.akka
    val akkaDistributedData        = "com.typesafe.akka"        %% "akka-distributed-data-experimental" % Version.akka
    val akkaPersistenceCassandra   = "com.typesafe.akka"        %% "akka-persistence-cassandra"         % Version.akkaPersistenceCassandra
    val akkaHttp                   = "com.typesafe.akka"        %% "akka-http"                          % Version.akkaHttp
    val akkaHttpCirce              = "de.heikoseeberger"        %% "akka-http-circe"                    % Version.akkaHttpJson
    val akkaHttpTestkit            = "com.typesafe.akka"        %% "akka-http-testkit"                  % Version.akkaHttp
    val akkaLog4j                  = "de.heikoseeberger"        %% "akka-log4j"                         % Version.akkaLog4j
    val akkaMultiNodeTestkit       = "com.typesafe.akka"        %% "akka-multi-node-testkit"            % Version.akka
    val akkaSse                    = "de.heikoseeberger"        %% "akka-sse"                           % Version.akkaSse
    val akkaTestkit                = "com.typesafe.akka"        %% "akka-testkit"                       % Version.akka
    val circeGeneric               = "io.circe"                 %% "circe-generic"                      % Version.circe
    val circeJava8                 = "io.circe"                 %% "circe-java8"                        % Version.circe
    val constructr                 = "de.heikoseeberger"        %% "constructr"                         % Version.constructr
    val constructrCoordinationEtcd = "de.heikoseeberger"        %% "constructr-coordination-etcd"       % Version.constructr
    val log4jCore                  = "org.apache.logging.log4j" %  "log4j-core"                         % Version.log4j
    val log4jSlf4jImpl             = "org.apache.logging.log4j" %  "log4j-slf4j-impl"                   % Version.log4j
    val scalaTest                  = "org.scalatest"            %% "scalatest"                          % Version.scalaTest
  }

// *****************************************************************************
// Settings
// *****************************************************************************        |

lazy val settings =
  commonSettings ++
  gitSettings ++
  headerSettings ++
  dockerSettings ++
  multiJvmSettings

lazy val commonSettings =
  Seq(
    // scalaVersion and crossScalaVersions from .travis.yml via sbt-travisci
    // scalaVersion := "2.12.1",
    // crossScalaVersions := Seq(scalaVersion.value, "2.11.8"),
    organization := "de.heikoseeberger",
    licenses += ("Apache 2.0",
                 url("http://www.apache.org/licenses/LICENSE-2.0")),
    mappings.in(Compile, packageBin) += baseDirectory.in(ThisBuild).value / "LICENSE" -> "LICENSE",
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding", "UTF-8"
    ),
    javacOptions ++= Seq(
      "-source", "1.8",
      "-target", "1.8"
    ),
    unmanagedSourceDirectories.in(Compile) := Seq(scalaSource.in(Compile).value),
    unmanagedSourceDirectories.in(Test) := Seq(scalaSource.in(Test).value),
    publishArtifact.in(Compile, packageDoc) := false,
    publishArtifact.in(Compile, packageSrc) := false
)

lazy val gitSettings =
  Seq(
    git.useGitDescribe := true
  )

import de.heikoseeberger.sbtheader.license._
lazy val headerSettings =
  Seq(
    headers := Map("scala" -> Apache2_0("2015", "Heiko Seeberger"))
  )

lazy val dockerSettings =
  Seq(
    daemonUser.in(Docker) := "root",
    maintainer.in(Docker) := "Heiko Seeberger",
    version.in(Docker)    := "latest",
    dockerBaseImage       := "openjdk:8",
    dockerExposedPorts    := Vector(8000),
    dockerRepository      := Some("hseeberger")
  )

lazy val multiJvmSettings =
  automateScalafmtFor(MultiJvm) ++
  AutomateHeaderPlugin.automateFor(MultiJvm) ++
  HeaderPlugin.settingsFor(MultiJvm) ++
  Seq(
    unmanagedSourceDirectories.in(MultiJvm) := Vector(scalaSource.in(MultiJvm).value),
    test.in(Test) := test.in(Test).dependsOn(test.in(MultiJvm)).value
  )
