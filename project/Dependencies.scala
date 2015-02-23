import sbt._

object Version {
  val akka                 = "2.4-M2"
  val akkaHttp             = "1.0"
  val akkaLog4j            = "0.3.0"
  val akkaSse              = "1.1.0-4dd887e10bad23189baf2a331a1986bae915bc07"
  val levelDb              = "0.7"
  val log4j                = "2.3"
  val scala                = "2.11.7"
  val scalaTest            = "2.2.5"
}

object Library {
  val akkaActor            = "com.typesafe.akka"        %% "akka-actor"                         % Version.akka
  val akkaCluster          = "com.typesafe.akka"        %% "akka-cluster"                       % Version.akka
  val akkaClusterSharding  = "com.typesafe.akka"        %% "akka-cluster-sharding"              % Version.akka
  val akkaClusterTools     = "com.typesafe.akka"        %% "akka-cluster-tools"                 % Version.akka
  val akkaDistributedData  = "com.typesafe.akka"        %% "akka-distributed-data-experimental" % Version.akka
  val akkaHttp             = "com.typesafe.akka"        %% "akka-http-experimental"             % Version.akkaHttp
  val akkaHttpSprayJson    = "com.typesafe.akka"        %% "akka-http-spray-json-experimental"  % Version.akkaHttp
  val akkaHttpTestkit      = "com.typesafe.akka"        %% "akka-http-testkit-experimental"     % Version.akkaHttp
  val akkaLog4j            = "de.heikoseeberger"        %% "akka-log4j"                         % Version.akkaLog4j
  val akkaMultiNodeTestkit = "com.typesafe.akka"        %% "akka-multi-node-testkit"            % Version.akka
  val akkaSse              = "de.heikoseeberger"        %% "akka-sse"                           % Version.akkaSse
  val akkaTestkit          = "com.typesafe.akka"        %% "akka-testkit"                       % Version.akka
  val levelDb              = "org.iq80.leveldb"         % "leveldb"                             % Version.levelDb
  val log4jCore            = "org.apache.logging.log4j" %  "log4j-core"                         % Version.log4j
  val scalaTest            = "org.scalatest"            %% "scalatest"                          % Version.scalaTest
}
