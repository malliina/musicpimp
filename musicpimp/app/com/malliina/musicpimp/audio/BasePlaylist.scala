package com.malliina.musicpimp.audio

import com.malliina.audio.IPlaylist
import com.malliina.musicpimp.audio.BasePlaylist.log
import com.malliina.util.Lists
import play.api.Logger

import scala.concurrent.stm._

object BasePlaylist {
  private val log = Logger(getClass)
  val NoPosition = -1
}

/**
  *
  * @tparam T type of playlist item
  */
trait BasePlaylist[T] extends IPlaylist[T] {
  type PlaylistIndex = Int

  val NO_POSITION = BasePlaylist.NoPosition

  protected def pos: Ref[PlaylistIndex]

  protected def songs: Ref[Seq[T]]

  def songList = songs.single.get

  /**
    * The playlist index. An empty playlist implies index == NO_POSITION,
    * but index == NO_POSITION does not imply an empty playlist.
    *
    * @return the playlist index, or NO_POSITION if no playlist item is selected or the playlist is empty
    */
  def index = pos.single.get

  def index_=(newPlaylistPosition: PlaylistIndex) = atomic { implicit txn =>
    require(newPlaylistPosition >= 0, s"Negative playlist position: $newPlaylistPosition")
    songs()
    val songCount = songs.get.size
    require(newPlaylistPosition < songCount, s"No song at index: $newPlaylistPosition, playlist only contains $songCount tracks")
    val changed = index != newPlaylistPosition

    pos.set(newPlaylistPosition)
    if (changed) {
      onPlaylistIndexChanged(pos.get)
    }
  }

  def current: Option[T] = atomic { implicit txn =>
    val idx = pos.get
    val tracks = songs.get
    if (idx >= 0 && tracks.size > idx) {
      Some(tracks(idx))
    } else {
      None
    }
  }

  def next: Option[T] = atomic { implicit txn =>
    val tracks = songs.get
    if (tracks.size > pos.get + 1) {
      pos.transform(_ + 1)
      val newIndex = pos.get
      onPlaylistIndexChanged(newIndex)
      Some(tracks(newIndex))
    } else {
      None
    }
  }

  def prev: Option[T] = atomic { implicit txn =>
    val idx = pos.get
    val tracks = songs.get
    if (idx > 0 && tracks.size > idx - 1) {
      pos.transform(_ - 1)
      val newIndex = pos.get
      onPlaylistIndexChanged(newIndex)
      Some(tracks(newIndex))
    } else {
      None
    }
  }

  def add(song: T): Unit = atomic { implicit txn =>
    songs.transform(_ :+ song)
    onPlaylistModified(songs.get)
  }

  def insert(position: PlaylistIndex, song: T) = atomic { implicit txn =>
    songs.transform(list => Lists.insertAt(position, list, song))
    onPlaylistModified(songs.get)
    val idx = pos.get
    if (position <= idx && songs.get.size > idx + 1) {
      pos.transform(_ + 1)
      onPlaylistIndexChanged(pos.get)
    }
  }

  def move(sourcePosition: PlaylistIndex, destPosition: PlaylistIndex) = atomic { implicit txn =>
    val songCount = songList.size
    val isActionable =
      sourcePosition != destPosition &&
        sourcePosition < songCount &&
        destPosition < songCount &&
        sourcePosition >= 0 &&
        destPosition >= 0
    if (isActionable) {
      val newIndex = indexAfterMove(index, sourcePosition, destPosition)
      songs.transform(ts => Lists.move(sourcePosition, destPosition, ts))
      onPlaylistModified(songs.get)
      index = newIndex
    }
  }

  def indexAfterMove(current: Int, src: Int, dest: Int) = {
    if (src == current) {
      // current one being moved
      dest
    } else if (src < current && dest >= current) {
      // removed from below
      current - 1
    } else if (src > current && dest <= current) {
      // added to below
      current + 1
    } else {
      current
    }
  }

  def reset(position: PlaylistIndex, tracks: Seq[T]) = atomic { implicit txn =>
    clearButDontTell()
    val previousSongs = songs.getAndTransform(_ => tracks)
    val previousIndex = pos.getAndTransform(_ => position)
    if (previousSongs != tracks) {
      onPlaylistModified(songs.get)
    }
    if (previousIndex != pos.get) {
      onPlaylistIndexChanged(pos.get)
    }
  }

  def delete(position: PlaylistIndex) = atomic { implicit txn =>
    songs.transform(list => Lists.removeAt(position, list))
    onPlaylistModified(songs.get)
    val idx = pos.get
    if (position <= idx && idx >= 0) {
      pos.transform(_ - 1)
      onPlaylistIndexChanged(pos.get)
    }
  }

  private def clearButDontTell() = atomic { implicit txn =>
    songs.transform(_ => Nil)
    pos.transform(_ => NO_POSITION)
  }

  def clear() {
    clearButDontTell()
    onPlaylistModified(songList)
    onPlaylistIndexChanged(index)
  }

  def set(song: T) = atomic { implicit txn =>
    clearButDontTell()
    add(song)
    index = 0
    log.info(s"Playlist set to: $song")
  }

  protected def onPlaylistIndexChanged(idx: PlaylistIndex) {

  }

  protected def onPlaylistModified(songs: Seq[T]) {

  }
}
