package com.mle.musicpimp.library

import com.mle.audio.meta.{MediaInfo, SongTags, SongMeta}
import models.MusicItemInfo
import com.mle.util.Log
import java.net.URI
import scala.concurrent.duration._
import com.mle.storage.StorageSize
import com.mle.musicpimp.audio.TrackMeta
import java.io.InputStream

/**
 *
 * @author mle
 */
class TrackInfo(id: String, val meta: SongMeta)
  extends MusicItemInfo(meta.tags.title, id, dir = false)
  with TrackMeta {

  val media = meta.media

  override def stream: InputStream = media.uri.toURL.openStream()

  override val size: StorageSize = media.size

  override val duration: Duration = media.duration

  override val album: String = meta.tags.album

  override val artist: String = meta.tags.artist

  override def toString = id
}

object TrackInfo extends Log {
  val empty = new TrackInfo(
    id = "",
    meta = SongMeta(
      MediaInfo(new URI("http://www.musicpimp.org/"), 0 seconds, StorageSize.empty),
      tags = new SongTags("", "", "")
    ))
}