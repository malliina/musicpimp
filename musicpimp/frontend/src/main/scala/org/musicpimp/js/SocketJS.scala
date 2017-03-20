package org.musicpimp.js

import com.malliina.musicpimp.js.FrontStrings.{FailStatus, HiddenClass, OkStatus}
import org.scalajs.dom
import org.scalajs.dom.CloseEvent
import org.scalajs.dom.raw.{ErrorEvent, Event, MessageEvent}
import org.scalajs.jquery.{JQuery, JQueryEventObject}
import upickle.{Invalid, Js}

import scala.util.Either.RightProjection

abstract class SocketJS(wsPath: String, val log: Logger) extends BaseScript {
  def this(wsPath: String) = this(wsPath, Logger.default)

  val EventField = "event"
  val Ping = "ping"

  val okStatus = elem(OkStatus)
  val failStatus = elem(FailStatus)

  val socket: dom.WebSocket = openSocket(wsPath)

  def onClick(element: JQuery, cmd: Command) =
    install(element, send(cmd))

  def install(element: JQuery, onClick: => Unit) =
    element click { (e: JQueryEventObject) =>
      onClick
      e.preventDefault()
    }

  def handlePayload(payload: Js.Value)

  def send[T: PimpJSON.Writer](payload: T) =
    socket.send(PimpJSON.write(payload))

  def onConnected(e: Event): Unit = showConnected()

  def onClosed(e: CloseEvent): Unit = showDisconnected()

  def onError(e: ErrorEvent): Unit = showDisconnected()

  def showConnected() = {
    okStatus removeClass HiddenClass
    failStatus addClass HiddenClass
  }

  def showDisconnected() = {
    okStatus addClass HiddenClass
    failStatus removeClass HiddenClass
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
    PimpJSON.parse(asString).fold(onJsonFailure, json => {
      if (readField[String](json, EventField).exists(_ == Ping)) {
      } else {
        handlePayload(json)
      }
    })
  }

  def readField[T: PimpJSON.Reader](json: Js.Value, field: String): RightProjection[Invalid, T] = {
    val either = for {
      map <- PimpJSON.toEither(json.obj).right
      fieldJson <- map.get(field).toRight(Invalid.Data(json, s"Missing field: '$field'.")).right
      parsed <- validate[T](fieldJson).right
    } yield parsed
    either.right
  }

  def onJsonFailure = onInvalidData.lift

  private def onInvalidData: PartialFunction[Invalid, Unit] = {
    case Invalid.Data(jsValue, errorMessage) =>
      log.info(s"JSON failed to parse: '$errorMessage' in value '$jsValue'.")
    case Invalid.Json(errorMessage, in) =>
      log.info(s"Not JSON, '$errorMessage' in value '$in'.")
  }
}
