import sbt._

object Version {
  final val Akka         = "2.4.2"
  final val AkkaHttpJson = "1.5.2"
  final val AkkaLog4j    = "1.1.2"
  final val AkkaSse      = "1.6.3"
  final val Circe        = "0.3.0"
  final val Log4j        = "2.5"
  final val Scala        = "2.11.7"
  final val ScalaTest    = "2.2.6"
}

object Library {
  val akkaClusterTools     = "com.typesafe.akka"        %% "akka-cluster-tools"                 % Version.Akka
  val akkaDistributedData  = "com.typesafe.akka"        %% "akka-distributed-data-experimental" % Version.Akka
  val akkaHttp             = "com.typesafe.akka"        %% "akka-http-experimental"             % Version.Akka
  val akkaHttpCirce        = "de.heikoseeberger"        %% "akka-http-circe"                    % Version.AkkaHttpJson
  val akkaHttpTestkit      = "com.typesafe.akka"        %% "akka-http-testkit"                  % Version.Akka
  val akkaLog4j            = "de.heikoseeberger"        %% "akka-log4j"                         % Version.AkkaLog4j
  val akkaMultiNodeTestkit = "com.typesafe.akka"        %% "akka-multi-node-testkit"            % Version.Akka
  val akkaSse              = "de.heikoseeberger"        %% "akka-sse"                           % Version.AkkaSse
  val akkaTestkit          = "com.typesafe.akka"        %% "akka-testkit"                       % Version.Akka
  val circeGeneric         = "io.circe"                 %% "circe-generic"                      % Version.Circe
  val circeJava8           = "io.circe"                 %% "circe-java8"                        % Version.Circe
  val log4jCore            = "org.apache.logging.log4j" %  "log4j-core"                         % Version.Log4j
  val scalaTest            = "org.scalatest"            %% "scalatest"                          % Version.ScalaTest
}
