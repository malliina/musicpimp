package com.malliina.musicpimp.library

import com.malliina.musicpimp.models.{PlaylistID, TrackID}
import play.api.libs.json.Json

case class PlaylistSubmission(playlistId: Option[PlaylistID],
                              name: String,
                              tracks: Seq[TrackID]) {
  val isUpdate = playlistId.nonEmpty
}

object PlaylistSubmission {
  implicit val json = Json.format[PlaylistSubmission]
}
