package com.malliina.musicpimp.models

import com.malliina.musicpimp.audio.Track
import play.api.libs.json.Json

case class SavedPlaylist(id: PlaylistID, name: String, tracks: Seq[Track])

object SavedPlaylist {
  implicit val json = Json.format[SavedPlaylist]
}
