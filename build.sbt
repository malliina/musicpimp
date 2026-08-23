import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import com.malliina.appbundler.FileMapping
import com.malliina.sbt.GenericKeys.*
import com.malliina.filetree.DirMap
import com.malliina.rollup.CommonKeys.{assetsPrefix, isProd}
import com.malliina.sbt.mac.MacKeys.*
import com.malliina.sbt.mac.MacPlugin.{Mac, macSettings}
import com.malliina.sbt.unix.LinuxKeys.{httpPort, httpsPort}
import com.malliina.sbt.unix.LinuxPlugin as LinusPlugin
import com.malliina.sbt.win.WinKeys.{minJavaVersion, msiMappings, useTerminateProcess, winSwExe}
import com.malliina.sbt.win.{WinKeys, WinPlugin}
import com.typesafe.sbt.SbtNativePackager.Windows
import com.typesafe.sbt.packager.Keys.{maintainer, packageSummary, rpmVendor}
import sbt.Keys.scalaVersion
import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoKeys.{buildInfoKeys, buildInfoPackage}
import sbtcrossproject.CrossPlugin.autoImport.{CrossType as PortableType, crossProject as portableProject}
import sbtrelease.ReleaseStateTransformations.{checkSnapshotDependencies, runTest}
import scalajsbundler.util.JSON

import scala.sys.process.Process
import scala.util.Try

val prettyMappings = taskKey[Unit]("Prints the file mappings, prettily")
// wtf?
val release = taskKey[Unit]("Uploads native msi, deb and rpm packages to azure")
val buildAndMove = taskKey[Path]("builds and moves the package")
val bootClasspath = taskKey[String]("bootClasspath")

val versions = new {
  val catsEffect = "3.7.0"
  val circe = "0.14.9"
  val fs2 = "3.13.0"
  val http = "4.5.14"
  val logstreams = "6.14.3"
  val mobilePush = "3.17.1"
  val munit = "1.3.5"
  val munitCats = "2.2.0"
  val mysql = "8.0.33"
  val nvWebSocket = "2.14"
  val pekko = "1.0.3"
  val scalaJsDom = "2.8.1"
  val primitives = "6.14.3"
  val scala3 = "3.8.3"
  val scalatags = "0.13.1"
  val slf4j = "2.0.17"
}

val malliinaGroup = "com.malliina"
val soundGroup = "com.googlecode.soundlibs"
val logstreamsDep = malliinaGroup %% "logstreams-client" % versions.logstreams
val jacksonDep =
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.18.0" // Fixes some dep hell
val httpGroup = "org.apache.httpcomponents"

inThisBuild(
  Seq(
    scalaVersion := versions.scala3
  )
)

val packageAndCopy = taskKey[File]("Copies packaged file for convenience")

val cross = portableProject(JSPlatform, JVMPlatform)
  .crossType(PortableType.Full)
  .in(file("cross"))
  .settings(
    organization := "org.musicpimp",
    libraryDependencies ++= Seq("generic", "parser")
      .map(m => "io.circe" %%% s"circe-$m" % versions.circe) ++ Seq(
      malliinaGroup %%% "primitives" % versions.primitives
    )
  )
val crossJvm = cross.jvm
val crossJs = cross.js
  .enablePlugins(ScalaJSBundlerPlugin, ScalaJSWeb)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % versions.scalaJsDom
    ),
    Compile / npmDependencies ++= Seq(
      "jquery" -> "3.3.1",
      "jquery-ui" -> "1.12.1"
    )
  )

val html = portableProject(JSPlatform, JVMPlatform)
  .crossType(PortableType.Full)
  .in(file("util-html"))
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % versions.scalatags,
      malliinaGroup %%% "util-html" % versions.primitives,
      "org.scalameta" %%% "munit" % versions.munit % Test
    )
  )
val htmlJvm = html.jvm
val htmlJs = html.js

val utilAudio = Project("util-audio", file("util-audio"))
  .enablePlugins(MavenCentralPlugin)
  .settings(
    organization := malliinaGroup,
    gitUserName := "malliina",
    developerName := "Michael Skogberg",
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % versions.fs2,
      "commons-io" % "commons-io" % "2.18.0",
      "org.slf4j" % "slf4j-api" % "2.0.17",
      malliinaGroup %% "primitives" % versions.primitives,
      "org" % "jaudiotagger" % "2.0.3",
      soundGroup % "tritonus-share" % "0.3.7.4",
      soundGroup % "jlayer" % "1.0.1.4",
      soundGroup % "mp3spi" % "1.9.5.4",
      "org.typelevel" %% "cats-effect" % versions.catsEffect,
      "org.scalameta" %% "munit" % versions.munit % Test,
      "org.typelevel" %% "munit-cats-effect" % versions.munitCats % Test,
    ),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "versions", "9", "module-info.class") => MergeStrategy.last
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )

