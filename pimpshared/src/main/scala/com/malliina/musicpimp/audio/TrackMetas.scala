package com.malliina.musicpimp.audio

import com.malliina.http.FullUrl
import com.malliina.musicpimp.json.CrossFormats
import com.malliina.musicpimp.models.*
import com.malliina.play.http.FullUrls2
import io.circe.{Codec, Encoder}
import org.http4s.Uri

import scala.concurrent.duration.Duration

object TrackMetas:
  implicit val dur: Codec[Duration] = CrossFormats.duration

  def writer(host: FullUrl, url: TrackID => Uri): Encoder[TrackMeta] =
    JsonHelpers.urlWriter(id => FullUrls2.absolute(host, url(id)))
