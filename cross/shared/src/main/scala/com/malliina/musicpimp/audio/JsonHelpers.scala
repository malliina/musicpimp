package com.malliina.musicpimp.audio

import com.malliina.http.FullUrl
import com.malliina.musicpimp.json.CommonStrings.Id
import com.malliina.musicpimp.json.CrossFormats
import com.malliina.musicpimp.json.TrackKeys.*
import com.malliina.musicpimp.models.TrackID
import io.circe.syntax.EncoderOps
import io.circe.{Codec, Decoder, Encoder, Json}

import scala.concurrent.duration.FiniteDuration

object JsonHelpers:
  implicit val durFormat: Codec[FiniteDuration] = CrossFormats.finiteDuration
  val reader: Decoder[TrackMeta] = Decoder[Track].map[TrackMeta](identity)

  private val dummyHost = FullUrl("https", "example.com", "")
  val dummyTrackFormat = Codec.from(reader, urlWriter(_ => dummyHost))

  def urlWriter(url: TrackID => FullUrl): Encoder[TrackMeta] = (t: TrackMeta) =>
    Json.obj(
      Id -> t.id.id.asJson,
      Title -> t.title.asJson,
      Artist -> t.artist.asJson,
      Album -> t.album.asJson,
      PathKey -> t.path.path.asJson,
      DurationKey -> t.duration.asJson,
      Size -> t.size.bytes.asJson,
      Url -> url(t.id).url.asJson
    )
