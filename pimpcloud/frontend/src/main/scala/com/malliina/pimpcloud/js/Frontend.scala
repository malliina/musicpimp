package com.malliina.pimpcloud.js

import org.musicpimp.js.ScriptHelpers
import org.scalajs.dom
import org.scalajs.jquery.JQueryStatic

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Frontend extends ScriptHelpers {
  var app: Option[SocketJS] = None
  private val jq = MyJQuery
  private val jqui = MyJQueryUI
  private val p = Popper
  private val b = Bootstrap

  def main(args: Array[String]): Unit = {
    val path = dom.window.location.pathname
    val jsImpl: PartialFunction[String, SocketJS] = {
      case "/admin/logs"                                          => new LogsJS
      case "/admin"                                               => new AdminJS
      case p if p.isEmpty || p == "/" || p.startsWith("/folders") => new PlayerJS
    }

    app = jsImpl.lift(path)
  }
}

@js.native
@JSImport("jquery", JSImport.Namespace)
object MyJQuery extends JQueryStatic

@js.native
@JSImport("jquery-ui", JSImport.Namespace)
object MyJQueryUI extends js.Object

@js.native
@JSImport("popper.js", JSImport.Namespace)
object Popper extends js.Object

@js.native
@JSImport("bootstrap", JSImport.Namespace)
object Bootstrap extends js.Object
