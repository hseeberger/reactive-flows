lazy val reactiveFlows = project
  .copy(id = "reactive-flows")
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin, GitVersioning)

name := "reactive-flows"

libraryDependencies ++= Vector(
  Library.akkaHttp,
  Library.akkaHttpCirce,
  Library.akkaLog4j,
  Library.circeGeneric,
  Library.circeJava8,
  Library.log4jCore,
  Library.akkaHttpTestkit % "test",
  Library.akkaTestkit     % "test",
  Library.scalaTest       % "test"
)

initialCommands := """|import de.heikoseeberger.reactiveflows._
                      |""".stripMargin

coverageExcludedPackages := ".*ReactiveFlowsApp;.*Settings"
