package org.musicpimp.js

import org.scalajs.jquery.JQueryEventObject
import upickle.Js

case class StatusEvent(event: String, status: String)

class Search(music: MusicItems) extends SocketJS("/search/ws?f=json") {
  elem("refresh-button").click { (_: JQueryEventObject) =>
    send(Command("refresh"))
  }

  override def handlePayload(payload: Js.Value) = {
    validate[StatusEvent](payload).fold(
      onInvalidData,
      event => onStatus(event.status)
    )
  }

  def onStatus(status: String) =
    elem("index-info").html(s" $status")
}
