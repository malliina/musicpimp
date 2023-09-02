scalaVersion := "2.12.12"
scalacOptions ++= Seq("-unchecked", "-deprecation", "-language:implicitConversions")

Seq(
  "com.typesafe.play" % "sbt-plugin" % "2.8.7",
  "com.malliina" % "sbt-utils-maven" % "1.0.0",
  "com.malliina" % "sbt-nodejs" % "1.0.0",
  "com.malliina" % "sbt-filetree" % "0.4.1",
  "com.malliina" % "sbt-packager" % "2.9.0",
  "org.scala-js" % "sbt-scalajs" % "1.13.2",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2",
  "com.vmunier" % "sbt-web-scalajs" % "1.2.0",
  "com.typesafe.sbt" % "sbt-digest" % "1.1.4",
  "com.typesafe.sbt" % "sbt-gzip" % "1.0.2",
  "com.typesafe.sbt" % "sbt-less" % "1.1.2",
  "com.eed3si9n" % "sbt-buildinfo" % "0.10.0",
  "com.github.gseitz" % "sbt-release" % "1.0.13",
  "ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.21.1",
  "ch.epfl.scala" % "sbt-bloop" % "1.4.6",
  "org.scalameta" % "sbt-scalafmt" % "2.4.2"
) map addSbtPlugin
