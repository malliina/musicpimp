package org.musicpimp.js

import org.scalajs.dom

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

object Frontend {

  var app: Option[BaseScript] = None
  var footer: Option[FooterSocket] = None

  def main(args: Array[String]): Unit = {
    val path = dom.window.location.pathname
    val front: PartialFunction[String, BaseScript] = {
      case "/search" => new Search(new MusicItems)
      case "/logs" => new Logs
      case "/player" => new Playback
      case "/alarms" => new Alarms
      case "/alarms/editor" => new AlarmEditor
      case "/cloud" => new Cloud
      case p if containsMusic(p) => new MusicItems
    }
    app = front lift path
    footer = Option(new FooterSocket)
  }

  def containsMusic(p: String) =
    p.isEmpty || p == "/" || p.startsWith("/folders") ||
      p == "/player/recent" || p == "/player/popular"
}
