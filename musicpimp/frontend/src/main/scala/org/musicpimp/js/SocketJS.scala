package org.musicpimp.js

import com.malliina.musicpimp.js.FrontStrings.*
import io.circe.Encoder
import org.scalajs.dom.Element

abstract class SocketJS(wsPath: String, log: BaseLogger)
  extends BaseSocket(wsPath, HiddenClass, log)
  with BaseScript:
  def this(wsPath: String) = this(wsPath, BaseLogger.printer)

  def onClick[C: Encoder](element: Element, cmd: C): Unit =
    install(element, send(cmd))

  private def install(element: Element, onClick: => Unit): Unit =
    element.onClick: e =>
      onClick
      e.preventDefault()
