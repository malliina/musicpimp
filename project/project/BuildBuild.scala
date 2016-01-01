import sbt.Keys._
import sbt._

/**
  *
  * @author mle
  */
object BuildBuild extends Build {
  // "build.sbt" goes here
  override lazy val settings = super.settings ++ Seq(
    scalaVersion := "2.10.6",
    resolvers ++= Seq(
      "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
      Resolver.url("sbt-plugin-snapshots", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns),
      Resolver.bintrayRepo("malliina", "maven"),
      Resolver.url("malliina bintray sbt", url("https://dl.bintray.com/malliina/sbt-plugins"))(Resolver.ivyStylePatterns)),
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-language:implicitConversions"),
    incOptions := incOptions.value.withNameHashing(true)
  ) ++ sbtPlugins

  def sbtPlugins = Seq(
    "com.typesafe.play" % "sbt-plugin" % "2.4.6",
    "com.malliina" %% "sbt-packager" % "1.9.0",
    "com.malliina" %% "sbt-play" % "0.6.0",
    "com.malliina" %% "sbt-jenkins-control" % "0.2.0",
    "com.eed3si9n" % "sbt-buildinfo" % "0.4.0",
    "com.eed3si9n" % "sbt-assembly" % "0.11.2"
  ) map addSbtPlugin

  override lazy val projects = Seq(root)
  lazy val root = Project("plugins", file("."))
}
