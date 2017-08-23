package com.malliina.musicpimp.audio

import com.malliina.http.FullUrl
import com.malliina.musicpimp.json.CommonStrings.Id
import com.malliina.musicpimp.json.CrossFormats
import com.malliina.musicpimp.json.TrackKeys._
import com.malliina.musicpimp.models.TrackID
import play.api.libs.json.Json.obj
import play.api.libs.json.{Format, Reads, Writes}

object JsonHelpers {
  implicit val durFormat = CrossFormats.duration
  val reader: Reads[TrackMeta] = Track.jsonFormat.map[TrackMeta](identity)

  private val dummyHost = FullUrl("https", "example.com", "")
  val dummyTrackFormat = Format(reader, urlWriter(_ => dummyHost))

  def urlWriter(url: TrackID => FullUrl): Writes[TrackMeta] =
    Writes[TrackMeta] { t =>
      obj(
        Id -> t.id,
        Title -> t.title,
        Artist -> t.artist,
        Album -> t.album,
        PathKey -> t.path,
        DurationKey -> t.duration,
        Size -> t.size,
        Url -> url(t.id)
      )
    }
}
