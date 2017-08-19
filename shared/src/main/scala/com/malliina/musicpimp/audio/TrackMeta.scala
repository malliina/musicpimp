package com.malliina.musicpimp.audio

import com.malliina.json.JsonFormats
import com.malliina.musicpimp.json.PimpStrings._
import com.malliina.musicpimp.models.{BaseTrack, PimpPath, TrackID, TrackIdent}
import com.malliina.play.http.FullUrl
import com.malliina.storage.StorageSize
import play.api.libs.json.Json._
import play.api.libs.json.{Reads, Writes}
import play.api.mvc.Call

import scala.concurrent.duration.Duration

trait TrackMeta extends BaseTrack {
  def id: TrackID

  def title: String

  def artist: String

  def album: String

  def path: PimpPath

  def duration: Duration

  def size: Long = storageSize.toBytes

  def storageSize: StorageSize
}

object TrackMeta {
  implicit val sto = JsonFormats.storageSizeFormat
  implicit val dur = JsonFormats.durationFormat

  val reader: Reads[TrackMeta] = Track.jsonFormat.map[TrackMeta](identity)

  def writer(host: FullUrl, url: TrackIdent => Call): Writes[BaseTrack] =
    Writes[BaseTrack] { t =>
      obj(
        Id -> TrackIdent.json.writes(t.id),
        Title -> t.title,
        Artist -> t.artist,
        Album -> t.album,
        PathKey -> t.path,
        DurationKey -> t.duration,
        Size -> t.size,
        Url -> host.absolute(url(t.id))
      )
    }
}
