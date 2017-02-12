package com.malliina.pimpcloud.js

import org.scalajs.dom

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

object Frontend extends JSApp {
  var app: Option[SocketJS] = None

  @JSExport
  override def main() = {
    val path = dom.window.location.pathname
    val jsImpl: PartialFunction[String, SocketJS] = {
      case "/admin/logs" => new LogsJS
      case "/admin" => new AdminJS
      case p if p.isEmpty || p == "/" || p.startsWith("/folders") => new PlayerJS
    }

    app = jsImpl.lift(path)
  }
}
