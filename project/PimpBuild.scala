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
import com.malliina.sbt.unix.LinuxKeys.{httpPort, httpsPort}
import com.malliina.sbt.unix.LinuxPlugin
import com.malliina.sbt.win.{WinKeys, WinPlugin}
import com.malliina.sbtplay.PlayProject
import com.typesafe.sbt.SbtNativePackager.{Linux, Universal, _}
import com.typesafe.sbt.packager.Keys.{maintainer, packageSummary, rpmVendor}
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
import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoKeys.{buildInfoKeys, buildInfoPackage}
import webscalajs.ScalaJSWeb
import webscalajs.WebScalaJS.autoImport.{scalaJSPipeline, scalaJSProjects}

object PimpBuild {
  val musicpimpVersion = "3.6.4"
  val pimpcloudVersion = "1.7.8"
  val sharedVersion = "1.0.2"
  val crossVersion = "1.0.1"
  val malliinaGroup = "com.malliina"
  val httpGroup = "org.apache.httpcomponents"
  val httpVersion = "4.4.1"
  val utilPlayDep = malliinaGroup %% "util-play" % "3.6.8"

  val prettyMappings = taskKey[Unit]("Prints the file mappings, prettily")
  val jenkinsPackage = taskKey[Unit]("Packages the app for msi (locally), deb, and rpm (remotely)")
  // wtf?
  val release = taskKey[Unit]("Uploads native msi, deb and rpm packages to azure")

  lazy val root = Project("root", file(".")).aggregate(musicpimp, pimpcloud)

  lazy val musicpimpFrontend = scalajsProject("musicpimp-frontend", file("musicpimp") / "frontend")
    .dependsOn(crossJs)

  lazy val musicpimp = PlayProject.server("musicpimp", file("musicpimp"))
    .dependsOn(shared, crossJvm)
    .settings(pimpPlaySettings: _*)

  lazy val pimpcloudFrontend = scalajsProject("pimpcloud-frontend", file("pimpcloud") / "frontend")
    .dependsOn(crossJs)

  lazy val pimpcloud = PlayProject.server("pimpcloud", file("pimpcloud"))
    .dependsOn(shared, crossJvm)
    .settings(pimpcloudSettings: _*)

  lazy val shared = Project("pimp-shared", file("shared"))
    .settings(sharedSettings: _*)

  lazy val it = Project("it", file("it"))
    .dependsOn(pimpcloud % "test->test", musicpimp % "test->test")
    .settings(baseSettings: _*)

