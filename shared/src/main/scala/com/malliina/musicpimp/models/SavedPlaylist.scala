package com.malliina.musicpimp.models

import com.malliina.json.PrimitiveFormats
import com.malliina.musicpimp.audio.{FullTrack, TrackMeta}
import com.malliina.play.Writeables
import play.api.http.Writeable
import play.api.libs.json.Json

import scala.concurrent.duration.Duration

case class FullSavedPlaylist(id: PlaylistID, name: String, trackCount: Int, duration: Duration, tracks: Seq[FullTrack])

object FullSavedPlaylist {
  implicit val duration = PrimitiveFormats.durationFormat
  implicit val json = Json.format[FullSavedPlaylist]
  implicit val html: Writeable[FullSavedPlaylist] = Writeables.fromJson[FullSavedPlaylist]
  implicit val htmlSeq: Writeable[Seq[FullSavedPlaylist]] = Writeables.fromJson[Seq[FullSavedPlaylist]]
}

case class SavedPlaylist(id: PlaylistID, name: String, trackCount: Int, duration: Duration, tracks: Seq[TrackMeta])
