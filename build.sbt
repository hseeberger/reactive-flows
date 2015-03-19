lazy val reactiveFlows = project
  .in(file("."))
  .configs(MultiJvm)
  .enablePlugins(AutomateHeaderPlugin, GitVersioning)

name := "reactive-flows"

libraryDependencies ++= List(
  Library.akkaActor,
  Library.akkaCluster,
  Library.akkaContrib,
  Library.akkaDataReplication,
  Library.akkaHttp,
  Library.akkaLog4j,
  Library.akkaHttpSprayJson,
  Library.akkaSse,
  Library.log4jCore,
  Library.akkaHttpTestkit      % "test",
  Library.akkaMultiNodeTestkit % "test",
  Library.akkaTestkit          % "test",
  Library.scalaTest            % "test"
)

initialCommands := """|import de.heikoseeberger.reactiveflows._""".stripMargin

addCommandAlias("rf1", "reStart -Dakka.remote.netty.tcp.port=2551 -Dreactive-flows.http-service.port=9001 -Dakka.cluster.roles.0=shared-journal")
addCommandAlias("rf2", "run     -Dakka.remote.netty.tcp.port=2552 -Dreactive-flows.http-service.port=9002")
addCommandAlias("rf3", "run     -Dakka.remote.netty.tcp.port=2553 -Dreactive-flows.http-service.port=9003")
