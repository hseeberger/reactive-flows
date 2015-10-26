lazy val reactiveFlows = project
  .copy(id = "reactive-flows")
  .in(file("."))
  .configs(MultiJvm)
  .enablePlugins(AutomateHeaderPlugin, GitVersioning, JavaAppPackaging, DockerPlugin)

name := "reactive-flows"

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
  Library.log4jCore,
  Library.akkaHttpTestkit      % "test",
  Library.akkaMultiNodeTestkit % "test",
  Library.akkaTestkit          % "test",
  Library.scalaTest            % "test"
)

initialCommands := """|import de.heikoseeberger.reactiveflows._
                      |""".stripMargin

coverageExcludedPackages := ".*ReactiveFlowsApp;.*Settings"

maintainer.in(Docker) := "Heiko Seeberger"
daemonUser.in(Docker) := "root"
dockerBaseImage       := "java:8"
dockerRepository      := Some("hseeberger")
dockerExposedPorts    := Vector(2552, 8000)

addCommandAlias("rf1", "reStart -Dreactive-flows.http-service.port=8001 -Dakka.remote.netty.tcp.port=2551 -Dcassandra-journal.contact-points.0=192.168.99.100 -Dconstructr.akka.coordination.host=192.168.99.100")
addCommandAlias("rf2", "run     -Dreactive-flows.http-service.port=8002 -Dakka.remote.netty.tcp.port=2552 -Dcassandra-journal.contact-points.0=192.168.99.100 -Dconstructr.akka.coordination.host=192.168.99.100")
addCommandAlias("rf3", "run     -Dreactive-flows.http-service.port=8003 -Dakka.remote.netty.tcp.port=2553 -Dcassandra-journal.contact-points.0=192.168.99.100 -Dconstructr.akka.coordination.host=192.168.99.100")
