package com.mle.musicpimp.library

import com.mle.musicpimp.models.PlaylistID
import play.api.libs.json.Json

/**
  * @author mle
  */
case class PlaylistSubmission(playlistId: Option[PlaylistID],
                              name: String,
                              tracks: Seq[String]) {
  val isUpdate = playlistId.nonEmpty
}

object PlaylistSubmission {
  implicit val json = Json.format[PlaylistSubmission]
}
