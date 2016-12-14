import sbt.Keys._
import sbt._

object BuildBuild {
  // "build.sbt" goes here
  val settings = Seq(
    scalaVersion := "2.10.6",
    resolvers ++= Seq(
      "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
      Resolver.url("sbt-plugin-snapshots", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns),
      Resolver.bintrayRepo("malliina", "maven"),
      Resolver.url("malliina bintray sbt", url("https://dl.bintray.com/malliina/sbt-plugins"))(Resolver.ivyStylePatterns)),
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-language:implicitConversions")
  ) ++ sbtPlugins

  def sbtPlugins = Seq(
    "com.malliina" %% "sbt-packager" % "2.1.0",
    "com.malliina" %% "sbt-play" % "0.8.2",
    "com.malliina" %% "sbt-jenkins-control" % "0.3.1",
    "com.eed3si9n" % "sbt-buildinfo" % "0.4.0",
    "com.eed3si9n" % "sbt-assembly" % "0.11.2",
    "org.scala-js" % "sbt-scalajs" % "0.6.13",
    "com.vmunier" % "sbt-web-scalajs" % "1.0.3"
  ) map addSbtPlugin
}
