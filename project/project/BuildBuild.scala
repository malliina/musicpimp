import sbt.Keys._
import sbt._

/**
 *
 * @author mle
 */
object BuildBuild extends Build {
  // "build.sbt" goes here
  override lazy val settings = super.settings ++ Seq(
    scalaVersion := "2.10.4",
    resolvers ++= Seq(
      "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Sona rels" at "https://oss.sonatype.org/content/repositories/releases/",
      Resolver.url("sbt-plugin-snapshots", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns)),
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    incOptions := incOptions.value.withNameHashing(true)
  ) ++ sbtPlugins

  def sbtPlugins = Seq(
    "com.typesafe.play" % "sbt-plugin" % "2.3.6",
    "com.github.malliina" %% "sbt-packager" % "1.5.10",
    "com.github.malliina" %% "sbt-play" % "0.1.1",
    "com.eed3si9n" % "sbt-buildinfo" % "0.3.0",
    "com.timushev.sbt" % "sbt-updates" % "0.1.6",
    "net.virtual-void" % "sbt-dependency-graph" % "0.7.4",
    "com.eed3si9n" % "sbt-assembly" % "0.11.2"
  ) map addSbtPlugin

  override lazy val projects = Seq(root)
  lazy val root = Project("plugins", file("."))
}