  lazy val cross = crossProject.in(file("cross"))
    .settings(crossSettings: _*)
    .jvmSettings(scalaVersion := "2.11.8")
    .jsSettings(libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % "0.6.3",
      "com.lihaoyi" %%% "upickle" % "0.4.3"))

  lazy val crossJvm = cross.jvm
  lazy val crossJs = cross.js

  lazy val crossSettings = Seq(
    organization := "org.musicpimp",
    version := "1.0.1"
  )

  def scalajsProject(name: String, path: File) =
    Project(name, path)
      .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
      .settings(
        persistLauncher := true,
        libraryDependencies ++= Seq(
          "be.doeraene" %%% "scalajs-jquery" % "0.9.1",
          "com.lihaoyi" %%% "utest" % "0.4.4" % Test
        ),
        testFrameworks += new TestFramework("utest.runner.Framework")
      )

  lazy val commonSettings = PlayProject.assetSettings ++ scalajsSettings ++ Seq(
    buildInfoKeys += BuildInfoKey("frontName" -> (name in musicpimpFrontend).value),
    javaOptions ++= Seq("-Dorg.slf4j.simpleLogger.defaultLogLevel=error"),
    version := musicpimpVersion,
    resolvers ++= Seq(
      Resolver.jcenterRepo,
      Resolver.bintrayRepo("malliina", "maven")
    ),
    // for background, see: http://tpolecat.github.io/2014/04/11/scalac-flags.html
    scalacOptions ++= Seq(
      "-encoding", "UTF-8"
    )
  )

  lazy val scalajsSettings = Seq(
    scalaJSProjects := Seq(musicpimpFrontend),
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

  lazy val pimpLinuxSettings = LinuxPlugin.playSettings ++ Seq(
    javaOptions in Universal ++= Seq(
      "-Dmusicpimp.home=/var/run/musicpimp",
      "-Dlog.dir=/var/run/musicpimp/logs",
      "-Dlogger.resource=prod-logger.xml"
    ),
    PackagerKeys.packageSummary in Linux := "MusicPimp summary here.",
    PackagerKeys.rpmVendor := "Skogberg Labs",
    PackagerKeys.rpmLicense := Option("BSD License")
  )

  lazy val azureSettings = AzurePlugin.azureSettings ++ Seq(
    AzureKeys.azureContainerName := "files"
  )

  lazy val pimpWindowsSettings = WinPlugin.windowsSettings ++ windowsConfSettings ++ Seq(
    // never change
    WinKeys.upgradeGuid := "5EC7F255-24F9-4E1C-B19D-581626C50F02",
    WinKeys.minJavaVersion := Some(8),
    WinKeys.postInstallUrl := Some("http://localhost:8456")
  )

  lazy val windowsConfSettings = inConfig(Windows)(Seq(
    prettyMappings := {
      val out: String = WinKeys.msiMappings.value.map {
        case (src, dest) => s"$dest\t\t$src"
      }.sorted.mkString("\n")
      logger.value.log(Level.Info, out)
    },
    appIcon := Some(pkgHome.value / "guitar-128x128-np.ico")
  ))

  lazy val pimpMacSettings = macSettings ++ Seq(
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

  lazy val pimpPlaySettings =
    commonServerSettings ++
      jenkinsSettings ++
      commonSettings ++
      nativePackagingSettings ++
      Seq(
        libraryDependencies ++= Seq(
          malliinaGroup %% "util-actor" % "2.5.6",
          malliinaGroup %% "util-rmi" % "2.5.6",
          malliinaGroup %% "util-audio" % "2.0.2",
          httpGroup % "httpclient" % httpVersion,
          httpGroup % "httpcore" % httpVersion,
          httpGroup % "httpmime" % httpVersion,
          "net.glxn" % "qrgen" % "1.4",
          "it.sauronsoftware.cron4j" % "cron4j" % "2.2.5",
          "com.h2database" % "h2" % "1.4.193",
          "com.neovisionaries" % "nv-websocket-client" % "1.31"
        ).map(dep => dep withSources()),
        buildInfoPackage := "com.malliina.musicpimp",
        RoutesKeys.routesImport ++= Seq(
          "com.malliina.musicpimp.models._",
          "com.malliina.play.models.Username"
        ),
        // TODO get rid of this
        libs ++= Seq(
          (packageBin in Assets).value.toPath,
          (packageBin in shared in Compile).value.toPath,
          (packageBin in crossJvm in Compile).value.toPath
        )
      )

  lazy val commonServerSettings = baseSettings ++ Seq(
    libraryDependencies ++= Seq(
      utilPlayDep,
      utilPlayDep % Test classifier "tests",
      malliinaGroup %% "logstreams-client" % "0.0.6",
      "org.java-websocket" % "Java-WebSocket" % "1.3.0",
      PlayImport.filters
    ).map(dep => dep.withSources()),
    RoutesKeys.routesImport ++= Seq(
      "com.malliina.musicpimp.models._",
      "com.malliina.play.models.Username"
    )
  )

  // pimpcloud settings

  lazy val pimpcloudSettings =
    commonServerSettings ++
      pimpcloudJenkinsSettings ++
      pimpcloudLinuxSettings ++
      pimpcloudScalaJSSettings ++
      Seq(
        buildInfoKeys += BuildInfoKey("frontName" -> (name in pimpcloudFrontend).value),
        version := pimpcloudVersion,
        libraryDependencies ++= Seq(
          PlayImport.cache
        ),
        PlayKeys.externalizeResources := false,
        libs ++= Seq(
          (packageBin in Assets).value.toPath,
          (packageBin in shared in Compile).value.toPath,
          (packageBin in crossJvm in Compile).value.toPath
        )
      )

  lazy val pimpcloudScalaJSSettings = Seq(
    scalaJSProjects := Seq(pimpcloudFrontend),
    pipelineStages in Assets := Seq(scalaJSPipeline)
  )

  lazy val pimpcloudJenkinsSettings = JenkinsPlugin.settings ++ Seq(
    JenkinsKeys.jenkinsDefaultBuild := Option(BuildOrder.simple(JobName("pimpcloud")))
  )

  lazy val pimpcloudLinuxSettings = LinuxPlugin.playSettings ++ Seq(
    httpPort in Linux := Option("disabled"),
    httpsPort in Linux := Option("8458"),
    maintainer := "Michael Skogberg <malliina123@gmail.com>",
    manufacturer := "Skogberg Labs",
    mainClass := Some("com.malliina.pimpcloud.Starter"),
    javaOptions in Universal ++= {
      val linuxName = (name in Linux).value
      Seq(
        s"-Dgoogle.oauth=/etc/$linuxName/google-oauth.key",
        s"-Dpush.conf=/etc/$linuxName/push.conf",
        s"-Dlog.dir=/var/run/$linuxName/logs",
        "-Dlogger.resource=prod-logger.xml",
        "-Dfile.encoding=UTF-8",
        "-Dsun.jnu.encoding=UTF-8"
      )
    },
    packageSummary in Linux := "This is the pimpcloud summary.",
    rpmVendor := "Skogberg Labs"
  )

  lazy val sharedSettings = baseSettings ++ Seq(
    version := sharedVersion,
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick" % "3.1.1",
      malliinaGroup %% "mobile-push" % "1.7.0",
      PlayImport.json,
      utilPlayDep
    )
  )

  lazy val baseSettings = Seq(
    scalaVersion := "2.11.8",
    organization := "org.musicpimp"
  )
}