val shared = Project("pimp-shared", file("pimpshared"))
  .dependsOn(crossJvm)
  .settings(baseSettings *)
  .settings(
    libraryDependencies ++= Seq(
      "com.malliina" %% "util-http4s" % versions.primitives,
      "com.malliina" %% "web-auth" % versions.primitives,
      "com.malliina" %% "config" % versions.primitives,
      logstreamsDep,
      "mysql" % "mysql-connector-java" % versions.mysql,
      malliinaGroup %% "mobile-push-io" % versions.mobilePush,
      "com.lihaoyi" %% "scalatags" % versions.scalatags
    )
  )

val musicpimpFrontend = scalajsProject("musicpimp-frontend", file("musicpimp") / "frontend")
  .dependsOn(crossJs)
  .settings(
    libraryDependencies ++= Seq("generic", "parser")
      .map(m => "io.circe" %%% s"circe-$m" % versions.circe) ++ Seq(
      malliinaGroup %%% "primitives" % versions.primitives
    ),
    assetsRoot := (Compile / npmUpdate / crossTarget).value.toPath,
//    assetsRoot := ((Compile / crossTarget).value / "stage").toPath.resolve("assets"),
    assetsPrefix := "public/"
  )
val musicpimp = project
  .in(file("musicpimp"))
  .enablePlugins(
    JavaServerAppPackaging,
    SystemdPlugin,
    BuildInfoPlugin,
    FileTreePlugin,
    WebScalaJSBundlerPlugin
  )
  .dependsOn(shared, crossJvm, utilAudio)
  .settings(musicpimpSettings *)
  .settings(
    buildInfoKeys ++= Seq[BuildInfoKey](
      "assetsDir" -> Def.settingDyn(musicpimpFrontend / assetsRoot).value.toFile,
      "publicDir" -> (Assets / resourceDirectory).value,
      "publicFolder" -> Def.settingDyn(musicpimpFrontend / assetsPrefix).value,
    ),
  )

val pimpcloudFrontend = scalajsProject("pimpcloud-frontend", file("pimpcloud") / "frontend")
  .dependsOn(crossJs)
  .settings(
    libraryDependencies ++= Seq("generic", "parser")
      .map(m => "io.circe" %%% s"circe-$m" % versions.circe) ++ Seq(
      malliinaGroup %%% "primitives" % versions.primitives
    ),
    assetsRoot := (Compile / npmUpdate / crossTarget).value.toPath,
    assetsPrefix := "",
    Compile / npmDependencies ++= Seq("jquery" -> "3.3.1")
  )
val pimpcloud = project
  .in(file("pimpcloud"))
  .enablePlugins(
    JavaServerAppPackaging,
    SystemdPlugin,
    BuildInfoPlugin,
    FileTreePlugin,
    WebScalaJSBundlerPlugin
  )
  .dependsOn(
    shared,
    shared % Test,
    crossJvm
  )
  .settings(pimpcloudSettings *)
  .settings(
    version := "4.26.9",
    scalaJSProjects := Seq(pimpcloudFrontend),
    Assets / pipelineStages ++= Seq(scalaJSPipeline),
    isProd := scalaJSStage.value == FullOptStage,
    buildInfoKeys ++= Seq[BuildInfoKey](
      BuildInfoKey("frontName" -> (pimpcloudFrontend / name).value),
      "isProd" -> isProd.value,
      "assetsDir" -> Def.settingDyn(pimpcloudFrontend / assetsRoot).value.toFile,
      "publicDir" -> (Assets / resourceDirectory).value,
      "publicFolder" -> Def.settingDyn(pimpcloudFrontend / assetsPrefix).value,
    ),
    Compile / unmanagedResources ++= ((pimpcloudFrontend / assetsRoot).value.toFile * ("*.css" || "*.js") --- (pimpcloudFrontend / assetsRoot).value.toFile * ("webpack.*.js" || "postcss.config.js")).get,
    fileTreeSources := Seq(
      DirMap(
        (Assets / resourceDirectory).value.toPath,
        "com.malliina.pimpcloud.assets.CloudAssets",
        "com.malliina.pimpcloud.html.CloudTags.at"
      )
    ),
    buildInfoPackage := "com.malliina.pimpcloud",
    linuxPackageSymlinks := linuxPackageSymlinks.value.filterNot(_.link == "/usr/bin/starter"),
    Linux / httpPort := Option("8458"),
    Linux / httpsPort := Option("disabled"),
    maintainer := "Michael Skogberg <malliina123@gmail.com>",
    manufacturer := "Skogberg Labs",
    mainClass := Some("com.malliina.pimpcloud.Starter"),
    Universal / javaOptions ++= {
      val linuxName = (Linux / name).value
      // https://www.scala-sbt.org/sbt-native-packager/archetypes/java_app/customize.html
      Seq(
        "-J-Xmx192m",
        s"-Dgoogle.oauth=/etc/$linuxName/google-oauth.key",
        s"-Dpush.conf=/etc/$linuxName/push.conf",
        s"-Dconfig.file=/etc/$linuxName/production.conf",
        s"-Dpidfile.path=/dev/null",
        s"-Dlog.dir=/var/log/$linuxName"
      )
    },
    Linux / packageSummary := "This is the pimpcloud summary.",
    rpmVendor := "Skogberg Labs",
    libraryDependencies ++= Seq("server", "client").map { module =>
      "org.eclipse.jetty" % s"jetty-alpn-java-$module" % "9.4.20.v20190813"
    }
  )

