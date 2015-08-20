lazy val reactiveFlows = project
  .copy(id = "reactive-flows")
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin, GitVersioning)

name := "reactive-flows"

libraryDependencies ++= Vector(
  Library.akkaHttp,
  Library.akkaLog4j,
  Library.log4jCore,
  Library.akkaHttpTestkit % "test",
  Library.akkaTestkit     % "test",
  Library.scalaTest       % "test"
)

initialCommands := """|import de.heikoseeberger.reactiveflows._
                      |""".stripMargin
