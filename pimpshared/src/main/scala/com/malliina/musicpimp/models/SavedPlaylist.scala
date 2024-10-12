package com.malliina.musicpimp.models

import com.malliina.musicpimp.audio.{FullTrack, TrackMeta}
import com.malliina.musicpimp.json.CrossFormats
import com.malliina.play.Writeables
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import play.api.http.Writeable

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
  implicit val html: Writeable[FullSavedPlaylist] = Writeables.fromCirceJson[FullSavedPlaylist]
  implicit val htmlSeq: Writeable[Seq[FullSavedPlaylist]] =
    Writeables.fromCirceJson[Seq[FullSavedPlaylist]]

case class SavedPlaylist(
  id: PlaylistID,
  name: String,
  trackCount: Int,
  duration: Duration,
  tracks: Seq[TrackMeta]
)
