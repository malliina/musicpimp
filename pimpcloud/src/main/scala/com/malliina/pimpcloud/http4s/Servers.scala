package com.malliina.pimpcloud.http4s

import cats.effect.{Async, Ref}
import cats.implicits.{toFlatMapOps, toFunctorOps}
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.musicpimp.models.CloudID
import fs2.Stream
import fs2.concurrent.Topic

trait Servers[F[_]]:
  def updates: Stream[F, PimpServerSocket[F]]
  def connectedServers: F[Set[PimpServerSocket[F]]]
  def connected(server: PimpServerSocket[F]): F[Set[PimpServerSocket[F]]]
  def disconnected(id: CloudID): F[Set[PimpServerSocket[F]]]

object Servers:
  def default[F[_]: Async]: F[Servers[F]] =
    for
      ref <- Ref.of[F, Set[PimpServerSocket[F]]](Set.empty)
      topic <- Topic[F, PimpServerSocket[F]]
    yield MusicPimpServers[F](ref, topic)

class MusicPimpServers[F[_]](
  serversRef: Ref[F, Set[PimpServerSocket[F]]],
  changes: Topic[F, PimpServerSocket[F]]
) extends Servers[F]:
  val updates: Stream[F, PimpServerSocket[F]] = changes.subscribe(100)

  override def connectedServers: F[Set[PimpServerSocket[F]]] = serversRef.get

  override def connected(server: PimpServerSocket[F]): F[Set[PimpServerSocket[F]]] =
    serversRef.updateAndGet(set => set ++ Set(server))

  override def disconnected(id: CloudID): F[Set[PimpServerSocket[F]]] =
    serversRef.updateAndGet(_.filterNot(_.id == id))
