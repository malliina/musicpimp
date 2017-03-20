package org.musicpimp.js

import com.malliina.musicpimp.js.SearchStrings
import org.scalajs.jquery.JQueryEventObject
import upickle.Js

case class StatusEvent(event: String, status: String)

class Search(music: MusicItems)
  extends SocketJS("/search/ws?f=json")
    with SearchStrings {

  val RefreshCommandValue = "refresh"

  elem(RefreshButton).click { (_: JQueryEventObject) =>
    send(Command(RefreshCommandValue))
  }

  override def handlePayload(payload: Js.Value) = {
    validate[StatusEvent](payload).fold(
      onJsonFailure,
      event => onStatus(event.status)
    )
  }

  def onStatus(status: String) =
    elem(IndexInfo).html(s" $status")
}
