lazy val `reactive-flows` =
  project
    .in(file("."))
    .configs(MultiJvm)
    .enablePlugins(
      AutomateHeaderPlugin,
      GitVersioning,
      JavaAppPackaging,
      DockerPlugin
    )

libraryDependencies ++= Vector(
  Library.akkaClusterSharding,
  Library.akkaDistributedData,
  Library.akkaHttp,
  Library.akkaHttpCirce,
  Library.akkaLog4j,
  Library.akkaPersistenceCassandra,
  Library.akkaSse,
  Library.circeGeneric,
  Library.circeJava8,
  Library.constructrAkka,
  Library.constructrCoordinationEtcd,
  Library.log4jCore,
  Library.log4jSlf4jImpl,
  Library.akkaHttpTestkit      % "test",
  Library.akkaMultiNodeTestkit % "test",
  Library.akkaTestkit          % "test",
  Library.scalaTest            % "test"
)

initialCommands := """|import de.heikoseeberger.reactiveflows._
                      |""".stripMargin

daemonUser.in(Docker) := "root"
maintainer.in(Docker) := "Heiko Seeberger"
version.in(Docker)    := "latest"
dockerBaseImage       := "java:8"
dockerExposedPorts    := Vector(8000)
dockerRepository      := Some("hseeberger")
