lazy val reactiveFlows = project
  .copy(id = "reactive-flows")
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin, GitVersioning)

name := "reactive-flows"

libraryDependencies ++= Vector(
  Library.scalaCheck % "test"
)

initialCommands := """|import de.heikoseeberger.reactive.flows._
                      |""".stripMargin
