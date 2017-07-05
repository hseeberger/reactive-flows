addSbtPlugin("com.dwijnand"      % "sbt-travisci"        % "1.1.0")
addSbtPlugin("com.geirsson"      % "sbt-scalafmt"        % "0.6.6")
addSbtPlugin("com.thesamet"      % "sbt-protoc"          % "0.99.11")
addSbtPlugin("com.typesafe.sbt"  % "sbt-git"             % "0.9.3")
addSbtPlugin("com.typesafe.sbt"  % "sbt-multi-jvm"       % "0.3.11")
addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager" % "1.2.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header"          % "2.0.0")
addSbtPlugin("io.spray"          % "sbt-revolver"        % "0.8.0")

libraryDependencies += "org.slf4j"              % "slf4j-nop"       % "1.7.25" // Needed by sbt-git
libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.0"
