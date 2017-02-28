package com.malliina.musicpimp.audio

import com.malliina.json.JsonFormats
import com.malliina.play.http.FullUrl
import play.api.libs.json.{Format, Writes}
import play.api.mvc.RequestHeader

object TrackJson {
  implicit val sto = JsonFormats.storageSizeFormat
  implicit val dur = JsonFormats.durationFormat

  def writer(request: RequestHeader): Writes[TrackMeta] =
    writer(FullUrl.hostOnly(request))

  def writer(host: FullUrl): Writes[TrackMeta] = TrackMeta.writer(
    host,
    controllers.musicpimp.routes.LibraryController.supplyForPlayback
  )

  def format(request: RequestHeader): Format[TrackMeta] =
    format(FullUrl.hostOnly(request))

  def format(host: FullUrl): Format[TrackMeta] =
    Format(TrackMeta.reader, writer(host))
}
