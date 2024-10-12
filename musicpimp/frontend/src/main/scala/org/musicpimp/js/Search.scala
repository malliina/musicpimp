package org.musicpimp.js

import com.malliina.musicpimp.js.SearchStrings
import com.malliina.musicpimp.models.Refresh
import io.circe.{Codec, Json}

case class SearchStatus(event: String, status: String) derives Codec.AsObject

class Search(music: MusicItems) extends SocketJS("/search/ws?f=json") with SearchStrings:

  elem(RefreshButton).onClick: _ =>
    send(Refresh)

  override def handlePayload(payload: Json): Unit =
    handleValidated[SearchStatus](payload): event =>
      onStatus(event.status)

  def onStatus(status: String): Unit =
    elem(IndexInfo).html(s" $status")