val it = project
  .in(file("it"))
  .dependsOn(pimpcloud % "test->test", musicpimp % "test->test")
  .settings(baseSettings *)

val pimpbeam = project
  .in(file("pimpbeam"))
  .enablePlugins(
    JavaServerAppPackaging,
    com.malliina.sbt.unix.LinuxPlugin,
    SystemdPlugin,
    BuildInfoPlugin
  )
  .dependsOn(shared, crossJvm)
  .settings((serverSettings ++ http4sServerSettings) *)
  .settings(
    libraryDependencies ++= Seq(
      logstreamsDep,
      "net.glxn" % "qrgen" % "1.4",
      jacksonDep,
    ),
    Linux / httpPort := Option("8557"),
    Linux / httpsPort := Option("disabled"),
    maintainer := "Michael Skogberg <malliina123@gmail.com>",
    Universal / javaOptions ++= {
      val linuxName = (Linux / name).value
      Seq(
        s"-Dconfig.file=/etc/$linuxName/production.conf",
        s"-Dlogger.file=/etc/$linuxName/logback-prod.xml"
      )
    },
    buildInfoPackage := "com.malliina.beam",
    buildInfoKeys ++= Seq[BuildInfoKey](
      "publicDir" -> (Compile / resourceDirectory).value,
    )
  )

import sbtrelease.ReleaseStateTransformations.*

val pimp = project
  .in(file("."))
  .aggregate(
    musicpimp,
    pimpcloud,
    pimpbeam,
    utilAudio,
    crossJvm,
    shared
  )
  .settings(
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )

addCommandAlias("pimp", ";project musicpimp")
addCommandAlias("cloud", ";project pimpcloud")
addCommandAlias("it", ";project it")
ThisBuild / scalacOptions ++= Seq("-unchecked", "-deprecation")

// musicpimp settings

