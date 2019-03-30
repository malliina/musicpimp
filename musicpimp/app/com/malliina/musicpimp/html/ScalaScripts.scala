package com.malliina.musicpimp.html

case class ScalaScripts(jsFiles: Seq[String])

object ScalaScripts {

  /**
    * @param appName typically the name of the Scala.js module
    * @param isProd  true if the app runs in production, false otherwise
    * @return HTML templates with either prod or dev javascripts
    */
  def forApp(appName: String, isProd: Boolean): ScalaScripts = {
    val name = appName.toLowerCase
    val opt = if (isProd) "opt" else "fastopt"
    ScalaScripts(Seq(s"$name-$opt-library.js", s"$name-$opt-loader.js", s"$name-$opt.js"))
  }
}
