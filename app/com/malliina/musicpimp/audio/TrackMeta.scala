package com.malliina.musicpimp.audio

import com.malliina.json.JsonFormats
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.models.{PimpPath, PimpUrl, TrackID}
import com.malliina.storage.StorageSize
import play.api.libs.json.Json._
import play.api.libs.json.{Format, Reads, Writes}
import play.api.mvc.{Call, RequestHeader}

import scala.concurrent.duration.Duration

trait TrackMeta {
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

  def writer(request: RequestHeader): Writes[TrackMeta] =
    writer(PimpUrl.hostOnly(request))

  def writer(host: PimpUrl): Writes[TrackMeta] = Writes[TrackMeta] { t =>
    val call: Call = controllers.routes.LibraryController.supplyForPlayback(t.id)
    obj(
      Id -> t.id,
      Title -> t.title,
      Artist -> t.artist,
      Album -> t.album,
      PathKey -> t.path,
      DurationKey -> t.duration,
      Size -> t.size,
      Url -> host.absolute(call)
    )
  }

  val reader: Reads[TrackMeta] = BaseTrackMeta.jsonFormat.map(base => {
    val meta: TrackMeta = base
    meta
  })

  def format(request: RequestHeader): Format[TrackMeta] =
    format(PimpUrl.hostOnly(request))

  def format(host: PimpUrl): Format[TrackMeta] =
    Format(reader, writer(host))
}
