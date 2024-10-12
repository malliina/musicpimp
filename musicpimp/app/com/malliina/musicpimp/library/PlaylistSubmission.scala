package com.malliina.musicpimp.library

import com.malliina.musicpimp.models.{PlaylistID, TrackID}
import io.circe.Codec

case class PlaylistSubmission(id: Option[PlaylistID], name: String, tracks: Seq[TrackID])
  derives Codec.AsObject:
  val isUpdate = id.nonEmpty
