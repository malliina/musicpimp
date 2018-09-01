package com.malliina.pimpcloud.js

import org.scalajs.dom

object Frontend {
  var app: Option[SocketJS] = None

  def main(args: Array[String]): Unit = {
    val path = dom.window.location.pathname
    val jsImpl: PartialFunction[String, SocketJS] = {
      case "/admin/logs" => new LogsJS
      case "/admin" => new AdminJS
      case p if p.isEmpty || p == "/" || p.startsWith("/folders") => new PlayerJS
    }

    app = jsImpl.lift(path)
  }
}
