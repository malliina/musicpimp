package com.malliina.pimpcloud.tags

case class ScalaScripts(scripts: Seq[String])

object ScalaScripts:
  def forApp(appName: String, isProd: Boolean): ScalaScripts =
    val name = appName.toLowerCase
    val opt = if isProd then "opt" else "fastopt"
    ScalaScripts(Seq(s"$name-$opt-library.js", s"$name-$opt-loader.js", s"$name-$opt.js"))
