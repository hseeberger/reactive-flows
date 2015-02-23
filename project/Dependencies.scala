import sbt._

object Version {
  val akka                 = "2.3.11"
  val akkaDataReplication  = "0.11"
  val akkaHttp             = "1.0-RC2"
  val akkaHttpJson         = "0.6.0"
  val akkaLog4j            = "0.1.0"
  val akkaSse              = "0.12.0"
  val log4j                = "2.2"
  val scala                = "2.11.6"
  val scalaTest            = "2.2.4"
}

object Library {
  val akkaActor            = "com.typesafe.akka"        %% "akka-actor"                           % Version.akka
  val akkaCluster          = "com.typesafe.akka"        %% "akka-cluster"                         % Version.akka
  val akkaContrib          = "com.typesafe.akka"        %% "akka-contrib"                         % Version.akka
  val akkaDataReplication  = "com.github.patriknw"      %% "akka-data-replication"                % Version.akkaDataReplication
  val akkaHttp             = "com.typesafe.akka"        %% "akka-http-scala-experimental"         % Version.akkaHttp
  val akkaHttpJsonSpray    = "de.heikoseeberger"        %% "akka-http-json-spray"                 % Version.akkaHttpJson
  val akkaHttpTestkit      = "com.typesafe.akka"        %% "akka-http-testkit-scala-experimental" % Version.akkaHttp
  val akkaLog4j            = "de.heikoseeberger"        %% "akka-log4j"                           % Version.akkaLog4j
  val akkaMultiNodeTestkit = "com.typesafe.akka"        %% "akka-multi-node-testkit"              % Version.akka
  val akkaSse              = "de.heikoseeberger"        %% "akka-sse"                             % Version.akkaSse
  val akkaTestkit          = "com.typesafe.akka"        %% "akka-testkit"                         % Version.akka
  val log4jCore            = "org.apache.logging.log4j" %  "log4j-core"                           % Version.log4j
  val scalaTest            = "org.scalatest"            %% "scalatest"                            % Version.scalaTest
}
