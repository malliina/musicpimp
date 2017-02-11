import java.nio.file.Paths

import com.malliina.appbundler.FileMapping
import com.malliina.file.StorageFile
import com.malliina.jenkinsctrl.models.{BuildOrder, JobName}
import com.malliina.sbt.GenericKeys._
import com.malliina.sbt.GenericPlugin
import com.malliina.sbt.azure.{AzureKeys, AzurePlugin}
import com.malliina.sbt.jenkinsctrl.{JenkinsKeys, JenkinsPlugin}
import com.malliina.sbt.mac.MacKeys._
import com.malliina.sbt.mac.MacPlugin._
import com.malliina.sbt.unix.LinuxPlugin
import com.malliina.sbt.win.{WinKeys, WinPlugin}
import com.malliina.sbtplay.PlayProject
import com.typesafe.sbt.SbtNativePackager
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.{Keys => PackagerKeys}
import com.typesafe.sbt.web.Import.{Assets, pipelineStages}
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport.persistLauncher
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import play.sbt.PlayImport
import play.sbt.PlayImport.PlayKeys
import play.sbt.routes.RoutesKeys
import sbt.Keys._
import sbt._
import sbtbuildinfo.BuildInfoKeys.{buildInfoPackage, buildInfoKeys}
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoKey
import webscalajs.ScalaJSWeb
import webscalajs.WebScalaJS.autoImport.{scalaJSPipeline, scalaJSProjects}

object PimpBuild {

  val prettyMappings = taskKey[Unit]("Prints the file mappings, prettily")
  val jenkinsPackage = taskKey[Unit]("Packages the app for msi (locally), deb, and rpm (remotely)")
  val release = taskKey[Unit]("Uploads native msi, deb and rpm packages to azure")

  lazy val frontend = Project("frontend", file("frontend"))
    .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
    .settings(
      persistLauncher := true,
      libraryDependencies ++= Seq(
        "com.lihaoyi" %%% "scalatags" % "0.6.2",
        "com.lihaoyi" %%% "upickle" % "0.4.3",
        //        "org.scala-js" %%% "scalajs-dom" % "0.9.1"
        "be.doeraene" %%% "scalajs-jquery" % "0.9.1",
        "com.lihaoyi" %%% "utest" % "0.4.4" % Test
      ),
      testFrameworks += new TestFramework("utest.runner.Framework")
    )

  lazy val pimpProject = PlayProject.default("musicpimp")
    .enablePlugins(BuildInfoPlugin, SbtNativePackager)
    .settings(pimpPlaySettings: _*)

  lazy val commonSettings = PlayProject.assetSettings ++ scalaJSSettings ++ Seq(
    buildInfoKeys += BuildInfoKey("frontName" -> (name in frontend).value),
    javaOptions ++= Seq("-Dorg.slf4j.simpleLogger.defaultLogLevel=error"),
    version := "3.5.1",
    organization := "org.musicpimp",
    scalaVersion := "2.11.8",
    resolvers ++= Seq(
      Resolver.jcenterRepo,
      Resolver.bintrayRepo("malliina", "maven"),
      "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
      "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
    ),
    // for background, see: http://tpolecat.github.io/2014/04/11/scalac-flags.html
    scalacOptions ++= Seq(
      "-encoding", "UTF-8"
    )
  )

  def scalaJSSettings = Seq(
    scalaJSProjects := Seq(frontend),
    pipelineStages in Assets := Seq(scalaJSPipeline)
  )

  lazy val jenkinsSettings = JenkinsPlugin.settings ++ Seq(
    release := {
      (AzureKeys.azureUpload in Windows).value
      val order = BuildOrder.simple(JobName("musicpimp-azure"))
      val creds = JenkinsKeys.jenkinsReadCreds.value
      val log = JenkinsKeys.logger.value
      JenkinsPlugin.runLogged(order, creds, log)
    },
    JenkinsKeys.jenkinsDefaultBuild := Option(BuildOrder.simple(JobName("musicpimp-deb-rpm"))),
    jenkinsPackage := {
      val order = BuildOrder.simple(JobName("musicpimp-deb-rpm"))
      val creds = JenkinsKeys.jenkinsReadCreds.value
      val log = JenkinsKeys.logger.value
      WinKeys.msi.value
      JenkinsPlugin.runLogged(order, creds, log)
    }
  )

