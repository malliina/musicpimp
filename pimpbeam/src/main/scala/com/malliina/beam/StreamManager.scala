package com.malliina.beam

import cats.effect.Async
import cats.implicits.{toFlatMapOps, toFunctorOps}
import fs2.concurrent.Topic

class StreamManager[F[_]: Async](
  val channel: Topic[F, Option[Array[Byte]]]
) extends StreamEndpoint[F]:
  val stream = channel.subscribe(100)
  val F = Async[F]

  def send(bytes: Array[Byte]): F[Boolean] =
    channel.isClosed.flatMap: isClosed =>
      if !isClosed then channel.publish1(Option(bytes)).map(_.isRight)
      else F.pure(false)

  def close: F[Boolean] =
    channel.isClosed.flatMap: isClosed =>
      if !isClosed then channel.publish1(None).map(_.isRight)
      else F.pure(false)

trait StreamEndpoint[F[_]]:
  def send(bytes: Array[Byte]): F[Boolean]
  def close: F[Boolean]
