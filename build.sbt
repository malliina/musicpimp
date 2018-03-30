import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import com.malliina.appbundler.FileMapping
import com.malliina.file.StorageFile
import com.malliina.sbt.GenericKeys.{appIcon, displayName, libs, logger, manufacturer, pkgHome}
import com.malliina.sbt.filetree.DirMap
import com.malliina.sbt.mac.MacKeys._
import com.malliina.sbt.mac.MacPlugin.{Mac, macSettings}
import com.malliina.sbt.unix.LinuxKeys.{httpPort, httpsPort}
import com.malliina.sbt.win.WinKeys.winSwExe
import com.malliina.sbt.win.{WinKeys, WinPlugin}
import com.malliina.sbtplay.PlayProject
import com.typesafe.sbt.SbtNativePackager.Windows
import com.typesafe.sbt.packager.Keys.{maintainer, packageName, packageSummary, rpmVendor}
import play.sbt.PlayImport
import play.sbt.routes.RoutesKeys
import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoKeys.{buildInfoKeys, buildInfoPackage}
import com.malliina.sbtutils.{SbtProjects, SbtUtils}

val prettyMappings = taskKey[Unit]("Prints the file mappings, prettily")
// wtf?
val release = taskKey[Unit]("Uploads native msi, deb and rpm packages to azure")
val buildAndMove = taskKey[Path]("builds and moves the package")
val bootClasspath = taskKey[String]("bootClasspath")

val musicpimpVersion = "3.16.0"
val pimpcloudVersion = "1.13.0"
val sharedVersion = "1.6.0"
val crossVersion = "1.6.0"
val malliinaGroup = "com.malliina"
val soundGroup = "com.googlecode.soundlibs"
val utilPlayVersion = "4.11.0"
val utilPlayDep = malliinaGroup %% "util-play" % utilPlayVersion

scalaVersion in ThisBuild := "2.12.5"

lazy val pimpRoot = project.in(file(".")).aggregate(musicpimp, pimpcloud)
lazy val musicpimpFrontend = scalajsProject("musicpimp-frontend", file("musicpimp") / "frontend")
  .dependsOn(crossJs)
lazy val musicpimp = PlayProject.server("musicpimp", file("musicpimp"))
  .enablePlugins(FileTreePlugin)
  .dependsOn(shared, crossJvm, utilAudio)
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
lazy val utilAudio = SbtProjects.testableProject("util-audio", file("util-audio"))
  .settings(utilAudioSettings: _*)

lazy val crossJvm = cross.jvm
lazy val crossJs = cross.js

addCommandAlias("pimp", ";project musicpimp")
addCommandAlias("cloud", ";project pimpcloud")
addCommandAlias("it", ";project it")
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

lazy val crossSettings = Seq(
  organization := "org.musicpimp",
  version := crossVersion,
  resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
  libraryDependencies ++= Seq(
    "com.typesafe.play" %%% "play-json" % "2.6.9",
    "com.lihaoyi" %%% "scalatags" % "0.6.7",
    "com.malliina" %%% "primitives" % "1.5.0",
    "com.malliina" %%% "util-html" % utilPlayVersion
  )
)

// musicpimp settings

