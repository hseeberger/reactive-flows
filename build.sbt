val reactiveFlows = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin, GitVersioning, JavaAppPackaging, DockerPlugin)

organization := "de.heikoseeberger"
name         := "reactive-flows"
licenses     += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

scalaVersion   := "2.11.7"
scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-target:jvm-1.8",
  "-encoding", "UTF-8"
)

unmanagedSourceDirectories.in(Compile) := List(scalaSource.in(Compile).value)
unmanagedSourceDirectories.in(Test)    := List(scalaSource.in(Test).value)

val akkaVersion       = "2.4.1"
val akkaHttpVersion   = "2.0-M2"
libraryDependencies ++= List(
  "com.typesafe.akka"        %% "akka-actor"                        % akkaVersion,
  "com.typesafe.akka"        %% "akka-http-experimental"            % akkaHttpVersion,
  "com.typesafe.akka"        %% "akka-http-spray-json-experimental" % akkaHttpVersion,
  "de.heikoseeberger"        %% "akka-log4j"                        % "1.0.2",
  "de.heikoseeberger"        %% "akka-macro-logging"                % "0.1.0",
  "org.apache.logging.log4j" %  "log4j-core"                        % "2.4.1",
  "com.typesafe.akka"        %% "akka-http-testkit-experimental"    % akkaHttpVersion % "test",
  "com.typesafe.akka"        %% "akka-testkit"                      % akkaVersion     % "test",
  "org.scalatest"            %% "scalatest"                         % "2.2.5"         % "test"
)

initialCommands := """|import de.heikoseeberger.reactiveflows._""".stripMargin

git.baseVersion := "2.4.0"

import scalariform.formatter.preferences._
preferences := preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)

headers := Map("scala" -> de.heikoseeberger.sbtheader.license.Apache2_0("2015", "Heiko Seeberger"))

test.in(Test)         := { scalastyle.in(Compile).toTask("").value; test.in(Test).value }
scalastyleFailOnError := true

coverageMinimum          := 100
coverageFailOnMinimum    := true
coverageExcludedPackages := ".*ReactiveFlowsApp;.*Settings"

maintainer in Docker := "Heiko Seeberger"
version in Docker    := "latest"
daemonUser in Docker := "root"
dockerBaseImage      := "java:8"
dockerRepository     := Some("hseeberger")
dockerExposedPorts   := List(8000)

addCommandAlias("rf1", "reStart -Dreactive-flows.http-service.port=8001")
addCommandAlias("rf2", "run     -Dreactive-flows.http-service.port=8002")
addCommandAlias("rf3", "run     -Dreactive-flows.http-service.port=8003")
