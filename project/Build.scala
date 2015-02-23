import com.typesafe.sbt.GitPlugin
import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.autoImport.MultiJvm
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import de.heikoseeberger.sbtheader.{ AutomateHeaderPlugin, HeaderPlugin }
import de.heikoseeberger.sbtheader.license.Apache2_0
import sbt._
import sbt.Keys._
import scalariform.formatter.preferences.{ AlignSingleLineCaseStatements, DoubleIndentClassDeclaration }
import spray.revolver.RevolverPlugin.Revolver

object Build extends AutoPlugin {

  override def requires = plugins.JvmPlugin && HeaderPlugin && GitPlugin && SbtMultiJvm

  override def trigger = allRequirements

  override def projectSettings =
    // Core settings
    List(
      organization := "de.heikoseeberger",
      licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
      scalaVersion := Version.scala,
      crossScalaVersions := List(scalaVersion.value),
      scalacOptions ++= List(
        "-unchecked",
        "-deprecation",
        "-language:_",
        "-target:jvm-1.7",
        "-encoding", "UTF-8"
      ),
      unmanagedSourceDirectories.in(Compile) := List(scalaSource.in(Compile).value),
      unmanagedSourceDirectories.in(Test) := List(scalaSource.in(Test).value),
      unmanagedSourceDirectories.in(MultiJvm) := List(scalaSource.in(MultiJvm).value),
      resolvers ++= List("hseeberger", "patriknw").map(Resolver.bintrayRepo(_, "maven"))
    ) ++
    // Scalariform settings
    SbtScalariform.scalariformSettings ++
    List(
      ScalariformKeys.preferences := ScalariformKeys.preferences.value
        .setPreference(AlignSingleLineCaseStatements, true)
        .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
        .setPreference(DoubleIndentClassDeclaration, true)
    ) ++
    inConfig(MultiJvm)(SbtScalariform.configScalariformSettings) ++
    inConfig(MultiJvm)(compileInputs.in(compile) <<= compileInputs.in(compile).dependsOn(ScalariformKeys.format)) ++
    // Git settings
    List(
      GitPlugin.autoImport.git.baseVersion := "3.0.0"
    ) ++
    // Header settings
    AutomateHeaderPlugin.automateFor(Compile, Test, MultiJvm) ++
    List(
      HeaderPlugin.autoImport.headers := Map("scala" -> Apache2_0("2015", "Heiko Seeberger"))
    ) ++
    HeaderPlugin.settingsFor(Compile, Test, MultiJvm) ++
    // Revolver settings
    Revolver.settings
}
