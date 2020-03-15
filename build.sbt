import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import com.malliina.appbundler.FileMapping
import com.malliina.sbt.GenericKeys._
import com.malliina.sbt.filetree.DirMap
import com.malliina.sbt.mac.MacKeys._
import com.malliina.sbt.mac.MacPlugin.{Mac, macSettings}
import com.malliina.sbt.unix.LinuxKeys.{appHome, ciBuild, httpPort, httpsPort}
import com.malliina.sbt.unix.{LinuxPlugin => LinusPlugin}
import com.malliina.sbt.win.WinKeys.{msiMappings, useTerminateProcess, winSwExe, minJavaVersion}
import com.malliina.sbt.win.{WinKeys, WinPlugin}
import com.typesafe.sbt.SbtNativePackager.Windows
import com.typesafe.sbt.packager.Keys.{maintainer, packageSummary, rpmVendor}
import play.sbt.PlayImport
import play.sbt.routes.RoutesKeys
import sbt.Keys.scalaVersion
import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoKeys.{buildInfoKeys, buildInfoPackage}
import sbtcrossproject.CrossPlugin.autoImport.{
  CrossType => PortableType,
  crossProject => portableProject
}
import sbtrelease.ReleaseStateTransformations.{checkSnapshotDependencies, runTest}
import scalajsbundler.util.JSON

import scala.sys.process.Process
import scala.util.Try

val prettyMappings = taskKey[Unit]("Prints the file mappings, prettily")
// wtf?
val release = taskKey[Unit]("Uploads native msi, deb and rpm packages to azure")
val buildAndMove = taskKey[Path]("builds and moves the package")
val bootClasspath = taskKey[String]("bootClasspath")

val musicpimpVersion = "4.23.1"
val pimpcloudVersion = "1.28.0"
val sharedVersion = "1.13.0"
val crossVersion = "1.13.0"

val utilAudioVersion = "2.8.0"
val primitivesVersion = "1.13.0"
val playJsonVersion = "2.8.1"
val scalaTagsVersion = "0.8.4"
val utilPlayVersion = "5.4.1"
val httpVersion = "4.5.11"
val mysqlVersion = "5.1.48"
val nvWebSocketVersion = "2.9"
val scalaTestVersion = "3.0.8"
val akkaStreamsVersion = "2.6.1"

val malliinaGroup = "com.malliina"
val soundGroup = "com.googlecode.soundlibs"
val utilPlayDep = malliinaGroup %% "util-play" % utilPlayVersion
val logstreamsDep = malliinaGroup %% "logstreams-client" % "1.8.2"

val httpGroup = "org.apache.httpcomponents"

scalaVersion in ThisBuild := "2.13.1"

val utilAudio = Project("util-audio", file("util-audio"))
  .enablePlugins(MavenCentralPlugin)
  .settings(
    version := utilAudioVersion,
    organization := malliinaGroup,
    gitUserName := "malliina",
    developerName := "Michael Skogberg",
    libraryDependencies ++= Seq(
      "commons-io" % "commons-io" % "2.6",
      "org.slf4j" % "slf4j-api" % "1.7.25",
      malliinaGroup %% "util-base" % primitivesVersion,
      "org" % "jaudiotagger" % "2.0.3",
      soundGroup % "tritonus-share" % "0.3.7.4",
      soundGroup % "jlayer" % "1.0.1.4",
      soundGroup % "mp3spi" % "1.9.5.4",
      "com.typesafe.akka" %% "akka-stream" % akkaStreamsVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
    )
  )

val cross = portableProject(JSPlatform, JVMPlatform)
  .crossType(PortableType.Full)
  .in(file("cross"))
  .settings(
    organization := "org.musicpimp",
    version := crossVersion,
    libraryDependencies ++= Seq(
      "com.typesafe.play" %%% "play-json" % playJsonVersion,
      "com.lihaoyi" %%% "scalatags" % scalaTagsVersion,
      malliinaGroup %%% "primitives" % primitivesVersion,
      malliinaGroup %%% "util-html" % utilPlayVersion
    )
  )
  .jsSettings(libraryDependencies += "be.doeraene" %%% "scalajs-jquery" % "0.9.5")
val crossJvm = cross.jvm
val crossJs = cross.js
  .enablePlugins(ScalaJSBundlerPlugin, ScalaJSWeb)
  .settings(
    npmDependencies in Compile ++= Seq(
      "jquery" -> "3.3.1",
      "jquery-ui" -> "1.12.1"
    )
  )

