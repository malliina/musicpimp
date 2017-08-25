package org.musicpimp.js

import com.malliina.musicpimp.js.SearchStrings
import org.scalajs.jquery.JQueryEventObject
import play.api.libs.json.{JsValue, Json}

case class SearchStatus(event: String, status: String)

object SearchStatus {
  implicit val json = Json.format[SearchStatus]
}

class Search(music: MusicItems)
  extends SocketJS("/search/ws?f=json")
    with SearchStrings {

  elem(RefreshButton).click { (_: JQueryEventObject) =>
    send(Command(Refresh))
  }

  override def handlePayload(payload: JsValue): Unit =
    handleValidated[SearchStatus](payload) { event =>
      onStatus(event.status)
    }

  def onStatus(status: String) =
    elem(IndexInfo).html(s" $status")
}
