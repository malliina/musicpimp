package com.mle.musicpimp.library

import com.mle.musicpimp.audio.TrackMeta
import com.mle.musicpimp.models.PlaylistID
import play.api.libs.json.Json

/**
 * @author mle
 */
case class SavedPlaylist(id: PlaylistID, name: String, tracks: Seq[TrackMeta])

object SavedPlaylist {
  implicit val json = Json.format[SavedPlaylist]
}
