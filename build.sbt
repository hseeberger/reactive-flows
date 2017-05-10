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
        library.akkaHttpTestkit         % Test,
        library.akkaMultiNodeTestkit    % Test,
        library.akkaPersistenceInmemory % Test,
        library.akkaTestkit             % Test,
        library.scalaTest               % Test
      )
    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val akka                     = "2.5.1"
      val akkaHttp                 = "10.0.6"
      val akkaHttpJson             = "1.15.0"
      val akkaLog4j                = "1.4.0"
      val akkaPersistenceCassandra = "0.52"
      val akkaPersistenceInmemory  = "2.5.1.0"
      val akkaSse                  = "3.0.0"
      val circe                    = "0.7.1"
      val constructr               = "0.17.0"
      val log4j                    = "2.8.2"
      val scalaTest                = "3.0.3"
      val scalapb                  = com.trueaccord.scalapb.compiler.Version.scalapbVersion
    }
    val akkaClusterSharding        = "com.typesafe.akka"        %% "akka-cluster-sharding"        % Version.akka
    val akkaDistributedData        = "com.typesafe.akka"        %% "akka-distributed-data"        % Version.akka
    val akkaPersistenceCassandra   = "com.typesafe.akka"        %% "akka-persistence-cassandra"   % Version.akkaPersistenceCassandra
    val akkaHttp                   = "com.typesafe.akka"        %% "akka-http"                    % Version.akkaHttp
    val akkaHttpCirce              = "de.heikoseeberger"        %% "akka-http-circe"              % Version.akkaHttpJson
    val akkaHttpTestkit            = "com.typesafe.akka"        %% "akka-http-testkit"            % Version.akkaHttp
    val akkaLog4j                  = "de.heikoseeberger"        %% "akka-log4j"                   % Version.akkaLog4j
    val akkaMultiNodeTestkit       = "com.typesafe.akka"        %% "akka-multi-node-testkit"      % Version.akka
    val akkaPersistenceInmemory    = "com.github.dnvriend"      %% "akka-persistence-inmemory"    % Version.akkaPersistenceInmemory
    val akkaSse                    = "de.heikoseeberger"        %% "akka-sse"                     % Version.akkaSse
    val akkaTestkit                = "com.typesafe.akka"        %% "akka-testkit"                 % Version.akka
    val circeGeneric               = "io.circe"                 %% "circe-generic"                % Version.circe
    val circeJava8                 = "io.circe"                 %% "circe-java8"                  % Version.circe
    val constructr                 = "de.heikoseeberger"        %% "constructr"                   % Version.constructr
    val constructrCoordinationEtcd = "de.heikoseeberger"        %% "constructr-coordination-etcd" % Version.constructr
    val log4jCore                  = "org.apache.logging.log4j" %  "log4j-core"                   % Version.log4j
    val log4jSlf4jImpl             = "org.apache.logging.log4j" %  "log4j-slf4j-impl"             % Version.log4j
    val scalaTest                  = "org.scalatest"            %% "scalatest"                    % Version.scalaTest
    val scalapbRuntime             = "com.trueaccord.scalapb"   %% "scalapb-runtime"              % Version.scalapb
  }

// *****************************************************************************
// Settings
// *****************************************************************************        |

lazy val settings =
  commonSettings ++
  gitSettings ++
  dockerSettings ++
  multiJvmSettings ++
  pbSettings

lazy val commonSettings =
  Seq(
    // scalaVersion and crossScalaVersions from .travis.yml via sbt-travisci
    // scalaVersion := "2.12.2",
    // crossScalaVersions := Seq(scalaVersion.value, "2.11.11"),
    organization := "de.heikoseeberger",
    organizationName := "Heiko Seeberger",
    startYear := Some(2015),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding", "UTF-8"
    ),
    unmanagedSourceDirectories.in(Compile) := Seq(scalaSource.in(Compile).value),
    unmanagedSourceDirectories.in(Test) := Seq(scalaSource.in(Test).value),
    publishArtifact.in(Compile, packageDoc) := false,
    publishArtifact.in(Compile, packageSrc) := false,
    shellPrompt in ThisBuild := { state =>
      val project = Project.extract(state).currentRef.project
      s"[$project]> "
    }
)

lazy val gitSettings =
  Seq(
    git.useGitDescribe := true
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
  headerSettings(MultiJvm) ++
  automateHeaderSettings(MultiJvm) ++
  Seq(
    unmanagedSourceDirectories.in(MultiJvm) := Seq(scalaSource.in(MultiJvm).value),
    test.in(Test) := test.in(MultiJvm).dependsOn(test.in(Test)).value
  )

lazy val pbSettings =
  Seq(
    PB.targets.in(Compile) := Seq(
      scalapb.gen(flatPackage = true) -> sourceManaged.in(Compile).value
    ),
    libraryDependencies ++= Seq(
      library.scalapbRuntime % "protobuf"
    )
  )