lazy val pimpPlaySettings =
  commonServerSettings ++
    pimpAssetSettings ++
    nativePackagingSettings ++
    artifactSettings ++
    Seq(
      version := musicpimpVersion,
      buildInfoKeys += BuildInfoKey("frontName" -> (name in musicpimpFrontend).value),
      javaOptions ++= Seq("-Dorg.slf4j.simpleLogger.defaultLogLevel=error"),
      resolvers ++= Seq(
        Resolver.jcenterRepo,
        Resolver.bintrayRepo("malliina", "maven")
      ),
      // for background, see: http://tpolecat.github.io/2014/04/11/scalac-flags.html
      scalacOptions ++= Seq("-encoding", "UTF-8"),
      libraryDependencies ++= Seq(
        malliinaGroup %% "util-actor" % "2.10.0",
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
      fullClasspath in Compile := (fullClasspath in Compile).value.filter { af =>
        !af.data.getAbsolutePath.endsWith("bundles\\nv-websocket-client-2.3.jar")
      }
    )

lazy val pimpAssetSettings = PlayProject.assetSettings ++ Seq(
  scalaJSProjects := Seq(musicpimpFrontend),
  pipelineStages in Assets ++= Seq(scalaJSPipeline)
)

lazy val nativePackagingSettings =
  pimpWindowsSettings ++
    pimpMacSettings ++
    Seq(
      com.typesafe.sbt.packager.Keys.scriptClasspath := Seq("*"),
      httpPort in Linux := Option("disabled"),
      httpsPort in Linux := Option("8455"),
      maintainer := "Michael Skogberg <malliina123@gmail.com>",
      manufacturer := "Skogberg Labs",
      displayName := "MusicPimp",
      mainClass := Some("com.malliina.musicpimp.Starter"),
      javaOptions in Universal ++= Seq(
        "-Dlogger.resource=prod-logger.xml"
      ),
      packageSummary in Linux := "MusicPimp summary here.",
      rpmVendor := "Skogberg Labs",
      rpmLicense := Option("BSD License"),
      PlayKeys.externalizeResources := false // packages files in /conf to the app jar
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
      buildInfoPackage := "com.malliina.pimpcloud",
      linuxPackageSymlinks := linuxPackageSymlinks.value.filterNot(_.link == "/usr/bin/starter")
    )

lazy val pimpcloudLinuxSettings = Seq(
  httpPort in Linux := Option("disabled"),
  httpsPort in Linux := Option("8458"),
  maintainer := "Michael Skogberg <malliina123@gmail.com>",
  manufacturer := "Skogberg Labs",
  mainClass := Some("com.malliina.pimpcloud.Starter"),
  bootClasspath := {
    val alpnFile = scriptClasspathOrdering.value
      .map { case (_, dest) => dest }
      .find(_.contains("alpn-boot"))
      .getOrElse(sys.error("Unable to find alpn-boot"))
    val name = (packageName in Debian).value
    val installLocation = defaultLinuxInstallLocation.value
    s"$installLocation/$name/$alpnFile"
  },
  //  bashScriptExtraDefines += s"""addJava "-Xbootclasspath/p:${bootClasspath.value}"""",
  javaOptions in Universal ++= {
    val linuxName = (name in Linux).value
    Seq(
      // for HTTP/2 support
      s"-J-Xbootclasspath/p:${bootClasspath.value}",
      s"-Dgoogle.oauth=/etc/$linuxName/google-oauth.key",
      s"-Dpush.conf=/etc/$linuxName/push.conf",
      "-Dlogger.resource=prod-logger.xml"
    )
  },
  packageSummary in Linux := "This is the pimpcloud summary.",
  rpmVendor := "Skogberg Labs"
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

lazy val utilAudioSettings = SbtUtils.mavenSettings ++ Seq(
  organization := malliinaGroup,
  SbtUtils.gitUserName := "malliina",
  SbtUtils.developerName := "Michael Skogberg",
  libraryDependencies ++= Seq(
    "commons-io" % "commons-io" % "2.6",
    "org.slf4j" % "slf4j-api" % "1.7.25",
    malliinaGroup %% "util-base" % "1.5.0",
    "org" % "jaudiotagger" % "2.0.3",
    soundGroup % "tritonus-share" % "0.3.7.4",
    soundGroup % "jlayer" % "1.0.1.4",
    soundGroup % "mp3spi" % "1.9.5.4"
  ),
  resolvers ++= Seq(
    "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
    Resolver.bintrayRepo("malliina", "maven")
  )
)

lazy val commonServerSettings = baseSettings ++ Seq(
  resolvers += Resolver.bintrayRepo("malliina", "maven"),
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
  pipelineStages ++= Seq(digest, gzip),
  //  pipelineStages in Assets ++= Seq(digest, gzip),
  dependencyOverrides ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.5.11",
    "com.typesafe.akka" %% "akka-stream" % "2.5.8"
  )
)

lazy val sharedSettings = baseSettings ++ Seq(
  version := sharedVersion,
  resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
  libraryDependencies ++= Seq(
    "com.typesafe.slick" %% "slick" % "3.2.2",
    malliinaGroup %% "mobile-push" % "1.12.0",
    utilPlayDep
  )
)

lazy val baseSettings = Seq(
  scalaVersion := "2.12.5",
  organization := "org.musicpimp"
)

def scalajsProject(name: String, path: File) =
  Project(name, path)
    .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
    .settings(
      scalaJSUseMainModuleInitializer := true,
      libraryDependencies ++= Seq("org.scalatest" %%% "scalatest" % "3.0.4" % Test),
      testFrameworks += new TestFramework("utest.runner.Framework")
    )
