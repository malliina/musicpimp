package org.musicpimp.js

import org.scalajs.dom
import org.scalajs.dom.CloseEvent
import org.scalajs.dom.raw.{ErrorEvent, Event, MessageEvent}
import upickle.{Invalid, Js}

abstract class SocketJS(wsPath: String) extends BaseScript {
  val EventField = "event"
  val Ping = "ping"
  val Hidden = "hide"

  val statusElem = elem("status")
  val okStatus = elem("okstatus")
  val failStatus = elem("failstatus")

  val socket: dom.WebSocket = openSocket(wsPath)

  def handlePayload(payload: Js.Value)

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
    socket.onmessage = (e: MessageEvent) => onMessage(e)
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
    val asString = msg.data.toString
    PimpJSON.parse(asString).fold(onInvalidData, json => {
      if (readField[String](json, EventField).right.exists(_ == Ping)) {
      } else {
        handlePayload(json)
      }
    })
  }

  def readField[T: PimpJSON.Reader](json: Js.Value, field: String): Either[Invalid, T] =
    for {
      jsObject <- validate[Js.Obj](json).right
      fieldJson <- jsObject.value.toMap.get(field).toRight(Invalid.Data(json, s"Missing field: '$field'.")).right
      parsed <- validate[T](fieldJson).right
    } yield parsed

  def onInvalidData(invalid: Invalid): PartialFunction[Invalid, Unit] = {
    case Invalid.Data(jsValue, errorMessage) =>
      println(s"JSON failed to parse: '$errorMessage' in value '$jsValue'.")
    case Invalid.Json(errorMessage, in) =>
      println(s"Not JSON, '$errorMessage' in value '$in'.")
  }
}
