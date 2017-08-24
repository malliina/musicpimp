package org.musicpimp.js

import com.malliina.musicpimp.js.FrontStrings._
import org.scalajs.jquery.{JQuery, JQueryEventObject}
import play.api.libs.json.Writes

abstract class SocketJS(wsPath: String, log: Logger)
  extends BaseSocket(wsPath, HiddenClass, log)
    with BaseScript {
  def this(wsPath: String) = this(wsPath, Logger.default)

  def onClick[C: Writes](element: JQuery, cmd: C) =
    install(element, send(cmd))

  def install(element: JQuery, onClick: => Unit) =
    element click { (e: JQueryEventObject) =>
      onClick
      e.preventDefault()
    }
}
