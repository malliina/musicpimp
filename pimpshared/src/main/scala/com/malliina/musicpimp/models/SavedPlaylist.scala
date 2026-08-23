package com.malliina.musicpimp.models

import com.malliina.musicpimp.audio.{FullTrack, TrackMeta}
import com.malliina.musicpimp.json.CrossFormats
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import scala.concurrent.duration.Duration

case class FullSavedPlaylist(
  id: PlaylistID,
  name: String,
  trackCount: Int,
  duration: Duration,
  tracks: Seq[FullTrack]
)

object FullSavedPlaylist:
  implicit val duration: Codec[Duration] = CrossFormats.duration
  implicit val json: Codec[FullSavedPlaylist] = deriveCodec[FullSavedPlaylist]

case class SavedPlaylist(
  id: PlaylistID,
  name: String,
  trackCount: Int,
  duration: Duration,
  tracks: Seq[TrackMeta]
)
