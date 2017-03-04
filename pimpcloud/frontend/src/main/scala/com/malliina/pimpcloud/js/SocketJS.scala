package com.malliina.pimpcloud.js

import org.scalajs.dom
import org.scalajs.dom.CloseEvent
import org.scalajs.dom.raw.{ErrorEvent, Event, MessageEvent}
import org.scalajs.jquery.jQuery
import upickle.Invalid

import scala.scalajs.js
import scala.scalajs.js.JSON

abstract class SocketJS(wsPath: String) {
  val okStatus = elem("okstatus")
  val failStatus = elem("failstatus")
  val Hidden = "hidden"

  val socket: dom.WebSocket = openSocket(wsPath)

  def handlePayload(payload: String)

  def showConnected() = {
    okStatus removeClass Hidden
    failStatus addClass Hidden
  }

  def showDisconnected() = {
    okStatus addClass Hidden
    failStatus removeClass Hidden
  }

  def openSocket(pathAndQuery: String) = {
    val wsUrl = s"$wsBaseUrl$pathAndQuery"
    val socket = new dom.WebSocket(wsUrl)
//    socket.onopen = (_: Event) => {
//      socket.send(PimpJSON.write(Command.Subscribe))
//      setFeedback("Connected.")
//    }
    socket.onopen = (e: Event) => onConnected(e)
    socket.onmessage = (event: MessageEvent) => onMessage(event)
    socket.onclose = (e: CloseEvent) => onClosed(e)
    socket.onerror = (e: ErrorEvent) => onError(e)
    socket
  }

  def onConnected(e: Event): Unit = showConnected()

  def onClosed(e: CloseEvent): Unit = showDisconnected()

  def onError(e: ErrorEvent): Unit = showDisconnected()

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

  def onJsonFailure = onInvalidData.lift

  private def onInvalidData: PartialFunction[Invalid, Unit] = {
    case Invalid.Data(jsValue, errorMessage) =>
      println(s"JSON failed to parse: '$errorMessage' in value '$jsValue'")
    case Invalid.Json(errorMessage, in) =>
      println(s"Not JSON, '$errorMessage' in value '$in'")
  }

  def elem(id: String) = jQuery(s"#$id")

  def global = js.Dynamic.global

  def validate[T: PimpJSON.Reader](in: String): Either[Invalid, T] =
    PimpJSON.validate(in)
}
