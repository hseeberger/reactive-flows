import sbt._

object Version {
  val akka                 = "2.3.9"
  val akkaDataReplication  = "0.10"
  val akkaHttp             = "1.0-M5"
  val akkaHttpJson         = "0.4.0"
  val akkaPersistenceMongo = "0.7.5"
  val akkaSse              = "0.8.0"
  val logback              = "1.1.2"
  val scala                = "2.11.6"
  val scalaTest            = "2.2.4"
}

object Library {
  val akkaActor            = "com.typesafe.akka"   %% "akka-actor"                     % Version.akka
  val akkaCluster          = "com.typesafe.akka"   %% "akka-cluster"                   % Version.akka
  val akkaContrib          = "com.typesafe.akka"   %% "akka-contrib"                   % Version.akka
  val akkaDataReplication  = "com.github.patriknw" %% "akka-data-replication"          % Version.akkaDataReplication
  val akkaHttp             = "com.typesafe.akka"   %% "akka-http-experimental"         % Version.akkaHttp
  val akkaHttpJsonSpray    = "de.heikoseeberger"   %% "akka-http-json-spray"           % Version.akkaHttpJson
  val akkaHttpTestkit      = "com.typesafe.akka"   %% "akka-http-testkit-experimental" % Version.akkaHttp
  val akkaMultiNodeTestkit = "com.typesafe.akka"   %% "akka-multi-node-testkit"        % Version.akka
  val akkaPersistenceMongo = "com.github.ironfish" %% "akka-persistence-mongo-casbah"  % Version.akkaPersistenceMongo
  val akkaSlf4j            = "com.typesafe.akka"   %% "akka-slf4j"                     % Version.akka
  val akkaSse              = "de.heikoseeberger"   %% "akka-sse"                       % Version.akkaSse
  val akkaTestkit          = "com.typesafe.akka"   %% "akka-testkit"                   % Version.akka
  val logbackClassic       = "ch.qos.logback"      %  "logback-classic"                % Version.logback
  val scalaTest            = "org.scalatest"       %% "scalatest"                      % Version.scalaTest
}
