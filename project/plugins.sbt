val utilsVersion = "2.0.5"

Seq(
  "com.malliina" % "sbt-utils-maven" % utilsVersion,
  "com.malliina" % "sbt-nodejs" % utilsVersion,
  "com.malliina" % "sbt-filetree" % utilsVersion,
  "com.malliina" % "sbt-revolver-rollup" % utilsVersion,
  "com.malliina" % "sbt-packager" % "3.0.0",
  "org.scala-js" % "sbt-scalajs" % "1.22.0",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.4.0",
  "com.eed3si9n" % "sbt-buildinfo" % "0.13.1",
  "org.scalameta" % "sbt-scalafmt" % "2.6.1",
  "com.eed3si9n" % "sbt-assembly" % "2.5.0"
) map addSbtPlugin