lazy val musicpimpSettings =
  http4sServerSettings ++
    pimpAssetSettings ++
    nativeMusicPimpSettings ++
    artifactSettings ++
    Seq(
      isProd := scalaJSStage.value == FullOptStage,
      buildInfoKeys ++= Seq[BuildInfoKey](
        BuildInfoKey("frontName" -> (musicpimpFrontend / name).value),
        "isProd" -> isProd.value
      ),
      javaOptions ++= Seq("-Dorg.slf4j.simpleLogger.defaultLogLevel=error"),
      // for background, see: http://tpolecat.github.io/2014/04/11/scalac-flags.html
      scalacOptions ++= Seq("-encoding", "UTF-8"),
      libraryDependencies ++= Seq(
        malliinaGroup %% "database" % versions.primitives,
        malliinaGroup %% "okclient-io" % versions.primitives,
        "net.glxn" % "qrgen" % "1.4",
        "it.sauronsoftware.cron4j" % "cron4j" % "2.2.5",
        "mysql" % "mysql-connector-java" % versions.mysql,
        "com.neovisionaries" % "nv-websocket-client" % versions.nvWebSocket,
        httpGroup % "httpclient" % versions.http,
        httpGroup % "httpmime" % versions.http,
        "org.scala-stm" %% "scala-stm" % "0.11.1",
        "ch.vorburger.mariaDB4j" % "mariaDB4j" % "2.4.0",
        "co.fs2" %% "fs2-io" % versions.fs2,
        "com.dimafeng" %% "testcontainers-scala-mysql" % "0.41.8" % Test,
        "org.typelevel" %% "munit-cats-effect" % versions.munitCats % Test
      ).map(dep => dep.withSources()),
      buildInfoPackage := "com.malliina.musicpimp",
      fileTreeSources := Seq(
        DirMap(
          (Assets / resourceDirectory).value.toPath,
          "com.malliina.musicpimp.assets.AppAssets",
          "com.malliina.musicpimp.html.PimpHtml.at"
        ),
        DirMap(
          (Compile / resourceDirectory).value.toPath,
          "com.malliina.musicpimp.licenses.LicenseFiles"
        )
      ),
      libs := libs.value.filter { lib =>
        !lib.toFile.getAbsolutePath
          .endsWith(s"bundles\\nv-websocket-client-${versions.nvWebSocket}.jar")
      },
      Compile / fullClasspath := (Compile / fullClasspath).value.filter { af =>
        !af.data.getAbsolutePath
          .endsWith(s"bundles\\nv-websocket-client-${versions.nvWebSocket}.jar")
      },
      useTerminateProcess := true,
      Windows / msiMappings := (Windows / msiMappings).value.map { case (src, dest) =>
        (
          src,
          Paths.get(
            dest.toString
              .replace('[', '_')
              .replace(']', '_')
              .replace(',', '_')
          )
        )
      },
      minJavaVersion := None,
      Compile / packageDoc / publishArtifact := false,
      packageDoc / publishArtifact := false,
      Compile / doc / sources := Seq.empty
    )

lazy val pimpAssetSettings = assetSettings ++ Seq(
  scalaJSProjects := Seq(musicpimpFrontend),
  Assets / pipelineStages ++= Seq(scalaJSPipeline)
)

def assetSettings = Seq(
  Compile / packageBin / mappings ++=
    (Assets / unmanagedResourceDirectories).value.flatMap { assetDir =>
      assetDir.allPaths pair sbt.io.Path.relativeTo(baseDirectory.value)
    }
)

lazy val nativeMusicPimpSettings =
  pimpWindowsSettings ++
    pimpMacSettings ++
    Seq(
      com.typesafe.sbt.packager.Keys.scriptClasspath := Seq("*"),
      Linux / httpPort := Option("8456"),
      Linux / httpsPort := Option("disabled"),
      maintainer := "Michael Skogberg <malliina123@gmail.com>",
      manufacturer := "Skogberg Labs",
      displayName := "MusicPimp",
      Universal / javaOptions ++= Seq(
        "-Dlogger.resource=prod-logger.xml"
      ),
      // Hack because I want to use log.dir on Linux but not Windows, and "javaOptions in Linux" seems not to work
      bashScriptExtraDefines += """addJava "-Dlog.dir=/var/log/musicpimp"""",
      Linux / packageSummary := "MusicPimp summary here.",
      rpmVendor := "Skogberg Labs",
      rpmLicense := Option("BSD License")
    )

lazy val pimpWindowsSettings = WinPlugin.windowsSettings ++ windowsConfSettings ++ Seq(
  // never change
  WinKeys.upgradeGuid := "5EC7F255-24F9-4E1C-B19D-581626C50F02",
  //  WinKeys.postInstallUrl := Some("http://localhost:8456"),
  WinKeys.forceStopOnUninstall := true,
  Windows / winSwExe := (Windows / pkgHome).value.resolve("WinSW.NET2.exe")
)

lazy val windowsConfSettings = inConfig(Windows)(
  Seq(
    prettyMappings := {
      val out: String = WinKeys.msiMappings.value.map { case (src, dest) =>
        s"$dest\t\t$src"
      }.sorted
        .mkString("\n")
      logger.value.log(Level.Info, out)
    },
    appIcon := Some(pkgHome.value.resolve("guitar-128x128-np.ico")),
    buildAndMove := {
      val src = WinKeys.msi.value
      val dest = Files.move(
        src,
        target.value.toPath.resolve(name.value + ".msi"),
        StandardCopyOption.REPLACE_EXISTING
      )
      streams.value.log.info(s"Moved '$src' to '$dest'.")
      dest
    }
  )
)

