// addSbtPlugin("com.dwijnand"      % "sbt-travisci"        % "1.1.0")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"             % "1.1.0-M1")
addSbtPlugin("com.lucidchart"    % "sbt-scalafmt"        % "1.10")
addSbtPlugin("com.thesamet"      % "sbt-protoc"          % "0.99.12-rc5")
addSbtPlugin("com.typesafe.sbt"  % "sbt-git"             % "0.9.3")
// addSbtPlugin("com.typesafe.sbt"  % "sbt-multi-jvm"       % "0.3.11")
addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager" % "1.2.2-RC2")
addSbtPlugin("de.heikoseeberger" % "sbt-header"          % "3.0.0")
addSbtPlugin("org.foundweekends" % "sbt-bintray"         % "0.5.1")

libraryDependencies += "org.slf4j"              % "slf4j-nop"       % "1.7.25" // Needed by sbt-git
libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.1"
