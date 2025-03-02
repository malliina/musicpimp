scalaVersion := "2.12.20"
scalacOptions ++= Seq("-unchecked", "-deprecation", "-language:implicitConversions")

val utilsVersion = "1.6.46"

Seq(
  "org.playframework" % "sbt-plugin" % "3.0.5",
  "com.malliina" % "sbt-utils-maven" % utilsVersion,
  "com.malliina" % "sbt-nodejs" % utilsVersion,
  "com.malliina" % "sbt-filetree" % utilsVersion,
  "com.malliina" % "sbt-packager" % "2.10.1",
  "org.scala-js" % "sbt-scalajs" % "1.18.2",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2",
  "com.vmunier" % "sbt-web-scalajs" % "1.3.0",
  "com.typesafe.sbt" % "sbt-digest" % "1.1.4",
  "com.typesafe.sbt" % "sbt-gzip" % "1.0.2",
  "com.typesafe.sbt" % "sbt-less" % "1.1.2",
  "com.eed3si9n" % "sbt-buildinfo" % "0.13.1",
  "ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.21.1",
  "org.scalameta" % "sbt-scalafmt" % "2.5.4",
  "com.eed3si9n" % "sbt-assembly" % "2.3.1"
) map addSbtPlugin