val shared = Project("pimp-shared", file("shared"))
  .dependsOn(crossJvm)
  .settings(baseSettings: _*)
  .settings(
    version := sharedVersion,
    libraryDependencies ++= Seq(
      "io.getquill" %% "quill-jdbc" % "3.5.0",
      "org.flywaydb" % "flyway-core" % "6.0.3",
      "mysql" % "mysql-connector-java" % mysqlVersion,
      malliinaGroup %% "mobile-push" % "1.22.0",
      utilPlayDep
    )
  )

val musicpimpFrontend = scalajsProject("musicpimp-frontend", file("musicpimp") / "frontend")
  .dependsOn(crossJs)
val musicpimp = project
  .in(file("musicpimp"))
  .enablePlugins(
    PlayScala,
    JavaServerAppPackaging,
    SystemdPlugin,
    BuildInfoPlugin,
    FileTreePlugin,
    WebScalaJSBundlerPlugin
  )
  .dependsOn(shared, crossJvm, utilAudio)
  .settings(pimpPlaySettings: _*)

val pimpcloudFrontend = scalajsProject("pimpcloud-frontend", file("pimpcloud") / "frontend")
  .dependsOn(crossJs)
val pimpcloud = project
  .in(file("pimpcloud"))
  .enablePlugins(
    PlayScala,
    JavaServerAppPackaging,
    SystemdPlugin,
    BuildInfoPlugin,
    FileTreePlugin,
    WebScalaJSBundlerPlugin
  )
  .dependsOn(shared, shared % Test, crossJvm)
  .settings(pimpcloudSettings: _*)

val it = project
  .in(file("it"))
  .dependsOn(pimpcloud % "test->test", musicpimp % "test->test")
  .settings(baseSettings: _*)

val metaCommonSettings = Seq(
  version := "1.12.0",
  scalacOptions := Seq("-unchecked", "-deprecation")
)
val musicmetaFrontend = scalajsProject("musicmeta-frontend", file("musicmeta") / "frontend")
  .settings(metaCommonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % scalaTagsVersion,
      "be.doeraene" %%% "scalajs-jquery" % "0.9.5",
      "com.typesafe.play" %%% "play-json" % playJsonVersion,
      "com.malliina" %%% "primitives" % primitivesVersion
    ),
    npmDependencies in Compile ++= Seq("jquery" -> "3.3.1")
  )
val musicmeta = project
  .in(file("musicmeta"))
  .enablePlugins(
    PlayScala,
    JavaServerAppPackaging,
    SystemdPlugin,
    BuildInfoPlugin,
    FileTreePlugin,
    WebScalaJSBundlerPlugin
  )
  .settings(serverSettings ++ metaCommonSettings)
  .settings(
    scalaJSProjects := Seq(musicmetaFrontend),
    pipelineStages in Assets := Seq(scalaJSPipeline),
    libraryDependencies ++= Seq(
      "commons-codec" % "commons-codec" % "1.13",
      logstreamsDep,
      malliinaGroup %% "play-social" % utilPlayVersion,
      utilPlayDep,
      utilPlayDep % Test classifier "tests"
    ),
    httpPort in Linux := Option("disabled"),
    httpsPort in Linux := Option("8460"),
    maintainer := "Michael Skogberg <malliina123@gmail.com>",
    javaOptions in Universal ++= {
      val linuxName = (name in Linux).value
      val metaHome = (appHome in Linux).value
      Seq(
        s"-Ddiscogs.oauth=/etc/$linuxName/discogs-oauth.key",
        s"-Dgoogle.oauth=/etc/$linuxName/google-oauth.key",
        s"-Dcover.dir=$metaHome/covers",
        s"-Dconfig.file=/etc/$linuxName/production.conf",
        s"-Dlogger.file=/etc/$linuxName/logback-prod.xml",
        "-Dfile.encoding=UTF-8",
        "-Dsun.jnu.encoding=UTF-8"
      )
    },
    pipelineStages := Seq(digest, gzip),
    buildInfoKeys ++= Seq[BuildInfoKey](
      "frontName" -> (name in musicmetaFrontend).value
    ),
    buildInfoPackage := "com.malliina.musicmeta",
    linuxPackageSymlinks := linuxPackageSymlinks.value.filterNot(_.link == "/usr/bin/starter")
  )

