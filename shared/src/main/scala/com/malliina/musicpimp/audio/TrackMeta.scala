package com.malliina.musicpimp.audio

import com.malliina.json.JsonFormats
import com.malliina.musicpimp.json.PimpStrings._
import com.malliina.musicpimp.models.{MusicItem, PimpPath, TrackID}
import com.malliina.play.http.FullUrl
import com.malliina.storage.StorageSize
import play.api.libs.json.Json._
import play.api.libs.json.{Reads, Writes}
import play.api.mvc.Call

import scala.concurrent.duration.Duration

trait TrackMeta extends MusicItem {
  def id: TrackID

  def title: String

  def artist: String

  def album: String

  def path: PimpPath

  def duration: Duration

  def size: StorageSize
}

object TrackMeta {
  implicit val sto = JsonFormats.storageSizeFormat
  implicit val dur = JsonFormats.durationFormat

  val reader: Reads[TrackMeta] = Track.jsonFormat.map { base =>
    val meta: TrackMeta = base
    meta
  }

  def writer(host: FullUrl, url: TrackID => Call): Writes[TrackMeta] =
    Writes[TrackMeta] { t =>
      obj(
        Id -> t.id,
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
