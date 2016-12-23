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
      case "/player" => new Playback
      case "/alarms" => new Alarms
      case "/alarms/editor" => new AlarmEditor
      case p if containsMusic(p) => new MusicItems
    }
    app = front lift path
  }

  def containsMusic(p: String) =
    p.isEmpty || p == "/" || p.startsWith("/folders") ||
      p == "/player/recent" || p == "/player/popular"
}
