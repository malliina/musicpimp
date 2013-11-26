import sbt._

object Dependencies {
  val utilVersion = "0.7.1"
  val utilGroup = "com.github.malliina"
  val utilDep = utilGroup %% "util" % "1.0.0"
  val utilActor = utilGroup %% "util-actor" % utilVersion
  val utilRmi = utilGroup %% "util-rmi" % utilVersion
  val utilPlay = utilGroup %% "util-play" % "1.0.0"
  val scalaTest = "org.scalatest" %% "scalatest" % "1.9.2" % "test"
  val jodaTime = "joda-time" % "joda-time" % "2.1"
  val jodaConvert = "org.joda" % "joda-convert" % "1.3"
  val jAudioTagger = "org" % "jaudiotagger" % "2.0.3"
  val h2 = "com.h2database" % "h2" % "1.3.173"
  val slick = "com.typesafe.slick" %% "slick" % "1.0.1"
//  val playExtras = "com.typesafe.play.extras" %% "iteratees-extras" % "1.0.1"
  // used by play but play has an open-ended version [3.1.4)
  // force this instead to speed up build
  val ebeanOrm = "org.avaje.ebeanorm" % "avaje-ebeanorm-agent" % "3.2.1"
  val httpGroup = "org.apache.httpcomponents"
  val httpVersion = "4.3"
  val httpClient = httpGroup % "httpclient" % httpVersion
  val httpMime = httpGroup % "httpmime" % httpVersion
}