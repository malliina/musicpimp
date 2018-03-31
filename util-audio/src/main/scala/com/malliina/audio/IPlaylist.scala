package com.malliina.audio

/**
  * @tparam T type of playlist item
  */
trait IPlaylist[T] {
  def songList: Seq[T]

  def index: Int

  def index_=(newIndex: Int)

  /**
    *
    * @return the current track wrapped in an Option if any, or None otherwise
    */
  def current: Option[T]

  /**
    *
    * @return the next track wrapped in an Option if any, or None otherwise
    */
  def next: Option[T]

  /**
    *
    * @return the previous track wrapped in an Option if any, or None otherwise
    */
  def prev: Option[T]

  /**
    *
    * @param song to add
    */
  def add(song: T)

  /**
    *
    * @param pos index of track to remove
    */
  def delete(pos: Int)

  def clear()
}