val pimpbeam = project
  .in(file("pimpbeam"))
  .enablePlugins(
    PlayScala,
    JavaServerAppPackaging,
    com.malliina.sbt.unix.LinuxPlugin,
    SystemdPlugin,
    BuildInfoPlugin
  )
  .settings(serverSettings: _*)
  .settings(
    version := "2.1.0",
    libraryDependencies ++= Seq(
      utilPlayDep,
      logstreamsDep,
      "net.glxn" % "qrgen" % "1.4",
      PlayImport.ws
    ),
    resolvers += Resolver.bintrayRepo("malliina", "maven"),
    httpPort in Linux := Option("8557"),
    httpsPort in Linux := Option("disabled"),
    maintainer := "Michael Skogberg <malliina123@gmail.com>",
    javaOptions in Universal ++= {
      val linuxName = (name in Linux).value
      Seq(
        s"-Dconfig.file=/etc/$linuxName/production.conf",
        s"-Dlogger.file=/etc/$linuxName/logback-prod.xml"
      )
    },
    buildInfoPackage := "com.malliina.beam"
  )

val pimp = project.in(file(".")).aggregate(musicpimp, pimpcloud, musicmeta, pimpbeam)

addCommandAlias("pimp", ";project musicpimp")
addCommandAlias("cloud", ";project pimpcloud")
addCommandAlias("it", ";project it")
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

// musicpimp settings

