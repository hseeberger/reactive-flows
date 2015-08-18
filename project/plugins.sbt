addSbtPlugin("com.typesafe.sbt"  % "sbt-git"               % "0.8.5")
addSbtPlugin("com.typesafe.sbt"  % "sbt-multi-jvm"         % "0.3.11")
addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager"   % "1.0.6")
addSbtPlugin("de.heikoseeberger" % "sbt-header"            % "1.5.1")
addSbtPlugin("io.spray"          % "sbt-revolver"          % "0.8.0")
addSbtPlugin("org.scalariform"   % "sbt-scalariform"       % "1.6.0")
addSbtPlugin("org.scalastyle"    % "scalastyle-sbt-plugin" % "0.8.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"         % "1.3.1")

// Temporary workaround until https://github.com/scoverage/sbt-scoverage/issues/125 is fixed:
resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)
