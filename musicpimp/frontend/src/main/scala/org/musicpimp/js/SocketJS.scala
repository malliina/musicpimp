package org.musicpimp.js

import com.malliina.musicpimp.js.FrontStrings._
import org.scalajs.jquery.{JQuery, JQueryEventObject}

abstract class SocketJS(wsPath: String, log: Logger)
  extends BaseSocket(wsPath, HiddenClass, log)
    with BaseScript {
  def this(wsPath: String) = this(wsPath, Logger.default)

  def onClick(element: JQuery, cmd: Command) =
    install(element, send(cmd))

  def install(element: JQuery, onClick: => Unit) =
    element click { (e: JQueryEventObject) =>
      onClick
      e.preventDefault()
    }
}
