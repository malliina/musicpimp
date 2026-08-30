package org.musicpimp.js

import com.malliina.musicpimp.js.Classes
import org.scalajs.dom

import scala.annotation.unused
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportTopLevel, JSImport}

object Frontend:
  private var app: Option[BaseScript] = None
  var footer: Option[FooterSocket] = None

  def main(args: Array[String]): Unit =
    val _ = jquery
//    val _ = slider
//    val _ = autocomplete
    val _ = Popper
    val _ = Bootstrap
    val _ = AppCss
    val path = dom.window.location.pathname
    val front: PartialFunction[String, BaseScript] =
      case "/search"             => new Search(new MusicItems)
      case "/logs"               => new Logs
      case "/player"             => new Playback
      case "/alarms"             => new Alarms
      case "/cloud"              => new Cloud
      case p if containsMusic(p) => new MusicItems
    app = front.lift(path)
    if path.startsWith("/alarms/editor") then app = Option(new AlarmEditor)
    if !path.startsWith("/login") then footer = Option(new FooterSocket)
    if has(Classes.home) then ()

  private def containsMusic(p: String) =
    p.isEmpty || p == "/" || p.startsWith("/folders") ||
      p == "/player/recent" || p == "/player/popular"

  private def has(feature: String) = dom.document.body.classList.contains(feature)

@js.native
@JSImport("@popperjs/core", JSImport.Namespace)
object Popper extends js.Object

@js.native
@JSImport("bootstrap", JSImport.Namespace)
object Bootstrap extends js.Object

@js.native
@JSImport("jquery", JSImport.Default, globalFallback = "$")
object jquery extends JQuery

//@JSImport("jquery-ui/ui/widgets/slider", JSImport.Default)
//@js.native
//object slider extends js.Object
//
//@JSImport("jquery-ui/ui/widgets/autocomplete", JSImport.Default)
//@js.native
//object autocomplete extends js.Object

@js.native
@JSImport("./css/app", JSImport.Namespace)
object AppCss extends js.Object
