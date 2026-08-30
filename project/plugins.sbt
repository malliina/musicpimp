scalaVersion := "2.12.20"
scalacOptions ++= Seq("-unchecked", "-deprecation", "-language:implicitConversions")

val utilsVersion = "1.7.1"

Seq(
  "com.malliina" % "sbt-utils-maven" % utilsVersion,
  "com.malliina" % "sbt-nodejs" % utilsVersion,
  "com.malliina" % "sbt-filetree" % utilsVersion,
  "com.malliina" % "sbt-revolver-rollup" % utilsVersion,
  "com.malliina" % "sbt-packager" % "2.10.1",
  "org.scala-js" % "sbt-scalajs" % "1.21.0",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2",
  "com.eed3si9n" % "sbt-buildinfo" % "0.13.1",
  "org.scalameta" % "sbt-scalafmt" % "2.5.4",
  "com.eed3si9n" % "sbt-assembly" % "2.3.1"
) map addSbtPlugin
