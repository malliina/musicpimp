package com.mle.musicpimp.library

import com.mle.musicpimp.models.PlaylistID
import play.api.libs.json.Json

/**
 * @author mle
 */
case class PlaylistSubmission(id: Option[PlaylistID],
                              name: String,
                              tracks: Seq[String])

object PlaylistSubmission {
  implicit val json = Json.format[PlaylistSubmission]
}