lazy val pimpMacSettings = macSettings ++ Seq(
  mainClass := Some("com.malliina.musicpimp.http4s.PimpServer"),
  jvmOptions ++= Seq("-Dhttp.port=8456"),
  launchdConf := Some(defaultLaunchd.value.copy(plistDir = Paths get "/Library/LaunchDaemons")),
  Mac / appIcon := Some((Mac / pkgHome).value.resolve("guitar.icns")),
  pkgIcon := Some((Mac / pkgHome).value.resolve("guitar.png")),
  hideDock := true,
  extraDmgFiles := Seq(
    FileMapping((Mac / pkgHome).value.resolve("guitar.png"), Paths get ".background/.bg.png"),
    FileMapping((Mac / pkgHome).value.resolve("DS_Store"), Paths get ".DS_Store")
  )
)

// pimpcloud settings

lazy val pimpcloudSettings =
  http4sServerSettings ++
    artifactSettings

lazy val artifactSettings = Seq(
  libs ++= Seq(
    (Assets / packageBin).value.toPath,
    (shared / Compile / packageBin).value.toPath,
    (crossJvm / Compile / packageBin).value.toPath,
    (utilAudio / Compile / packageBin).value.toPath
  )
)

def serverSettings = LinusPlugin.playSettings ++ Seq(
  // https://github.com/sbt/sbt-release
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    setNextVersion,
    commitNextVersion,
    pushChanges
  ),
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    "gitHash" -> gitHash
  ),
  libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % "1.5.17",
    "org.slf4j" % "slf4j-api" % "2.0.17",
    "org.scalameta" %% "munit" % versions.munit % Test
  ),
  Debian / packageAndCopy := {
    val deb = (Debian / packageBin).value
    val artifact = (Debian / packageBin).value
    val destName = (Linux / name).value
    val dest = target.value / s"$destName.deb"
    sbt.IO.copyFile(artifact, dest)
    streams.value.log.info(s"Copied '$artifact' to '$dest'.")
    dest
  },
  Debian / packageAndCopy := (Debian / packageAndCopy).dependsOn(Debian / packageBin).value
)

lazy val http4sServerSettings = serverSettings ++ baseSettings ++ Seq(
  libraryDependencies ++= Seq(
    logstreamsDep,
    "org.typelevel" %% "munit-cats-effect" % versions.munitCats % Test
  ).map(dep => dep.withSources())
)

lazy val baseSettings = Seq(
  organization := "org.musicpimp"
)

def scalajsProject(name: String, path: File) =
  Project(name, path)
    .enablePlugins(ScalaJSBundlerPlugin)
    .dependsOn(htmlJs)
    .settings(
      scalaJSUseMainModuleInitializer := true,
      libraryDependencies ++= Seq("org.scalameta" %%% "munit" % versions.munit % Test),
      webpack / version := "5.88.2",
      webpackCliVersion := "5.1.4",
      startWebpackDevServer / version := "4.15.1",
      webpackEmitSourceMaps := false,
      scalaJSUseMainModuleInitializer := true,
      webpackBundlingMode := BundlingMode.LibraryOnly(),
      Compile / npmDependencies ++= Seq(
        "popper.js" -> "1.14.6",
        "bootstrap" -> "4.2.1"
      ),
      Compile / npmDevDependencies ++= Seq(
        "autoprefixer" -> "9.4.3",
        "cssnano" -> "4.1.8",
        "css-loader" -> "6.8.1",
        "file-loader" -> "6.2.0",
        "less" -> "3.9.0",
        "less-loader" -> "11.1.3",
        "mini-css-extract-plugin" -> "2.7.6",
        "postcss-import" -> "12.0.1",
        "postcss-loader" -> "3.0.0",
        "postcss-preset-env" -> "6.5.0",
        "style-loader" -> "3.3.3",
        "url-loader" -> "4.1.1",
        "webpack-merge" -> "4.1.5"
      ),
      Compile / additionalNpmConfig := Map(
        "private" -> JSON.bool(true),
        "license" -> JSON.str("BSD")
      ),
      fastOptJS / webpackConfigFile := Some(baseDirectory.value / "webpack.dev.config.js"),
      fullOptJS / webpackConfigFile := Some(baseDirectory.value / "webpack.prod.config.js"),
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % versions.scalaJsDom,
        "com.lihaoyi" %%% "scalatags" % versions.scalatags
      )
    )

def gitHash: String =
  Try(Process("git rev-parse --short HEAD").lineStream.head).toOption.getOrElse("unknown")

Global / onChangedBuildSource := ReloadOnSourceChanges
