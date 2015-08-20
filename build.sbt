val reactiveFlows = project
  .copy(id = "reactive-flows")
  .in(file("."))
  .configs(MultiJvm)
  .enablePlugins(AutomateHeaderPlugin, GitVersioning, JavaAppPackaging, DockerPlugin)

organization := "de.heikoseeberger"
name         := "reactive-flows"
licenses     += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

scalaVersion   := "2.11.7"
scalacOptions ++= Vector(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-target:jvm-1.8",
  "-encoding", "UTF-8"
)

unmanagedSourceDirectories.in(Compile)  := Vector(scalaSource.in(Compile).value)
unmanagedSourceDirectories.in(Test)     := Vector(scalaSource.in(Test).value)
unmanagedSourceDirectories.in(MultiJvm) := Vector(scalaSource.in(MultiJvm).value)

val akkaVersion       = "2.4.2"
val circeVersion      = "0.3.0"
libraryDependencies ++= Vector(
  "com.typesafe.akka"        %% "akka-cluster-sharding"              % akkaVersion,
  "com.typesafe.akka"        %% "akka-distributed-data-experimental" % akkaVersion,
  "com.typesafe.akka"        %% "akka-http-experimental"             % akkaVersion,
  "de.heikoseeberger"        %% "akka-http-circe"                    % "1.5.2",
  "de.heikoseeberger"        %% "akka-log4j"                         % "1.1.2",
  "de.heikoseeberger"        %% "akka-sse"                           % "1.6.3",
  "io.circe"                 %% "circe-generic"                      % circeVersion,
  "io.circe"                 %% "circe-java8"                        % circeVersion,
  "org.apache.logging.log4j" %  "log4j-core"                         % "2.5",
  "com.typesafe.akka"        %% "akka-http-testkit"                  % akkaVersion % "test",
  "com.typesafe.akka"        %% "akka-multi-node-testkit"            % akkaVersion % "test",
  "com.typesafe.akka"        %% "akka-testkit"                       % akkaVersion % "test",
  "org.scalatest"            %% "scalatest"                          % "2.2.6"     % "test"
)

initialCommands := """|import de.heikoseeberger.reactiveflows._""".stripMargin

git.useGitDescribe := true

import scalariform.formatter.preferences._
scalariformPreferences := scalariformPreferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)
inConfig(MultiJvm)(SbtScalariform.configScalariformSettings)
inConfig(MultiJvm)(compileInputs.in(compile) := { scalariformFormat.value; compileInputs.in(compile).value })

headers := Map("scala" -> de.heikoseeberger.sbtheader.license.Apache2_0("2015", "Heiko Seeberger"))
AutomateHeaderPlugin.automateFor(Compile, Test, MultiJvm)
HeaderPlugin.settingsFor(Compile, Test, MultiJvm)

test.in(Test)         := { scalastyle.in(Compile).toTask("").value; test.in(Test).value }
scalastyleFailOnError := true

coverageMinimum          := 100
coverageFailOnMinimum    := true
coverageExcludedPackages := ".*ReactiveFlowsApp;.*Settings"

maintainer.in(Docker) := "Heiko Seeberger"
daemonUser.in(Docker) := "root"
dockerBaseImage      := "java:8"
dockerRepository     := Some("hseeberger")
dockerExposedPorts   := Vector(2552, 8000)

addCommandAlias("rf1", "reStart -Dreactive-flows.http-service.port=8001 -Dakka.remote.netty.tcp.port=2551 -Dakka.cluster.seed-nodes.0=akka.tcp://reactive-flows-system@127.0.0.1:2551")
addCommandAlias("rf2", "run     -Dreactive-flows.http-service.port=8002 -Dakka.remote.netty.tcp.port=2552 -Dakka.cluster.seed-nodes.0=akka.tcp://reactive-flows-system@127.0.0.1:2551")
addCommandAlias("rf3", "run     -Dreactive-flows.http-service.port=8003 -Dakka.remote.netty.tcp.port=2553 -Dakka.cluster.seed-nodes.0=akka.tcp://reactive-flows-system@127.0.0.1:2551")
