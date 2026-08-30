package com.malliina.pimpcloud.tags

case class ScalaScripts(scripts: Seq[String])

object ScalaScripts:
  val default = ScalaScripts(Seq("main.js"))
