package org.musicpimp.js

import org.scalajs.dom

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

object Frontend extends JSApp {

  var app: Option[BaseScript] = None

  @JSExport
  override def main() = {
    val path = dom.window.location.pathname
    val front: PartialFunction[String, BaseScript] = {
      case "/search" | "/player2" => new Search
      case "/logs" | "/logs2" => new Logs
      case "/player" | "/player2" => new Playback
      case p if p.isEmpty || p == "/" || p.startsWith("/folders") => new Library
    }
    app = front.lift(path)
  }
}
