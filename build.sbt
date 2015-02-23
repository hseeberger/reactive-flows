lazy val reactiveFlows = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin, GitVersioning)

name := "reactive-flows"

libraryDependencies ++= List(
  Library.scalaCheck % "test",
  Library.scalaTest  % "test"
)

initialCommands := """|import de.heikoseeberger.reactiveflows._""".stripMargin
