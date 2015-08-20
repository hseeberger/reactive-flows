lazy val reactiveFlows = project
  .copy(id = "reactive-flows")
  .in(file("."))
  .configs(MultiJvm)
  .enablePlugins(AutomateHeaderPlugin, GitVersioning)

name := "reactive-flows"

libraryDependencies ++= Vector(
  Library.akkaClusterSharding,
  Library.akkaDistributedData,
  Library.akkaHttp,
  Library.akkaHttpCirce,
  Library.akkaLog4j,
  Library.akkaSse,
  Library.circeGeneric,
  Library.circeJava8,
  Library.log4jCore,
  Library.akkaHttpTestkit      % "test",
  Library.akkaMultiNodeTestkit % "test",
  Library.akkaTestkit          % "test",
  Library.scalaTest            % "test"
)

initialCommands := """|import de.heikoseeberger.reactiveflows._
                      |""".stripMargin

coverageExcludedPackages := ".*ReactiveFlowsApp;.*Settings"

addCommandAlias("rf1", "reStart -Dreactive-flows.http-service.port=8001 -Dakka.remote.netty.tcp.port=2551 -Dakka.cluster.seed-nodes.0=akka.tcp://reactive-flows-system@127.0.0.1:2551")
addCommandAlias("rf2", "run     -Dreactive-flows.http-service.port=8002 -Dakka.remote.netty.tcp.port=2552 -Dakka.cluster.seed-nodes.0=akka.tcp://reactive-flows-system@127.0.0.1:2551")
addCommandAlias("rf3", "run     -Dreactive-flows.http-service.port=8003 -Dakka.remote.netty.tcp.port=2553 -Dakka.cluster.seed-nodes.0=akka.tcp://reactive-flows-system@127.0.0.1:2551")
