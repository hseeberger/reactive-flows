lazy val reactiveFlows = project.in(file("."))

name := "reactive-flows"

libraryDependencies ++= List(
  Library.akkaContrib,
  Library.akkaDataReplication,
  Library.akkaHttp,
  Library.akkaPersistenceMongo,
  Library.akkaSlf4j,
  Library.akkaSse,
  Library.logbackClassic,
  Library.sprayJson,
  Library.akkaTestkit % "test",
  Library.scalaTest   % "test"
)

resolvers ++= List(
  Resolver.hseeberger,
  Resolver.patriknw
)

initialCommands := """|import de.heikoseeberger.reactiveflows._""".stripMargin

addCommandAlias("rf1", "reStart -Dakka.remote.netty.tcp.port=2551 -Dakka.cluster.roles.0=reactive-flows -Dreactive-flows.http-service.port=9001")
addCommandAlias("rf2", "run     -Dakka.remote.netty.tcp.port=2552 -Dakka.cluster.roles.0=reactive-flows -Dreactive-flows.http-service.port=9002")
addCommandAlias("rf3", "run     -Dakka.remote.netty.tcp.port=2553 -Dakka.cluster.roles.0=reactive-flows -Dreactive-flows.http-service.port=9003")
