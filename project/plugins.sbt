scalaVersion := "2.12.10"
scalacOptions ++= Seq("-unchecked", "-deprecation", "-language:implicitConversions")

Seq(
  "com.typesafe.play" % "sbt-plugin" % "2.8.1",
  "com.malliina" % "sbt-utils-maven" % "0.16.1",
  "com.malliina" % "sbt-nodejs" % "0.16.1",
  "com.malliina" % "sbt-filetree" % "0.4.1",
  "com.malliina" % "sbt-packager" % "2.8.4",
  "org.scala-js" % "sbt-scalajs" % "0.6.32",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.0",
  "com.vmunier" % "sbt-web-scalajs" % "1.0.6",
  "com.typesafe.sbt" % "sbt-digest" % "1.1.4",
  "com.typesafe.sbt" % "sbt-gzip" % "1.0.2",
  "com.typesafe.sbt" % "sbt-less" % "1.1.2",
  "com.eed3si9n" % "sbt-buildinfo" % "0.9.0",
  "com.github.gseitz" % "sbt-release" % "1.0.11",
  "ch.epfl.scala" % "sbt-web-scalajs-bundler-sjs06" % "0.17.0",
  "ch.epfl.scala" % "sbt-bloop" % "1.3.4",
  "org.scalameta" % "sbt-scalafmt" % "2.3.0"
) map addSbtPlugin
