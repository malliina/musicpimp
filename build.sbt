import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import com.malliina.appbundler.FileMapping
import com.malliina.file.StorageFile
import com.malliina.sbt.filetree.DirMap
import com.malliina.sbt.GenericPlugin
import com.malliina.sbt.mac.MacKeys._
import com.malliina.sbt.unix.LinuxKeys.{httpPort, httpsPort}
import com.malliina.sbt.win.WinKeys.winSwExe
import com.malliina.sbt.win.{WinKeys, WinPlugin}
import com.malliina.sbtplay.PlayProject
import com.typesafe.sbt.SbtNativePackager.Windows
import com.typesafe.sbt.packager.Keys.{maintainer, packageSummary, rpmVendor}
import play.sbt.PlayImport
import play.sbt.routes.RoutesKeys
import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoKeys.{buildInfoKeys, buildInfoPackage}

val prettyMappings = taskKey[Unit]("Prints the file mappings, prettily")
// wtf?
val release = taskKey[Unit]("Uploads native msi, deb and rpm packages to azure")
val buildAndMove = taskKey[Path]("builds and moves the package")

val musicpimpVersion = "3.9.2"
val pimpcloudVersion = "1.8.8"
val sharedVersion = "1.1.0"
val crossVersion = "1.1.0"
val malliinaGroup = "com.malliina"
val httpGroup = "org.apache.httpcomponents"
val httpVersion = "4.4.1"
val utilPlayDep = malliinaGroup %% "util-play" % "4.3.5"

scalaVersion in ThisBuild := "2.12.3"

lazy val root = project.in(file(".")).aggregate(musicpimp, pimpcloud)
lazy val musicpimpFrontend = scalajsProject("musicpimp-frontend", file("musicpimp") / "frontend")
  .dependsOn(crossJs)
lazy val musicpimp = PlayProject.server("musicpimp", file("musicpimp"))
  .enablePlugins(FileTreePlugin)
  .dependsOn(shared, crossJvm)
  .settings(pimpPlaySettings: _*)
lazy val pimpcloudFrontend = scalajsProject("pimpcloud-frontend", file("pimpcloud") / "frontend")
  .dependsOn(crossJs)
lazy val pimpcloud = PlayProject.server("pimpcloud", file("pimpcloud"))
  .enablePlugins(FileTreePlugin)
  .dependsOn(shared, shared % Test, crossJvm)
  .settings(pimpcloudSettings: _*)
lazy val shared = Project("pimp-shared", file("shared"))
  .dependsOn(crossJvm)
  .settings(sharedSettings: _*)
lazy val it = project.in(file("it"))
  .dependsOn(pimpcloud % "test->test", musicpimp % "test->test")
  .settings(baseSettings: _*)
lazy val cross = crossProject.in(file("cross"))
  .settings(crossSettings: _*)
  .jsSettings(libraryDependencies += "be.doeraene" %%% "scalajs-jquery" % "0.9.1")

lazy val crossJvm = cross.jvm
lazy val crossJs = cross.js

addCommandAlias("pimp", ";project musicpimp")
addCommandAlias("cloud", ";project pimpcloud")
addCommandAlias("it", ";project it")

lazy val crossSettings = Seq(
  updateOptions := updateOptions.value.withCachedResolution(true),
  organization := "org.musicpimp",
  version := "1.1.1",
  libraryDependencies ++= Seq(
    "com.typesafe.play" %%% "play-json" % "2.6.5",
    "com.lihaoyi" %%% "scalatags" % "0.6.7",
    "com.malliina" %%% "primitives" % "1.3.2"
  )
)

lazy val commonSettings = PlayProject.assetSettings ++ scalajsSettings ++ Seq(
  buildInfoKeys += BuildInfoKey("frontName" -> (name in musicpimpFrontend).value),
  javaOptions ++= Seq("-Dorg.slf4j.simpleLogger.defaultLogLevel=error"),
  version := musicpimpVersion,
  resolvers ++= Seq(
    "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
    Resolver.jcenterRepo,
    Resolver.bintrayRepo("malliina", "maven")
  ),
  // for background, see: http://tpolecat.github.io/2014/04/11/scalac-flags.html
  scalacOptions ++= Seq("-encoding", "UTF-8")
)

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

updateOptions in ThisBuild := (updateOptions in ThisBuild).value.withCachedResolution(true)

lazy val scalajsSettings = Seq(
  scalaJSProjects := Seq(musicpimpFrontend),
  pipelineStages in Assets ++= Seq(scalaJSPipeline)
)

lazy val nativePackagingSettings =
  pimpWindowsSettings ++
    pimpMacSettings ++
    pimpLinuxSettings ++
    GenericPlugin.confSettings ++ Seq(
    com.typesafe.sbt.packager.Keys.scriptClasspath := Seq("*"),
    maintainer := "Michael Skogberg <malliina123@gmail.com>",
    manufacturer := "Skogberg Labs",
    displayName := "MusicPimp",
    mainClass := Some("com.malliina.musicpimp.Starter"),
    PlayKeys.externalizeResources := false // packages files in /conf to the app jar
  )

lazy val pimpLinuxSettings = com.malliina.sbt.unix.LinuxPlugin.playSettings ++ Seq(
  javaOptions in Universal ++= Seq(
    "-Dmusicpimp.home=/var/run/musicpimp",
    "-Dlog.dir=/var/run/musicpimp/logs",
    "-Dlogger.resource=prod-logger.xml"
  ),
  packageSummary in Linux := "MusicPimp summary here.",
  rpmVendor := "Skogberg Labs",
  rpmLicense := Option("BSD License")
)

