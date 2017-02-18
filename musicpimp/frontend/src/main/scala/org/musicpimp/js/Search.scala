package org.musicpimp.js

import org.scalajs.jquery.JQueryEventObject
import upickle.Js

case class StatusEvent(event: String, status: String)

class Search(music: MusicItems) extends SocketJS("/search/ws?f=json") {
  val RefreshButtonId = "refresh-button"
  val IndexInfoId = "index-info"
  val RefreshCommandValue = "refresh"

  elem(RefreshButtonId).click { (_: JQueryEventObject) =>
    send(Command(RefreshCommandValue))
  }

  override def handlePayload(payload: Js.Value) = {
    validate[StatusEvent](payload).fold(
      onJsonFailure,
      event => onStatus(event.status)
    )
  }

  def onStatus(status: String) =
    elem(IndexInfoId).html(s" $status")
}
