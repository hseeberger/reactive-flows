import sbt._

object Version {
  final val Scala      = "2.11.7"
  final val ScalaCheck = "1.13.0"
}

object Library {
  val scalaCheck = "org.scalacheck" %% "scalacheck" % Version.ScalaCheck
}
