package com.malliina.audio

import cats.effect.Async
import cats.implicits.{toFlatMapOps, toFunctorOps}

type PlaylistIndex = Int

case class PlaylistState[T](songs: Seq[T], index: PlaylistIndex)

/** @tparam T
  *   type of playlist item
  */
trait IPlaylist[F[_]: Async, T]:
  def snapshot: F[PlaylistState[T]] =
    songList.flatMap: ss =>
      index.map: idx =>
        PlaylistState(ss, idx)

  def songList: F[Seq[T]]

  def index: F[PlaylistIndex]

  def setIndex(newIndex: PlaylistIndex): F[Unit]

  /** @return
    *   the current track wrapped in an Option if any, or None otherwise
    */
  def current: F[Option[T]]

  /** @return
    *   the next track wrapped in an Option if any, or None otherwise
    */
  def next: F[Option[T]]

  /** @return
    *   the previous track wrapped in an Option if any, or None otherwise
    */
  def prev: F[Option[T]]

  /** @param song
    *   to add
    */
  def add(song: T): F[Unit]

  /** @param pos
    *   index of track to remove
    */
  def delete(pos: Int): F[Unit]

  def clear(): F[Unit]
