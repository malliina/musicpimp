package org.musicpimp.js

import com.malliina.musicpimp.js.SearchStrings
import com.malliina.musicpimp.models.Refresh
import org.scalajs.jquery.JQueryEventObject
import play.api.libs.json.{JsValue, Json, OFormat}

case class SearchStatus(event: String, status: String)

object SearchStatus {
  implicit val json: OFormat[SearchStatus] = Json.format[SearchStatus]
}

class Search(music: MusicItems)
  extends SocketJS("/search/ws?f=json")
    with SearchStrings {

  elem(RefreshButton).click { (_: JQueryEventObject) =>
    send(Refresh)
  }

  override def handlePayload(payload: JsValue): Unit =
    handleValidated[SearchStatus](payload) { event =>
      onStatus(event.status)
    }

  def onStatus(status: String) =
    elem(IndexInfo).html(s" $status")
}
