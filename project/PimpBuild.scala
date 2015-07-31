import java.nio.file.Paths

import com.mle.appbundler.FileMapping
import com.mle.file.StorageFile
import com.mle.sbt.GenericKeys._
import com.mle.sbt.GenericPlugin
import com.mle.sbt.azure.{AzureKeys, AzurePlugin}
import com.mle.sbt.mac.MacKeys._
import com.mle.sbt.mac.MacPlugin._
import com.mle.sbt.unix.LinuxPlugin
import com.mle.sbt.win.{WinKeys, WinPlugin}
import com.mle.sbtplay.PlayProjects
import com.typesafe.sbt.SbtNativePackager
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.{linux, rpm}
import play.sbt.PlayImport.PlayKeys
import sbt.Keys._
import sbt._
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin._
import sbtbuildinfo.BuildInfoKeys.buildInfoPackage
import sbtbuildinfo.BuildInfoPlugin
import com.typesafe.sbt.packager.{Keys => PackagerKeys}

object PimpBuild extends Build {

  lazy val pimpProject = PlayProjects.plainPlayProject("musicpimp")
    .enablePlugins(BuildInfoPlugin, SbtNativePackager).settings(playSettings: _*)

  lazy val commonSettings = Seq(
    version := "2.8.6",
    organization := "org.musicpimp",
    scalaVersion := "2.11.7",
//    exportJars := true,
    retrieveManaged := false,
    sbt.Keys.fork in Test := true,
    resolvers ++= Seq(
      Resolver.jcenterRepo,
      Resolver.bintrayRepo("malliina", "maven"),
      "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
      "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    // for background, see: http://tpolecat.github.io/2014/04/11/scalac-flags.html
    scalacOptions ++= Seq(
      "-target:jvm-1.8",
      "-deprecation",
      "-encoding", "UTF-8",
      "-unchecked",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
//      "-Xfatal-warnings",
//      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen"),
    updateOptions := updateOptions.value.withCachedResolution(true)
  )

  lazy val nativePackagingSettings =
    azureSettings ++
      windowsSettings ++
      pimpMacSettings ++
      linuxSettings ++
      GenericPlugin.confSettings ++ Seq(
      com.typesafe.sbt.packager.Keys.scriptClasspath := Seq("*"),
      PackagerKeys.maintainer := "Michael Skogberg <malliina123@gmail.com>",
      manufacturer := "Skogberg Labs",
      displayName := "MusicPimp",
      mainClass := Some("com.mle.musicpimp.Starter")
  )

  def linuxSettings = Seq(
    javaOptions in Universal ++= Seq(
      "-Dhttp.port=8456",
      "-Dpidfile.path=/var/run/musicpimp/pimp.pid",
      "-Dmusicpimp.home=/var/run/musicpimp"
    ),
    PackagerKeys.packageSummary in Linux := "MusicPimp summary here.",
    PackagerKeys.rpmVendor := "Skogberg Labs"
  )

  def azureSettings = AzurePlugin.azureSettings ++ Seq(
    AzureKeys.azureContainerName := "files"
  )

  def windowsSettings = WinPlugin.windowsSettings ++ Seq(
    // never change
    WinKeys.upgradeGuid := "5EC7F255-24F9-4E1C-B19D-581626C50F02",
    WinKeys.minJavaVersion := Some(8),
    WinKeys.postInstallUrl := Some("http://localhost:8456"),
    appIcon in Windows := Some((pkgHome in Windows).value / "guitar-128x128-np.ico")
  )

  def pimpMacSettings = macSettings ++ Seq(
    jvmOptions ++= Seq("-Dhttp.port=8456"),
    launchdConf := Some(defaultLaunchd.value.copy(plistDir = Paths get "/Library/LaunchDaemons")),
    appIcon in Mac := Some((pkgHome in Mac).value / "guitar.icns"),
    pkgIcon := Some((pkgHome in Mac).value / "guitar.png"),
    hideDock := true,
    extraDmgFiles := Seq(
      FileMapping((pkgHome in Mac).value / "guitar.png", Paths get ".background/.bg.png"),
      FileMapping((pkgHome in Mac).value / "DS_Store", Paths get ".DS_Store")
    )
  )

  val mleGroup = "com.github.malliina"
  val httpGroup = "org.apache.httpcomponents"
  val httpVersion = "4.4.1"

  lazy val playSettings = assemblyConf ++
    commonSettings ++
    nativePackagingSettings ++
    Seq(
      libraryDependencies ++= Seq(
        mleGroup %% "play-base" % "0.5.2",
        mleGroup %% "util-actor" % "1.9.0",
        mleGroup %% "util-rmi" % "1.9.0",
        mleGroup %% "util-audio" % "1.6.0",
        mleGroup %% "mobile-push" % "0.9.4",
        httpGroup % "httpclient" % httpVersion,
        httpGroup % "httpmime" % httpVersion,
        play.sbt.PlayImport.filters,
        "net.glxn" % "qrgen" % "1.4",
        "it.sauronsoftware.cron4j" % "cron4j" % "2.2.5",
        "com.h2database" % "h2" % "1.3.176",
        "com.typesafe.slick" %% "slick" % "2.1.0",
        "org.java-websocket" % "Java-WebSocket" % "1.3.0").map(dep => dep withSources()),
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