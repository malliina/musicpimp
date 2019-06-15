package org.musicpimp.js

import com.malliina.musicpimp.js.FrontStrings._
import org.scalajs.dom
import org.scalajs.dom.CloseEvent
import org.scalajs.dom.raw.{Event, MessageEvent}
import play.api.libs.json._

import scala.util.Try

class BaseSocket(wsPath: String, val hideClass: String, val log: BaseLogger) extends ScriptHelpers {
  val okStatus = elem(OkStatus)
  val failStatus = elem(FailStatus)

  val socket: dom.WebSocket = openSocket(wsPath)

  def handlePayload(payload: JsValue): Unit = ()

  def handleValidated[T: Reads](json: JsValue)(process: T => Unit): Unit =
    json.validate[T].fold(err => onJsonFailure(JsError(err)), process)

  def showConnected(): Unit = {
    okStatus removeClass hideClass
    failStatus addClass hideClass
  }

  def showDisconnected(): Unit = {
    okStatus addClass hideClass
    failStatus removeClass hideClass
  }

  def send[T: Writes](payload: T): Unit = {
    val asString = Json.stringify(Json.toJson(payload))
    socket.send(asString)
  }

  def onMessage(msg: MessageEvent): Unit = {
    Try(Json.parse(msg.data.toString)).map { json =>
      val isPing = (json \ EventKey).validate[String].filter(_ == Ping).isSuccess
      if (!isPing) {
        handlePayload(json)
      }
    }.recover { case e => onJsonException(e) }
  }

  // alternative with native JSON parsing
  //  def onMessage(msg: MessageEvent): Unit = {
  //    val event = JSON.parse(msg.data.toString)
  //    if (event.event.toString == "ping") {
  //    } else {
  //      handlePayload(JSON.stringify(event))
  //    }
  //  }

  def onConnected(e: Event): Unit = showConnected()

  def onClosed(e: CloseEvent): Unit = showDisconnected()

  def onError(e: Event): Unit = showDisconnected()

  def openSocket(pathAndQuery: String) = {
    val wsUrl = s"$wsBaseUrl$pathAndQuery"
    val socket = new dom.WebSocket(wsUrl)
    socket.onopen = (e: Event) => onConnected(e)
    socket.onmessage = (e: MessageEvent) => onMessage(e)
    socket.onclose = (e: CloseEvent) => onClosed(e)
    socket.onerror = (e: Event) => onError(e)
    socket
  }

  def wsBaseUrl = {
    val location = dom.window.location
    val wsProto = if (location.protocol == "http:") "ws" else "wss"
    s"$wsProto://${location.host}"
  }

  def onJsonException(t: Throwable): Unit = {
    log error t
  }

  protected def onJsonFailure(result: JsError): Unit = {
    println(result)
    log info s"JSON error $result"
  }
}
