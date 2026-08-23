package com.malliina.beam

import cats.effect.{Async, Ref}
import cats.implicits.{toFlatMapOps, toFunctorOps}
import com.malliina.beam.PlayerClient.log
import com.malliina.musicpimp.json.Target
import com.malliina.util.AppLogger
import com.malliina.values.Username
import fs2.concurrent.Topic

object PlayerClient:
  private val log = AppLogger(getClass)

  def default[F[_]: Async](user: Username, target: Target[F]) =
    for
      initial <- makeStreamer[F]
      ref <- Ref.of[F, StreamManager[F]](initial)
    yield PlayerClient(user, target, ref)

  private def makeStreamer[F[_]: Async] = Topic[F, Option[Array[Byte]]].map: topic =>
    StreamManager(topic)

class PlayerClient[F[_]: Async](
  val user: Username,
  target: Target[F],
  current: Ref[F, StreamManager[F]]
):
  def send(bytes: Array[Byte]) = current.get.flatMap(c => c.send(bytes))

  def stream: fs2.Stream[F, Array[Byte]] = fs2.Stream
    .eval(current.get)
    .flatMap(s =>
      s.stream
        .takeWhile(_.isDefined)
        .flatMap(opt => fs2.Stream.emit(opt.getOrElse(Array.empty[Byte])))
    )

  /** Ends the current stream and replaces it with a new one, then sends a reset message to the
    * client so that the client will receive the newly created stream instead.
    *
    * This is used when the user starts playing a new track, discarding any currently playing
    * stream.
    */
  def resetStream: F[Unit] =
    log.info(s"Resetting streaming for '$user'...")
    for
      prev <- current.get
      _ <- prev.close
      next <- PlayerClient.makeStreamer[F]
      _ <- current.updateAndGet(_ => next)
      // instructs the client to GET /stream
      _ <- target.send(BeamMessages.reset)
    yield ()

  def close = current.get.flatMap(_.close)
