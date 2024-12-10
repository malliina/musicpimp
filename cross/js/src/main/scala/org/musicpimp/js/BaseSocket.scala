package org.musicpimp.js

import com.malliina.musicpimp.js.FrontStrings.*
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}
import org.scalajs.dom
import org.scalajs.dom.{CloseEvent, Event, MessageEvent}

class BaseSocket(wsPath: String, val hideClass: String, val log: BaseLogger) extends ScriptHelpers:
  private val okStatus = elem(OkStatus)
  private val failStatus = elem(FailStatus)

  private val socket: dom.WebSocket = openSocket(wsPath)

  def handlePayload(payload: Json): Unit = ()

  def handleValidated[T: Decoder](json: Json)(process: T => Unit): Unit =
    json.as[T].fold(err => onJsonFailure(err.message), process)

  def showConnected(): Unit =
    okStatus.removeClass(hideClass)
    failStatus.addClass(hideClass)

  def showDisconnected(): Unit =
    okStatus.addClass(hideClass)
    failStatus.removeClass(hideClass)

  def send[T: Encoder](payload: T): Unit =
    socket.send(payload.asJson.noSpaces)

  private def onMessage(msg: MessageEvent): Unit =
    try
      io.circe.parser
        .parse(msg.data.toString)
        .map: json =>
          val isPing = json.hcursor
            .downField(EventKey)
            .as[String]
            .contains(Ping)
          if !isPing then handlePayload(json)
    catch
      case e: Exception =>
        onJsonException(e)

  // alternative with native JSON parsing
  //  def onMessage(msg: MessageEvent): Unit = {
  //    val event = JSON.parse(msg.data.toString)
  //    if (event.event.toString == "ping") {
  //    } else {
  //      handlePayload(JSON.stringify(event))
  //    }
  //  }

  def onConnected(e: Event): Unit = showConnected()

  private def onClosed(e: CloseEvent): Unit = showDisconnected()

  private def onError(e: Event): Unit = showDisconnected()

  private def openSocket(pathAndQuery: String) =
    val wsUrl = s"$wsBaseUrl$pathAndQuery"
    val socket = new dom.WebSocket(wsUrl)
    socket.onopen = (e: Event) => onConnected(e)
    socket.onmessage = (e: MessageEvent) => onMessage(e)
    socket.onclose = (e: CloseEvent) => onClosed(e)
    socket.onerror = (e: Event) => onError(e)
    socket

  private def wsBaseUrl =
    val location = dom.window.location
    val wsProto = if location.protocol == "http:" then "ws" else "wss"
    s"$wsProto://${location.host}"

  private def onJsonException(t: Throwable): Unit =
    log.error(t)

  protected def onJsonFailure(result: String): Unit =
    println(result)
    log.info(s"JSON error $result")
