import sbt._

object Version {
  final val Akka       = "2.4.2"
  final val AkkaLog4j  = "1.1.2"
  final val Log4j      = "2.5"
  final val Scala      = "2.11.8"
  final val ScalaTest  = "2.2.6"
}

object Library {
  val akkaActor   = "com.typesafe.akka"        %% "akka-actor"   % Version.Akka
  val akkaLog4j   = "de.heikoseeberger"        %% "akka-log4j"   % Version.AkkaLog4j
  val akkaTestkit = "com.typesafe.akka"        %% "akka-testkit" % Version.Akka
  val log4jCore   = "org.apache.logging.log4j" %  "log4j-core"   % Version.Log4j
  val scalaTest   = "org.scalatest"            %% "scalatest"    % Version.ScalaTest
}
