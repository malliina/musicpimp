package com.malliina.musicpimp.audio

import com.malliina.json.JsonFormats
import com.malliina.musicpimp.models.PimpUrl
import play.api.libs.json.{Format, Writes}
import play.api.mvc.RequestHeader

object TrackJson {
  implicit val sto = JsonFormats.storageSizeFormat
  implicit val dur = JsonFormats.durationFormat

  def writer(request: RequestHeader): Writes[TrackMeta] =
    writer(PimpUrl.hostOnly(request))

  def writer(host: PimpUrl): Writes[TrackMeta] = TrackMeta.writer(
    host,
    controllers.musicpimp.routes.LibraryController.supplyForPlayback
  )

  def format(request: RequestHeader): Format[TrackMeta] =
    format(PimpUrl.hostOnly(request))

  def format(host: PimpUrl): Format[TrackMeta] =
    Format(TrackMeta.reader, writer(host))
}
