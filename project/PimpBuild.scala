import com.mle.sbt.azure.{AzureKeys, AzurePlugin}
import com.mle.sbt.GenericKeys
import com.mle.sbt.unix.LinuxPlugin
import com.mle.sbt.win.{WinKeys, WinPlugin}
import com.mle.sbt.GenericPlugin
import com.typesafe.sbt.packager.linux
import com.typesafe.sbt.packager.rpm
import com.typesafe.sbt.SbtNativePackager
import SbtNativePackager._
import sbtbuildinfo.Plugin._
import sbt._
import sbt.Keys._
import Dependencies._
import com.mle.sbt.FileImplicits._

object PimpBuild extends Build {
  lazy val pimpProject = Project("musicpimp", file(".")).settings(playSettings: _*)
    .dependsOn(RootProject(uri("git://github.com/malliina/util-audio.git")))

  lazy val commonSettings = Seq(
    version := "2.2.8",
    scalaVersion := "2.10.3",
    retrieveManaged := false,
    sbt.Keys.fork in Test := true,
    resolvers ++= Seq(
      "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
      "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/")
  )

  lazy val nativePackagingSettings = SbtNativePackager.packagerSettings ++
    WinPlugin.windowsSettings ++
    LinuxPlugin.rpmSettings ++
    LinuxPlugin.debianSettings ++
    GenericPlugin.confSettings ++
    AzurePlugin.azureSettings

  /**
   * Intentionally in this order order: commonSettings ++ playScalaSettings ++ nativePackagingSettings
   *
   * Our packaging settings must override the packaging settings from playScalaSettings. Play 2.2 also
   * uses sbt-native-packager and has some own settings but we don't use those for packaging.
   */
  lazy val playSettings = commonSettings ++
    play.Project.playScalaSettings ++
    nativePackagingSettings ++
    Seq(
      libraryDependencies ++= Seq(
        utilActor, utilRmi, scalaTest,
        utilDep, h2, slick,
        httpClient, httpMime, play.Project.filters,
        utilPlay, qrGen, cron4j),
      mainClass := Some("com.mle.musicpimp.Starter"),
      linux.Keys.maintainer := "Michael Skogberg <malliina123@gmail.com>",
      // why conf?
      linux.Keys.packageSummary in Linux := "MusicPimp summary here.",
      rpm.Keys.rpmVendor := "Skogberg Labs",
      GenericKeys.manufacturer := "Skogberg Labs",
      WinKeys.displayName in Windows := "MusicPimp",
      // generate a new product GUID for upgrades
      WinKeys.productGuid := "78970c41-97f7-46f4-ae0b-ef287dd96565",
      // never change
      WinKeys.upgradeGuid := "5EC7F255-24F9-4E1C-B19D-581626C50F02",
      AzureKeys.azureContainerName := "files",
      WinKeys.minJavaVersion := Some(7),
      WinKeys.postInstallUrl := Some("http://localhost:8456"),
      WinKeys.appIcon := Some((GenericKeys.pkgHome in Windows).value / "guitar-128x128-np.ico"),
      WinKeys.forceStopOnUninstall := true
    ) ++ buildMetaSettings

  def buildMetaSettings = buildInfoSettings ++ Seq(
    sourceGenerators in Compile <+= buildInfo,
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion),
    buildInfoPackage := "com.mle.musicpimp"
  )
}

object Dependencies {
  val utilGroup = "com.github.malliina"
  val utilDep = utilGroup %% "util" % "1.2.3"
  val utilPlay = utilGroup %% "util-play" % "1.2.1"
  val utilActor = utilGroup %% "util-actor" % "0.7.1"
  val utilRmi = utilGroup %% "util-rmi" % "1.2.3"
  val scalaTest = "org.scalatest" %% "scalatest" % "2.0" % "test"
  val jodaTime = "joda-time" % "joda-time" % "2.1"
  val jodaConvert = "org.joda" % "joda-convert" % "1.3"
  val jAudioTagger = "org" % "jaudiotagger" % "2.0.3"
  val h2 = "com.h2database" % "h2" % "1.3.173"
  val slick = "com.typesafe.slick" %% "slick" % "1.0.1"
  //  val playExtras = "com.typesafe.play.extras" %% "iteratees-extras" % "1.0.1"
  // used by play but play has an open-ended version [3.1.4)
  // force this instead to speed up build
  val ebeanOrm = "org.avaje.ebeanorm" % "avaje-ebeanorm-agent" % "3.2.1"
  val httpGroup = "org.apache.httpcomponents"
  val httpVersion = "4.3"
  val httpClient = httpGroup % "httpclient" % httpVersion
  val httpMime = httpGroup % "httpmime" % httpVersion
  val qrGen = "net.glxn" % "qrgen" % "1.3"
  val cron4j = "it.sauronsoftware.cron4j" % "cron4j" % "2.2.5"
}