lazy val pimpPlaySettings =
  commonServerSettings ++
    pimpAssetSettings ++
    nativeMusicPimpSettings ++
    artifactSettings ++
    Seq(
      version := musicpimpVersion,
      buildInfoKeys += BuildInfoKey("frontName" -> (name in musicpimpFrontend).value),
      javaOptions ++= Seq("-Dorg.slf4j.simpleLogger.defaultLogLevel=error"),
      // for background, see: http://tpolecat.github.io/2014/04/11/scalac-flags.html
      scalacOptions ++= Seq("-encoding", "UTF-8"),
      libraryDependencies ++= Seq(
        malliinaGroup %% "util-base" % primitivesVersion,
        "net.glxn" % "qrgen" % "1.4",
        "it.sauronsoftware.cron4j" % "cron4j" % "2.2.5",
        "mysql" % "mysql-connector-java" % mysqlVersion,
        "com.neovisionaries" % "nv-websocket-client" % nvWebSocketVersion,
        httpGroup % "httpclient" % httpVersion,
        httpGroup % "httpmime" % httpVersion,
        "org.scala-stm" %% "scala-stm" % "0.9.1",
        "ch.vorburger.mariaDB4j" % "mariaDB4j" % "2.4.0"
      ).map(dep => dep withSources ()),
      buildInfoPackage := "com.malliina.musicpimp",
      RoutesKeys.routesImport ++= Seq(
        "com.malliina.musicpimp.http.PimpImports._",
        "com.malliina.musicpimp.models._",
        "com.malliina.values.Username"
      ),
      fileTreeSources := Seq(
        DirMap(
          (resourceDirectory in Assets).value,
          "com.malliina.musicpimp.assets.AppAssets",
          "com.malliina.musicpimp.html.PimpHtml.at"
        ),
        DirMap((resourceDirectory in Compile).value, "com.malliina.musicpimp.licenses.LicenseFiles")
      ),
      libs := libs.value.filter { lib =>
        !lib.toFile.getAbsolutePath
          .endsWith(s"bundles\\nv-websocket-client-$nvWebSocketVersion.jar")
      },
      fullClasspath in Compile := (fullClasspath in Compile).value.filter { af =>
        !af.data.getAbsolutePath.endsWith(s"bundles\\nv-websocket-client-$nvWebSocketVersion.jar")
      },
      useTerminateProcess := true,
      msiMappings in Windows := (msiMappings in Windows).value.map {
        case (src, dest) =>
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
      publishArtifact in (Compile, packageDoc) := false,
      publishArtifact in packageDoc := false,
      sources in (Compile, doc) := Seq.empty
    )

lazy val pimpAssetSettings = assetSettings ++ Seq(
  scalaJSProjects := Seq(musicpimpFrontend),
  pipelineStages in Assets ++= Seq(scalaJSPipeline)
)

def assetSettings = Seq(
  mappings in (Compile, packageBin) ++= {
    (unmanagedResourceDirectories in Assets).value.flatMap { assetDir =>
      assetDir.allPaths pair sbt.io.Path.relativeTo(baseDirectory.value)
    }
  }
)

lazy val nativeMusicPimpSettings =
  pimpWindowsSettings ++
    pimpMacSettings ++
    Seq(
      com.typesafe.sbt.packager.Keys.scriptClasspath := Seq("*"),
      httpPort in Linux := Option("8456"),
      httpsPort in Linux := Option("disabled"),
      maintainer := "Michael Skogberg <malliina123@gmail.com>",
      manufacturer := "Skogberg Labs",
      displayName := "MusicPimp",
      javaOptions in Universal ++= Seq(
        "-Dlogger.resource=prod-logger.xml"
      ),
      // Hack because I want to use log.dir on Linux but not Windows, and "javaOptions in Linux" seems not to work
      bashScriptExtraDefines += """addJava "-Dlog.dir=/var/log/musicpimp"""",
      packageSummary in Linux := "MusicPimp summary here.",
      rpmVendor := "Skogberg Labs",
      rpmLicense := Option("BSD License"),
      PlayKeys.externalizeResources := false // packages files in /conf to the app jar
    )

lazy val pimpWindowsSettings = WinPlugin.windowsSettings ++ windowsConfSettings ++ Seq(
  // never change
  WinKeys.upgradeGuid := "5EC7F255-24F9-4E1C-B19D-581626C50F02",
  WinKeys.minJavaVersion := Some(8),
  //  WinKeys.postInstallUrl := Some("http://localhost:8456"),
  WinKeys.forceStopOnUninstall := true,
  winSwExe in Windows := (pkgHome in Windows).value.resolve("WinSW.NET2.exe")
)

lazy val windowsConfSettings = inConfig(Windows)(
  Seq(
    prettyMappings := {
      val out: String = WinKeys.msiMappings.value.map {
        case (src, dest) => s"$dest\t\t$src"
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
  mainClass := Some("play.core.server.ProdServerStart"),
  jvmOptions ++= Seq("-Dhttp.port=8456"),
  launchdConf := Some(defaultLaunchd.value.copy(plistDir = Paths get "/Library/LaunchDaemons")),
  appIcon in Mac := Some((pkgHome in Mac).value.resolve("guitar.icns")),
  pkgIcon := Some((pkgHome in Mac).value.resolve("guitar.png")),
  hideDock := true,
  extraDmgFiles := Seq(
    FileMapping((pkgHome in Mac).value.resolve("guitar.png"), Paths get ".background/.bg.png"),
    FileMapping((pkgHome in Mac).value.resolve("DS_Store"), Paths get ".DS_Store")
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
      libraryDependencies ++= Seq(
        malliinaGroup %% "play-social" % utilPlayVersion,
        PlayImport.ehcache
      ),
      PlayKeys.externalizeResources := false,
      fileTreeSources := Seq(
        DirMap(
          (resourceDirectory in Assets).value,
          "com.malliina.pimpcloud.assets.CloudAssets",
          "controllers.pimpcloud.CloudTags.at"
        )
      ),
      buildInfoPackage := "com.malliina.pimpcloud",
      linuxPackageSymlinks := linuxPackageSymlinks.value.filterNot(_.link == "/usr/bin/starter")
    )

lazy val pimpcloudLinuxSettings = Seq(
  httpPort in Linux := Option("disabled"),
  httpsPort in Linux := Option("8458"),
  maintainer := "Michael Skogberg <malliina123@gmail.com>",
  manufacturer := "Skogberg Labs",
  mainClass := Some("com.malliina.pimpcloud.Starter"),
  javaOptions in Universal ++= {
    val linuxName = (name in Linux).value
    // https://www.scala-sbt.org/sbt-native-packager/archetypes/java_app/customize.html
    Seq(
      "-J-Xmx192m",
      s"-Dgoogle.oauth=/etc/$linuxName/google-oauth.key",
      s"-Dpush.conf=/etc/$linuxName/push.conf",
      s"-Dlogger.resource=/etc/$linuxName/logback-prod.xml",
      s"-Dlogger.file=/etc/$linuxName/logback-prod.xml",
      s"-Dconfig.file=/etc/$linuxName/production.conf",
      s"-Dpidfile.path=/dev/null",
      s"-Dlog.dir=/var/log/$linuxName"
    )
  },
  packageSummary in Linux := "This is the pimpcloud summary.",
  rpmVendor := "Skogberg Labs",
  libraryDependencies ++= Seq(
    "org.eclipse.jetty" % "jetty-alpn-java-server" % "9.4.20.v20190813",
    "org.eclipse.jetty" % "jetty-alpn-java-client" % "9.4.20.v20190813",
    "com.typesafe.akka" %% "akka-http" % "10.1.11"
  )
)

lazy val artifactSettings = Seq(
  libs ++= Seq(
    (packageBin in Assets).value.toPath,
    (packageBin in shared in Compile).value.toPath,
    (packageBin in crossJvm in Compile).value.toPath,
    (packageBin in utilAudio in Compile).value.toPath
  )
)

lazy val pimpcloudScalaJSSettings = Seq(
  scalaJSProjects := Seq(pimpcloudFrontend),
  pipelineStages in Assets ++= Seq(scalaJSPipeline)
)

def serverSettings = LinusPlugin.playSettings ++ Seq(
  // https://github.com/sbt/sbt-release
  releaseProcess := Seq[ReleaseStep](
    releaseStepTask(clean in Compile),
    checkSnapshotDependencies,
    runTest,
    releaseStepTask(ciBuild)
  ),
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    "gitHash" -> gitHash
  ),
  RoutesKeys.routesGenerator := InjectedRoutesGenerator,
  libraryDependencies ++= defaultDeps
)

def libSettings = Seq(
  libraryDependencies ++= defaultDeps
)

def defaultDeps = Seq(
  "com.lihaoyi" %% "scalatags" % scalaTagsVersion,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
  PlayImport.specs2 % Test
)

lazy val commonServerSettings = serverSettings ++ baseSettings ++ Seq(
  libraryDependencies ++= Seq(
    utilPlayDep,
    utilPlayDep % Test classifier "tests",
    logstreamsDep,
    PlayImport.filters
  ).map(dep => dep.withSources()),
  RoutesKeys.routesImport ++= Seq(
    "com.malliina.musicpimp.http.PimpImports._",
    "com.malliina.musicpimp.models._",
    "com.malliina.values.Username"
  ),
  pipelineStages ++= Seq(digest, gzip)
)

lazy val baseSettings = Seq(
  organization := "org.musicpimp"
)

def scalajsProject(name: String, path: File) =
  Project(name, path)
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      scalaJSUseMainModuleInitializer := true,
      libraryDependencies ++= Seq("org.scalatest" %%% "scalatest" % scalaTestVersion % Test),
      testFrameworks += new TestFramework("utest.runner.Framework"),
      version in webpack := "4.35.2",
      emitSourceMaps := false,
      webpackEmitSourceMaps := false,
      scalaJSUseMainModuleInitializer := true,
      webpackBundlingMode := BundlingMode.LibraryOnly(),
      npmDependencies in Compile ++= Seq(
//        "jquery" -> "3.3.1",
        "popper.js" -> "1.14.6",
        "bootstrap" -> "4.2.1"
      ),
      npmDevDependencies in Compile ++= Seq(
        "autoprefixer" -> "9.4.3",
        "cssnano" -> "4.1.8",
        "css-loader" -> "3.0.0",
        "file-loader" -> "4.0.0",
        "less" -> "3.9.0",
        "less-loader" -> "4.1.0",
        "mini-css-extract-plugin" -> "0.7.0",
        "postcss-import" -> "12.0.1",
        "postcss-loader" -> "3.0.0",
        "postcss-preset-env" -> "6.5.0",
        "style-loader" -> "0.23.1",
        "url-loader" -> "1.1.2",
        "webpack-merge" -> "4.1.5"
      ),
      additionalNpmConfig in Compile := Map(
        "engines" -> JSON.obj("node" -> JSON.str("10.x")),
        "private" -> JSON.bool(true),
        "license" -> JSON.str("BSD")
      ),
      webpackConfigFile in fastOptJS := Some(baseDirectory.value / "webpack.dev.config.js"),
      webpackConfigFile in fullOptJS := Some(baseDirectory.value / "webpack.prod.config.js")
    )

def gitHash: String =
  Try(Process("git rev-parse --short HEAD").lineStream.head).toOption.getOrElse("unknown")

Global / onChangedBuildSource := ReloadOnSourceChanges
