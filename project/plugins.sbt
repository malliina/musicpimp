scalaVersion := "2.12.4"
resolvers ++= Seq(
  ivyRepo("bintray-sbt-plugin-releases",
    "http://dl.bintray.com/content/sbt/sbt-plugin-releases"),
  ivyRepo("malliina bintray sbt",
    "https://dl.bintray.com/malliina/sbt-plugins/"),
  Resolver.bintrayRepo("malliina", "maven")
)
scalacOptions ++= Seq("-unchecked", "-deprecation", "-language:implicitConversions")

Seq(
  "com.malliina" %% "sbt-play" % "1.2.0",
  "com.malliina" %% "sbt-packager" % "2.4.1",
  "org.scala-js" % "sbt-scalajs" % "0.6.20",
  "com.vmunier" % "sbt-web-scalajs" % "1.0.6",
  "com.malliina" % "sbt-filetree" % "0.2.0",
  "com.typesafe.sbt" % "sbt-digest" % "1.1.4",
  "com.typesafe.sbt" % "sbt-gzip" % "1.0.2",
  "com.typesafe.sbt" % "sbt-less" % "1.1.2"
) map addSbtPlugin

def ivyRepo(name: String, urlString: String) =
  Resolver.url(name, url(urlString))(Resolver.ivyStylePatterns)
