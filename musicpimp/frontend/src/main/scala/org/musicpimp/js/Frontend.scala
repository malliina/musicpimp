package org.musicpimp.js

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Frontend {
  var app: Option[BaseScript] = None
  var footer: Option[FooterSocket] = None

  def main(args: Array[String]): Unit = {
    MyJQuery
    MyJQueryUI
    slider
    autocomplete
    Popper
    Bootstrap
    val path = dom.window.location.pathname
    val front: PartialFunction[String, BaseScript] = {
      case "/search"             => new Search(new MusicItems)
      case "/logs"               => new Logs
      case "/player"             => new Playback
      case "/alarms"             => new Alarms
      case "/alarms/editor"      => new AlarmEditor
      case "/cloud"              => new Cloud
      case p if containsMusic(p) => new MusicItems
    }
    app = front lift path
    footer = Option(new FooterSocket)
  }

  def containsMusic(p: String) =
    p.isEmpty || p == "/" || p.startsWith("/folders") ||
      p == "/player/recent" || p == "/player/popular"
}

@js.native
@JSImport("popper.js", JSImport.Namespace)
object Popper extends js.Object

@js.native
@JSImport("bootstrap", JSImport.Namespace)
object Bootstrap extends js.Object
