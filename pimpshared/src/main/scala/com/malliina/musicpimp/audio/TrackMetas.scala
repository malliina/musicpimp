package com.malliina.musicpimp.audio

import com.malliina.http.FullUrl
import com.malliina.musicpimp.json.CrossFormats
import com.malliina.musicpimp.models.*
import com.malliina.play.http.FullUrls
import io.circe.{Codec, Encoder}
import play.api.mvc.Call

import scala.concurrent.duration.Duration

object TrackMetas:
  implicit val dur: Codec[Duration] = CrossFormats.duration

  def writer(host: FullUrl, url: TrackID => Call): Encoder[TrackMeta] =
    urlWriter(id => FullUrls.absolute(host, url(id)))

  def urlWriter(url: TrackID => FullUrl): Encoder[TrackMeta] =
    JsonHelpers.urlWriter(url)
