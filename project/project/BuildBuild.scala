import sbt._
import sbt.Keys._

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
      "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
      "Sona rels" at "https://oss.sonatype.org/content/repositories/releases/",
      Resolver.url("sbt-plugin-snapshots", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns)),
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    incOptions := incOptions.value.withNameHashing(true)
  ) ++ sbtPlugins

  def sbtPlugins = Seq(
    "com.typesafe.play" % "sbt-plugin" % "2.2.3",
    "com.github.malliina" %% "sbt-packager" % "1.2.2",
    "com.eed3si9n" % "sbt-buildinfo" % "0.3.0",
    "com.timushev.sbt" % "sbt-updates" % "0.1.6",
    "net.virtual-void" % "sbt-dependency-graph" % "0.7.4"
  ) map addSbtPlugin

  override lazy val projects = Seq(root)
  lazy val root = Project("plugins", file("."))
}


