scalaVersion := "2.12.18"
scalacOptions ++= Seq("-unchecked", "-deprecation", "-language:implicitConversions")

val utilsVersion = "1.6.40"

Seq(
  "org.playframework" % "sbt-plugin" % "3.0.5",
  "com.malliina" % "sbt-utils-maven" % utilsVersion,
  "com.malliina" % "sbt-nodejs" % utilsVersion,
  "com.malliina" % "sbt-filetree" % utilsVersion,
  "com.malliina" % "sbt-packager" % "2.10.1",
  "org.scala-js" % "sbt-scalajs" % "1.17.0",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2",
  "com.vmunier" % "sbt-web-scalajs" % "1.2.0",
  "com.typesafe.sbt" % "sbt-digest" % "1.1.4",
  "com.typesafe.sbt" % "sbt-gzip" % "1.0.2",
  "com.typesafe.sbt" % "sbt-less" % "1.1.2",
  "com.eed3si9n" % "sbt-buildinfo" % "0.11.0",
  "ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.21.1",
  "org.scalameta" % "sbt-scalafmt" % "2.5.2",
  "com.eed3si9n" % "sbt-assembly" % "1.2.0"
) map addSbtPlugin

//libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
