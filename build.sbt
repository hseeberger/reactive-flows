lazy val reactiveFlows = project.in(file("."))

name := "reactive-flows"

libraryDependencies ++= List(
)

initialCommands := """|import de.heikoseeberger.reactiveflows._""".stripMargin
