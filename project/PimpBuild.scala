import com.mle.sbt.FileImplicits._
import com.mle.sbt.azure.{AzureKeys, AzurePlugin}
import com.mle.sbt.unix.LinuxPlugin
import com.mle.sbt.win.{WinKeys, WinPlugin}
import com.mle.sbt.{GenericKeys, GenericPlugin}
import com.mle.sbtplay.PlayProjects
import com.typesafe.sbt.SbtNativePackager
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.{linux, rpm}
import play.PlayImport.PlayKeys
import sbt.Keys._
import sbt._
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin._
import sbtbuildinfo.Plugin._

object PimpBuild extends Build {

  lazy val pimpProject = PlayProjects.plainPlayProject("musicpimp").settings(playSettings: _*)

  lazy val commonSettings = Seq(
    version := "2.7.0",
    scalaVersion := "2.11.4",
    retrieveManaged := false,
    sbt.Keys.fork in Test := true,
    resolvers ++= Seq(
      "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
      "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/"),
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
    scalacOptions += "-target:jvm-1.7"
  )

  lazy val nativePackagingSettings = SbtNativePackager.packagerSettings ++
    WinPlugin.windowsSettings ++
    LinuxPlugin.rpmSettings ++
    LinuxPlugin.debianSettings ++
    GenericPlugin.confSettings ++
    AzurePlugin.azureSettings

  val mleGroup = "com.github.malliina"
  val httpGroup = "org.apache.httpcomponents"
  val httpVersion = "4.3.5"

  lazy val playSettings = assemblyConf ++
    buildMetaSettings ++
    commonSettings ++
    nativePackagingSettings ++
    net.virtualvoid.sbt.graph.Plugin.graphSettings ++
    Seq(
      libraryDependencies ++= Seq(
        mleGroup %% "util-base" % "0.3.0",
        mleGroup %% "util-play" % "1.6.11",
        mleGroup %% "play-base" % "0.1.2",
        mleGroup %% "util-actor" % "1.5.0",
        mleGroup %% "util-rmi" % "1.5.0",
        mleGroup %% "util-audio" % "1.4.4",
        mleGroup %% "logback-rx" % "0.1.0",
        mleGroup %% "mobile-push" % "0.1.1",
        httpGroup % "httpclient" % httpVersion,
        httpGroup % "httpmime" % httpVersion,
        play.PlayImport.filters,
        "net.glxn" % "qrgen" % "1.4",
        "it.sauronsoftware.cron4j" % "cron4j" % "2.2.5",
        "com.h2database" % "h2" % "1.3.176",
        "com.typesafe.slick" %% "slick" % "2.1.0",
        "org.java-websocket" % "Java-WebSocket" % "1.3.0").map(dep => dep withSources()),
      mainClass := Some("com.mle.musicpimp.Starter"),
      linux.Keys.maintainer := "Michael Skogberg <malliina123@gmail.com>",
      // why conf?
      linux.Keys.packageSummary in Linux := "MusicPimp summary here.",
      rpm.Keys.rpmVendor := "Skogberg Labs",
      GenericKeys.manufacturer := "Skogberg Labs",
      WinKeys.displayName in Windows := "MusicPimp",
      // never change
      WinKeys.upgradeGuid := "5EC7F255-24F9-4E1C-B19D-581626C50F02",
      AzureKeys.azureContainerName := "files",
      WinKeys.minJavaVersion := Some(7),
      WinKeys.postInstallUrl := Some("http://localhost:8456"),
      WinKeys.appIcon := Some((GenericKeys.pkgHome in Windows).value / "guitar-128x128-np.ico"),
      resolvers ++= Seq(
        "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
        "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/")
    )

  def buildMetaSettings = buildInfoSettings ++ Seq(
    sourceGenerators in Compile <+= buildInfo,
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion),
    buildInfoPackage := "com.mle.musicpimp"
  )

  def assemblyConf = assemblySettings ++ Seq(
    jarName in assembly := s"app-${version.value}.jar",
    test in assembly :=(),
    fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value),
    mergeStrategy in assembly <<= (mergeStrategy in assembly)((old: (String => MergeStrategy)) => {
      case "application.conf" =>
        MergeStrategy.concat
      case x if (x startsWith """org\apache\commons\logging""") || (x startsWith """play\core\server""") =>
        MergeStrategy.last
      case x if x startsWith """rx\""" =>
        MergeStrategy.first
      case "logger.xml" =>
        MergeStrategy.first
      case x =>
        old(x)
    })
  )
}