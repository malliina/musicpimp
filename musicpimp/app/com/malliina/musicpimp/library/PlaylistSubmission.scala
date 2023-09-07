package com.malliina.musicpimp.library

import com.malliina.musicpimp.models.{PlaylistID, TrackID}
import play.api.libs.json.{Format, Json, OFormat}

case class PlaylistSubmission(id: Option[PlaylistID], name: String, tracks: Seq[TrackID]) {
  val isUpdate = id.nonEmpty
}

object PlaylistSubmission {
  implicit val track: Format[TrackID] = TrackID.json
  implicit val json: OFormat[PlaylistSubmission] = Json.format[PlaylistSubmission]
}
