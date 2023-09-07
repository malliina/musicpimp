package com.malliina.musicpimp.audio

import com.malliina.http.FullUrl
import com.malliina.json.PrimitiveFormats
import com.malliina.musicpimp.models._
import com.malliina.play.http.FullUrls
import play.api.libs.json.{Format, Writes}
import play.api.mvc.Call

import scala.concurrent.duration.Duration

object TrackMetas {
  implicit val dur: Format[Duration] = PrimitiveFormats.durationFormat

  def writer(host: FullUrl, url: TrackID => Call): Writes[TrackMeta] =
    urlWriter(id => FullUrls.absolute(host, url(id)))

  def urlWriter(url: TrackID => FullUrl): Writes[TrackMeta] =
    JsonHelpers.urlWriter(url)
}
