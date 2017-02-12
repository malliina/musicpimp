package com.malliina.pimpcloud.js

import org.scalajs.dom
import org.scalajs.dom.CloseEvent
import org.scalajs.dom.raw.{ErrorEvent, Event, MessageEvent}
import org.scalajs.jquery.jQuery
import upickle.Invalid

import scala.scalajs.js
import scala.scalajs.js.JSON

abstract class SocketJS(wsPath: String) {
  val statusElem = elem("status")

  val socket: dom.WebSocket = openSocket(wsPath)

  def handlePayload(payload: String)

  def openSocket(pathAndQuery: String) = {
    val wsUrl = s"$wsBaseUrl$pathAndQuery"
    val socket = new dom.WebSocket(wsUrl)
    socket.onopen = (_: Event) => {
      socket.send(PimpJSON.write(Command.Subscribe))
      setFeedback("Connected.")
    }
    socket.onmessage = (event: MessageEvent) => onMessage(event)
    socket.onclose = (_: CloseEvent) => setFeedback("Connection closed.")
    socket.onerror = (_: ErrorEvent) => setFeedback("Connection error.")
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
      println(s"JSON failed to parse: '$errorMessage' in value '$jsValue'")
    case Invalid.Json(errorMessage, in) =>
      println(s"Not JSON, '$errorMessage' in value '$in'")
  }

  def setFeedback(feedback: String) = statusElem html feedback

  def elem(id: String) = jQuery(s"#$id")

  def global = js.Dynamic.global

  def validate[T: PimpJSON.Reader](in: String): Either[Invalid, T] =
    PimpJSON.validate(in)
}
