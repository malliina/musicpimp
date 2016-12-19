package org.musicpimp.js

import org.scalajs.dom
import org.scalajs.dom.CloseEvent
import org.scalajs.dom.raw.{ErrorEvent, Event, MessageEvent}
import upickle.Invalid

import scala.scalajs.js.JSON

abstract class SocketJS(wsPath: String) extends BaseScript {
  val Hidden = "hide"

  val statusElem = elem("status")
  val okStatus = elem("okstatus")
  val failStatus = elem("failstatus")

  println(s"Connecting to $wsPath")
  val socket: dom.WebSocket = openSocket(wsPath)

  def handlePayload(payload: String)

  def send[T: PimpJSON.Writer](payload: T) =
    socket.send(PimpJSON.write(payload))

  def onConnected(e: Event): Unit = showConnected()

  def onClosed(e: CloseEvent): Unit = showDisconnected()

  def onError(e: ErrorEvent): Unit = showDisconnected()

  def showConnected() = {
    okStatus.removeClass(Hidden)
    failStatus.addClass(Hidden)
  }

  def showDisconnected() = {
    okStatus.addClass(Hidden)
    failStatus.removeClass(Hidden)
  }

  def openSocket(pathAndQuery: String) = {
    val wsUrl = s"$wsBaseUrl$pathAndQuery"
    val socket = new dom.WebSocket(wsUrl)
    socket.onopen = (e: Event) => onConnected(e)
    socket.onmessage = (event: MessageEvent) => onMessage(event)
    socket.onclose = (e: CloseEvent) => onClosed(e)
    socket.onerror = (e: ErrorEvent) => onError(e)
    socket
  }

  def wsBaseUrl = {
    val location = dom.window.location
    val wsProto = if (location.protocol == "http:") "ws" else "wss"
    s"$wsProto://${location.host}"
  }

  def onMessage(msg: MessageEvent): Unit = {
    val event = JSON.parse(msg.data.toString)
    if (event.event.toString == "ping") {
    } else {
      handlePayload(JSON.stringify(event))
    }
  }

  def onInvalidData(invalid: Invalid): PartialFunction[Invalid, Unit] = {
    case Invalid.Data(jsValue, errorMessage) =>
      println(s"JSON failed to parse: '$errorMessage' in value '$jsValue'.")
    case Invalid.Json(errorMessage, in) =>
      println(s"Not JSON, '$errorMessage' in value '$in'.")
  }
}
