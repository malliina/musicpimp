package com.malliina.musicpimp.library

import com.malliina.musicpimp.models.{PlaylistID, TrackID}
import play.api.libs.json.Json

case class PlaylistSubmission(id: Option[PlaylistID],
                              name: String,
                              tracks: Seq[TrackID]) {
  val isUpdate = id.nonEmpty
}

object PlaylistSubmission {
  implicit val track = TrackID.format
  implicit val json = Json.format[PlaylistSubmission]
}
