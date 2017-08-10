scalaVersion := "2.10.6"
resolvers ++= Seq(
  ivyRepo("bintray-sbt-plugin-releases",
    "http://dl.bintray.com/content/sbt/sbt-plugin-releases"),
  ivyRepo("malliina bintray sbt",
    "https://dl.bintray.com/malliina/sbt-plugins/"),
  Resolver.bintrayRepo("malliina", "maven")
)
scalacOptions ++= Seq("-unchecked", "-deprecation", "-language:implicitConversions")

Seq(
  "com.malliina" %% "sbt-play" % "1.1.0",
  "com.malliina" %% "sbt-jenkins-control" % "0.3.1",
  "com.eed3si9n" % "sbt-assembly" % "0.11.2",
  "org.scala-js" % "sbt-scalajs" % "0.6.19",
  "com.vmunier" % "sbt-web-scalajs" % "1.0.3",
  "com.malliina" % "sbt-filetree" % "0.1.1"
) map addSbtPlugin

def ivyRepo(name: String, urlString: String) =
  Resolver.url(name, url(urlString))(Resolver.ivyStylePatterns)
