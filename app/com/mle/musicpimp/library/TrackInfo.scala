package com.mle.musicpimp.library

import com.mle.audio.meta.{MediaInfo, SongTags, SongMeta}
import models.MusicItemInfo
import play.api.libs.json.Json._
import com.mle.musicpimp.json.JsonStrings._
import play.api.libs.json.{JsValue, Writes}
import com.mle.util.Log
import java.net.URI
import scala.concurrent.duration._
import com.mle.storage.StorageSize

/**
 *
 * @author mle
 */
class TrackInfo(id: String, val meta: SongMeta)
  extends MusicItemInfo(meta.tags.title, id, dir = false)

object TrackInfo extends Log {
  // compatible with api ver 17 also; only added DURATION
  implicit val jsonWriter18 = new Writes[TrackInfo] {
    def writes(o: TrackInfo): JsValue = obj(
      ID -> o.id,
      TITLE -> o.meta.tags.title,
      ARTIST -> o.meta.tags.artist,
      ALBUM -> o.meta.tags.album,
      DURATION -> o.meta.media.duration.toSeconds,
      DURATION_SECONDS -> o.meta.media.duration.toSeconds,
      SIZE -> o.meta.media.size.toBytes
    )
  }

  val empty = new TrackInfo(
    id = "",
    meta = SongMeta(
      MediaInfo(new URI("http://www.musicpimp.org/"), 0 seconds, StorageSize.empty),
      tags = new SongTags("", "", "")
    ))
}