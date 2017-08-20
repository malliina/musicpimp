package com.malliina.musicpimp.audio

import com.malliina.http.FullUrl
import com.malliina.json.JsonFormats
import com.malliina.musicpimp.models.TrackID
import com.malliina.play.http.FullUrls
import play.api.libs.json.{Format, Writes}
import play.api.mvc.RequestHeader

object TrackJson {
  implicit val sto = JsonFormats.storageSizeFormat
  implicit val dur = JsonFormats.durationFormat

  def writer(request: RequestHeader): Writes[TrackMeta] =
    writer(FullUrls.hostOnly(request))

  def writer(host: FullUrl): Writes[TrackMeta] = TrackMetas.writer(
    host,
    id => controllers.musicpimp.routes.LibraryController.supplyForPlayback(TrackID(id.id))
  )

  def format(request: RequestHeader): Format[TrackMeta] =
    format(FullUrls.hostOnly(request))

  def format(host: FullUrl): Format[TrackMeta] =
    Format(TrackMetas.reader, writer(host))
}
