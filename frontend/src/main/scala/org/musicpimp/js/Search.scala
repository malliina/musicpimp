package org.musicpimp.js

import org.scalajs.jquery.JQueryEventObject

case class StatusEvent(event: String, status: String)

class Search extends SocketJS("/search/ws?f=json") {

  setup()

  def setup() = {
    elem("refresh-button").click { (_: JQueryEventObject) =>
      send(Command("refresh"))
    }
  }

  override def handlePayload(payload: String) = {
    validate[StatusEvent](payload).fold(
      onInvalidData,
      event => onStatus(event.status)
    )
  }

  def onStatus(status: String) =
    elem("index-info").html(status)
}