  lazy val nativePackagingSettings =
    azureSettings ++
      pimpWindowsSettings ++
      pimpMacSettings ++
      pimpLinuxSettings ++
      GenericPlugin.confSettings ++ Seq(
      com.typesafe.sbt.packager.Keys.scriptClasspath := Seq("*"),
      PackagerKeys.maintainer := "Michael Skogberg <malliina123@gmail.com>",
      manufacturer := "Skogberg Labs",
      displayName := "MusicPimp",
      mainClass := Some("com.malliina.musicpimp.Starter"),
      PlayKeys.externalizeResources := false // packages files in /conf to the app jar
    )

  def pimpLinuxSettings = LinuxPlugin.playSettings ++ Seq(
    javaOptions in Universal ++= Seq(
      "-Dmusicpimp.home=/var/run/musicpimp",
      "-Dlog.dir=/var/run/musicpimp/logs",
      "-Dlogger.resource=prod-logger.xml"
    ),
    PackagerKeys.packageSummary in Linux := "MusicPimp summary here.",
    PackagerKeys.rpmVendor := "Skogberg Labs",
    PackagerKeys.rpmLicense := Option("BSD License")
  )

  def azureSettings = AzurePlugin.azureSettings ++ Seq(
    AzureKeys.azureContainerName := "files"
  )

  def pimpWindowsSettings = WinPlugin.windowsSettings ++ windowsConfSettings ++ Seq(
    // never change
    WinKeys.upgradeGuid := "5EC7F255-24F9-4E1C-B19D-581626C50F02",
    WinKeys.minJavaVersion := Some(8),
    WinKeys.postInstallUrl := Some("http://localhost:8456")
  )

  def windowsConfSettings = inConfig(Windows)(Seq(
    prettyMappings := {
      val out: String = WinKeys.msiMappings.value.map {
        case (src, dest) => s"$dest\t\t$src"
      }.sorted.mkString("\n")
      logger.value.log(Level.Info, out)
    },
    appIcon := Some(pkgHome.value / "guitar-128x128-np.ico")
  ))

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

  val malliinaGroup = "com.malliina"
  val httpGroup = "org.apache.httpcomponents"
  val httpVersion = "4.4.1"
  val utilPlayDep = malliinaGroup %% "util-play" % "3.5.3"

  lazy val pimpPlaySettings =
    jenkinsSettings ++
    commonSettings ++
    nativePackagingSettings ++
    Seq(
      libraryDependencies ++= Seq(
        utilPlayDep,
        utilPlayDep % Test classifier "tests",
        malliinaGroup %% "util-actor" % "2.5.5",
        malliinaGroup %% "util-rmi" % "2.5.5",
        malliinaGroup %% "util-audio" % "2.0.0",
        malliinaGroup %% "mobile-push" % "1.7.0",
        httpGroup % "httpclient" % httpVersion,
        httpGroup % "httpcore" % httpVersion,
        httpGroup % "httpmime" % httpVersion,
        PlayImport.filters,
        "net.glxn" % "qrgen" % "1.4",
        "it.sauronsoftware.cron4j" % "cron4j" % "2.2.5",
        "com.h2database" % "h2" % "1.4.193",
        "com.typesafe.slick" %% "slick" % "3.1.1",
        "org.java-websocket" % "Java-WebSocket" % "1.3.0",
        "com.neovisionaries" % "nv-websocket-client" % "1.31"
      ).map(dep => dep withSources()),
      buildInfoPackage := "com.malliina.musicpimp",
      RoutesKeys.routesImport ++= Seq(
        "com.malliina.musicpimp.models._",
        "com.malliina.play.models.Username"
      ),
      libs += (packageBin in Assets).value.toPath
    )
}
