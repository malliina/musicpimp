package com.malliina.musicpimp.audio

import cats.effect.{Async, Ref}
import cats.implicits.toFunctorOps
import com.malliina.audio.PlaylistIndex
import fs2.Stream
import fs2.concurrent.Topic
import fs2.concurrent.Topic.Closed

class PimpPlaylist[F[_]: Async](
  eventHub: Topic[F, ServerMessage],
  val pos: Ref[F, PlaylistIndex],
  val songs: Ref[F, Seq[PlayableTrack]]
) extends BasePlaylist[F, PlayableTrack]
  with AutoCloseable:
  val events: Stream[F, ServerMessage] = eventHub.subscribe(100)

  protected override def onPlaylistIndexChanged(idx: Int): F[Unit] =
    send(PlaylistIndexChangedMessage(idx)).void

  protected override def onPlaylistModified(tracks: Seq[PlayableTrack]): F[Unit] =
    send(PlaylistModifiedMessage(tracks)).void

  def send(json: ServerMessage): F[Either[Closed, Unit]] =
    eventHub.publish1(json)

  def close(): Unit = ()
