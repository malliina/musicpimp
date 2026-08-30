package com.malliina.musicpimp.html

case class ScalaScripts(jsFiles: Seq[String])

object ScalaScripts:
  val default = ScalaScripts(Seq("main.js"))
