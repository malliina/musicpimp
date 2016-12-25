package org.musicpimp.js

import org.scalajs.dom.raw.Event
import upickle.Js.Value

case class SocketEvent(event: String, id: Option[String]) {
  def isConnected = id.isDefined
}

class Cloud extends SocketJS("/ws/cloud?f=json") {
  override def onConnected(e: Event) = {
    send(Command.subscribe)
    super.onConnected(e)
  }

  override def handlePayload(payload: Value) = {
    validate[SocketEvent](payload).fold[Any](onInvalidData, onSocketEvent)
  }

  def onSocketEvent(event: SocketEvent) = {
    event.event match {
      case "cloud" =>
        val msg = event.id.map(i => s"Connected as $i").getOrElse("Disconnected")
      case other =>
        println(s"Unknown event: '$other'")
    }
  }
}
