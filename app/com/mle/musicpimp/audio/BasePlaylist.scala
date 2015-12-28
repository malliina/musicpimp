package com.mle.musicpimp.audio

import com.mle.audio.IPlaylist
import com.mle.util.Log

import scala.concurrent.stm._

/**
  *
  * @tparam T type of playlist item
  */
trait BasePlaylist[T]
  extends IPlaylist[T]
  with Log {

  type PlaylistIndex = Int
  val NO_POSITION = -1

  def pos: Ref[PlaylistIndex]

  def songs: Ref[Seq[T]]

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
    songs.transform(list => insertAt(position, list, song))
    onPlaylistModified(songs.get)
    val idx = pos.get
    if (position <= idx && songs.get.size > idx + 1) {
      pos.transform(_ + 1)
      onPlaylistIndexChanged(pos.get)
    }
  }


  def delete(position: PlaylistIndex) = atomic { implicit txn =>
    songs.transform(list => removeAt(position, list))
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

  private def insertAt(pos: Int, xs: Seq[T], elem: T) = {
    val (left, right) = xs.splitAt(pos)
    left ++ Seq(elem) ++ right
  }

  private def removeAt(pos: Int, xs: Seq[T]): Seq[T] = {
    val (left, right) = xs.splitAt(pos)
    left ++ right.drop(1)
  }
}