lazy val pimpWindowsSettings = WinPlugin.windowsSettings ++ windowsConfSettings ++ Seq(
  // never change
  WinKeys.upgradeGuid := "5EC7F255-24F9-4E1C-B19D-581626C50F02",
  WinKeys.minJavaVersion := Some(8),
  WinKeys.postInstallUrl := Some("http://localhost:8456"),
  winSwExe in Windows := (pkgHome in Windows).value / "WinSW.NET2.exe"
)

lazy val windowsConfSettings = inConfig(Windows)(Seq(
  prettyMappings := {
    val out: String = WinKeys.msiMappings.value.map {
      case (src, dest) => s"$dest\t\t$src"
    }.sorted.mkString("\n")
    logger.value.log(Level.Info, out)
  },
  appIcon := Some(pkgHome.value / "guitar-128x128-np.ico"),
  buildAndMove := {
    val src = WinKeys.msi.value
    val dest = Files.move(src, target.value.toPath.resolve(name.value + ".msi"), StandardCopyOption.REPLACE_EXISTING)
    streams.value.log.info(s"Moved '$src' to '$dest'.")
    dest
  }
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
    commonSettings ++
    nativePackagingSettings ++
    artifactSettings ++
    Seq(
      libraryDependencies ++= Seq(
        malliinaGroup %% "util-actor" % "2.8.2",
        malliinaGroup %% "util-rmi" % "2.8.2",
        malliinaGroup %% "util-audio" % "2.3.2",
        httpGroup % "httpclient" % httpVersion,
        httpGroup % "httpcore" % httpVersion,
        httpGroup % "httpmime" % httpVersion,
        "net.glxn" % "qrgen" % "1.4",
        "it.sauronsoftware.cron4j" % "cron4j" % "2.2.5",
        "com.h2database" % "h2" % "1.4.196",
        "com.neovisionaries" % "nv-websocket-client" % "2.3"
      ).map(dep => dep withSources()),
      buildInfoPackage := "com.malliina.musicpimp",
      RoutesKeys.routesImport ++= Seq(
        "com.malliina.musicpimp.http.PimpImports._",
        "com.malliina.musicpimp.models._",
        "com.malliina.play.models.Username"
      ),
      fileTreeSources := Seq(
        DirMap((resourceDirectory in Assets).value, "com.malliina.musicpimp.assets.AppAssets", "com.malliina.musicpimp.html.PimpHtml.at"),
        DirMap((resourceDirectory in Compile).value, "com.malliina.musicpimp.licenses.LicenseFiles")
      ),
      libs := libs.value.filter(lib => !lib.toFile.getAbsolutePath.endsWith("bundles\\nv-websocket-client-2.3.jar")),
      fullClasspath in Compile := (fullClasspath in Compile).value.filter(af => !af.data.getAbsolutePath.endsWith("bundles\\nv-websocket-client-2.3.jar"))
    )

lazy val commonServerSettings = baseSettings ++ Seq(
  libraryDependencies ++= Seq(
    utilPlayDep,
    utilPlayDep % Test classifier "tests",
    malliinaGroup %% "logstreams-client" % "0.0.9",
    PlayImport.filters
  ).map(dep => dep.withSources()),
  RoutesKeys.routesImport ++= Seq(
    "com.malliina.musicpimp.http.PimpImports._",
    "com.malliina.musicpimp.models._",
    "com.malliina.play.models.Username"
  ),
  pipelineStages ++= Seq(digest, gzip)
//  pipelineStages in Assets ++= Seq(digest, gzip)
)

// pimpcloud settings

lazy val pimpcloudSettings =
  commonServerSettings ++
    pimpcloudLinuxSettings ++
    pimpcloudScalaJSSettings ++
    artifactSettings ++
    Seq(
      buildInfoKeys += BuildInfoKey("frontName" -> (name in pimpcloudFrontend).value),
      version := pimpcloudVersion,
      libraryDependencies += PlayImport.ehcache,
      PlayKeys.externalizeResources := false,
      fileTreeSources := Seq(DirMap((resourceDirectory in Assets).value, "com.malliina.pimpcloud.assets.CloudAssets", "controllers.pimpcloud.CloudTags.at")),
      buildInfoPackage := "com.malliina.pimpcloud"
    )

lazy val artifactSettings = Seq(
  libs ++= Seq(
    (packageBin in Assets).value.toPath,
    (packageBin in shared in Compile).value.toPath,
    (packageBin in crossJvm in Compile).value.toPath
  )
)

lazy val pimpcloudScalaJSSettings = Seq(
  scalaJSProjects := Seq(pimpcloudFrontend),
  pipelineStages in Assets ++= Seq(scalaJSPipeline)
)


lazy val pimpcloudLinuxSettings = com.malliina.sbt.unix.LinuxPlugin.playSettings ++ Seq(
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
  resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
  libraryDependencies ++= Seq(
    "com.typesafe.slick" %% "slick" % "3.2.1",
    malliinaGroup %% "mobile-push" % "1.7.5",
    utilPlayDep
  )
)

lazy val baseSettings = Seq(
  scalaVersion := "2.12.3",
  organization := "org.musicpimp",
  updateOptions := updateOptions.value.withCachedResolution(true)
)

def scalajsProject(name: String, path: File) =
  Project(name, path)
    .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
    .settings(
      scalaJSUseMainModuleInitializer := true,
      libraryDependencies ++= Seq("org.scalatest" %%% "scalatest" % "3.0.4" % Test),
      testFrameworks += new TestFramework("utest.runner.Framework")
    )
