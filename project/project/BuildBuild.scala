import sbt.Keys._
import sbt._

object BuildBuild {
  // "build.sbt" goes here
  val settings = Seq(
    scalaVersion := "2.10.6",
    resolvers ++= Seq(
      ivyRepo("bintray-sbt-plugin-releases",
        "http://dl.bintray.com/content/sbt/sbt-plugin-releases"),
      ivyRepo("malliina bintray sbt",
        "https://dl.bintray.com/malliina/sbt-plugins/"),
      Resolver.bintrayRepo("malliina", "maven")
    ),
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-language:implicitConversions")
  ) ++ sbtPlugins

  def ivyRepo(name: String, urlString: String) =
    Resolver.url(name, url(urlString))(Resolver.ivyStylePatterns)

  def sbtPlugins = Seq(
    "com.malliina" %% "sbt-play" % "0.9.7",
    "com.malliina" %% "sbt-jenkins-control" % "0.3.1",
    "com.eed3si9n" % "sbt-assembly" % "0.11.2",
    "org.scala-js" % "sbt-scalajs" % "0.6.13",
    "com.vmunier" % "sbt-web-scalajs" % "1.0.3"
  ) map addSbtPlugin
}
