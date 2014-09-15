package com.mle.musicpimp.library

import java.net.URI

import com.mle.audio.meta.{SongMeta, SongTags, UriSource}
import com.mle.musicpimp.audio.{PimpPlayer, PlayableTrack, StoragePlayer}
import com.mle.musicpimp.db.DataTrack
import com.mle.storage.StorageSize
import com.mle.util.Log
import models.MusicItemInfo

import scala.concurrent.duration._

/**
 *
 * @author mle
 */
class LocalTrack(id: String, val meta: SongMeta)
  extends MusicItemInfo(meta.tags.title, id, dir = false)
  with PlayableTrack {

  val media = meta.media

  override val size: StorageSize = media.size

  override val duration: Duration = media.duration

  override val album: String = meta.tags.album

  override val artist: String = meta.tags.artist

  override def toString = id

  override def buildPlayer(eom: () => Unit): PimpPlayer = new StoragePlayer(this, eom)

//  def path =

//  def toData = DataTrack(id,title,artist,album,)
}

object LocalTrack extends Log {
  val empty = new LocalTrack(
    id = "",
    meta = SongMeta(
      UriSource(new URI("http://www.musicpimp.org/"), 0 seconds, StorageSize.empty),
      tags = new SongTags("", "", "")
    ))
}