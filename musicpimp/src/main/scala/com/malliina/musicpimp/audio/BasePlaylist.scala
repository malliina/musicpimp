package com.malliina.musicpimp.audio

import com.malliina.audio.{IPlaylist, PlaylistIndex}
import com.malliina.musicpimp.audio.BasePlaylist.log
import com.malliina.util.{AppLogger, Lists}
import cats.effect.{Async, Ref}
import cats.implicits.{catsSyntaxFlatMapOps, toFlatMapOps, toFunctorOps}

object BasePlaylist:
  private val log = AppLogger(getClass)
  val NoPosition: PlaylistIndex = -1

/** @tparam T
  *   type of playlist item
  */
trait BasePlaylist[F[_]: Async, T] extends IPlaylist[F, T]:
  val F = Async[F]
  private val NO_POSITION = BasePlaylist.NoPosition

  protected def pos: Ref[F, PlaylistIndex]

  protected def songs: Ref[F, Seq[T]]

  def songList: F[Seq[T]] = songs.get

  /** The playlist index. An empty playlist implies index == NO_POSITION, but index == NO_POSITION
    * does not imply an empty playlist.
    *
    * @return
    *   the playlist index, or NO_POSITION if no playlist item is selected or the playlist is empty
    */
  def index: F[PlaylistIndex] = pos.get

  override def setIndex(newIndex: PlaylistIndex): F[Unit] =
    if newIndex < 0 then
      F.raiseError(IllegalArgumentException(s"Negative playlist position: $newIndex"))
    songList.flatMap: ss =>
      if newIndex >= ss.size then
        F.raiseError(
          IllegalArgumentException(
            s"No song at index: $newIndex, playlist only contains ${ss.size} tracks"
          )
        )
      index.flatMap: oldIndex =>
        val changed = oldIndex != newIndex
        pos
          .set(newIndex)
          .flatMap(_ => pos.get)
          .flatMap: updated =>
            if changed then onPlaylistIndexChanged(updated) else F.unit

  def current: F[Option[T]] =
    for
      ss <- songList
      idx <- index
    yield Option.when(idx >= 0 && ss.size > idx)(ss(idx))

  def next: F[Option[T]] =
    songList.flatMap: tracks =>
      index.flatMap: idx =>
        if tracks.size > idx + 1 then
          for
            updated <- pos.updateAndGet(_ + 1)
            _ <- onPlaylistIndexChanged(updated)
          yield Option(tracks(updated))
        else F.pure(None)

  def prev: F[Option[T]] =
    songList.flatMap: tracks =>
      index.flatMap: idx =>
        if idx > 0 && tracks.size > idx - 1 then
          for
            updated <- pos.updateAndGet(_ - 1)
            _ <- onPlaylistIndexChanged(updated)
          yield Option(tracks(updated))
        else F.pure(None)

  def add(song: T): F[Unit] =
    songs
      .updateAndGet(ss => ss :+ song)
      .flatMap: updated =>
        onPlaylistModified(updated)

  def insert(position: PlaylistIndex, song: T): F[Unit] =
    for
      inserted <- songs.updateAndGet(list => Lists.insertAt(position, list, song))
      _ <- onPlaylistModified(inserted)
      idx <- index
      _ <-
        if position <= idx && inserted.size > idx + 1 then
          pos.updateAndGet(_ + 1).flatMap(newIndex => onPlaylistIndexChanged(newIndex))
        else F.unit
    yield ()

  def move(sourcePosition: PlaylistIndex, destPosition: PlaylistIndex): F[Unit] =
    songList.flatMap: tracks =>
      index.flatMap: currentIndex =>
        val songCount = tracks.size
        val isActionable =
          sourcePosition != destPosition &&
            sourcePosition < songCount &&
            destPosition < songCount &&
            sourcePosition >= 0 &&
            destPosition >= 0
        if isActionable then
          val newIndex = indexAfterMove(currentIndex, sourcePosition, destPosition)
          for
            updated <- songs.updateAndGet(ts => Lists.move(sourcePosition, destPosition, ts))
            _ <- onPlaylistModified(updated)
            _ <- setIndex(newIndex)
          yield ()
        else F.unit

  private def indexAfterMove(current: PlaylistIndex, src: Int, dest: Int): PlaylistIndex =
    if src == current then
      // current one being moved
      dest
    else if src < current && dest >= current then
      // removed from below
      current - 1
    else if src > current && dest <= current then
      // added to below
      current + 1
    else current

  def reset(position: PlaylistIndex, tracks: Seq[T]): F[Unit] =
    for
      _ <- clearButDontTell()
      previousSongs <- songs.getAndUpdate(_ => tracks)
      previousIndex <- pos.getAndUpdate(_ => position)
      _ <- if previousSongs != tracks then onPlaylistModified(tracks) else F.unit
      _ <- if previousIndex != position then onPlaylistIndexChanged(position) else F.unit
    yield ()

  def delete(position: PlaylistIndex): F[Unit] =
    for
      deleted <- songs.updateAndGet(list => Lists.removeAt(position, list))
      _ <- onPlaylistModified(deleted)
      idx <- index
      _ <-
        if position <= idx && idx >= 0 then
          pos.updateAndGet(_ - 1).flatMap(newIndex => onPlaylistIndexChanged(newIndex))
        else F.unit
    yield ()

  private def clearButDontTell(): F[Unit] =
    songs.set(Nil) >> pos.set(NO_POSITION)

  def clear(): F[Unit] =
    for
      _ <- clearButDontTell()
      _ <- onPlaylistModified(Nil)
      _ <- onPlaylistIndexChanged(NO_POSITION)
    yield ()

  def set(song: T): F[Unit] =
    for
      _ <- clearButDontTell()
      _ <- add(song)
      - <- setIndex(0)
    yield log.info(s"Playlist set to: $song")

  protected def onPlaylistIndexChanged(idx: PlaylistIndex): F[Unit] = F.unit

  protected def onPlaylistModified(songs: Seq[T]): F[Unit] = F.unit
