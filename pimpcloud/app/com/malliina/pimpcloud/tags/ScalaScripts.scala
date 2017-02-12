package com.malliina.pimpcloud.tags

case class ScalaScripts(optimized: String, launcher: String, libs: String)

object ScalaScripts {
  /**
    * @param appName typically the name of the Scala.js module
    * @param isProd  true if the app runs in production, false otherwise
    * @return HTML templates with either prod or dev javascripts
    */
  def forApp(appName: String, isProd: Boolean): ScalaScripts = {
    val lowerName = appName.toLowerCase
    val suffix = if (isProd) "opt" else "fastopt"

    def withSuffix(suff: String) = s"$lowerName-$suff.js"

    val optimizedName = withSuffix(suffix)
    val launcherName = withSuffix("launcher")
    val libs = withSuffix("jsdeps")
    ScalaScripts(optimizedName, launcherName, libs)
  }
}
