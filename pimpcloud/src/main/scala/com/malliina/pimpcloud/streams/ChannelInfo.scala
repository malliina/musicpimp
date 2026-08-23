package com.malliina.pimpcloud.streams

import cats.effect.Async
import cats.implicits.toFunctorOps
import cats.syntax.all.toFlatMapOps
import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.models.CloudID
import com.malliina.pimpcloud.streams.ChannelInfo.log
import com.malliina.play.ContentRange
import com.malliina.util.AppLogger
import fs2.concurrent.Topic

import java.util.concurrent.atomic.AtomicBoolean

object ChannelInfo:
  private val log = AppLogger(getClass)

class ChannelInfo[F[_]: Async](
  val channel: Topic[F, Option[Seq[Byte]]],
  serverID: CloudID,
  val track: Track,
  val range: ContentRange
) extends StreamEndpoint[F]:
  val F = Async[F]
  private val isClosed = new AtomicBoolean(false)

  def send(t: Seq[Byte]): F[Boolean] =
    if !isClosed.get() then
//      log.info(s"Offering ${t.length} bytes of $describe")
      channel.publish1(Option(t)).map(_.isRight)
    else
      F.delay:
        log.warn(
          s"Tried to send from server '$serverID' to a closed channel of track '${track.title}'."
        )
        false

  def close: F[Boolean] =
    for
      _ <- channel.publish1(None).void
      closed <- channel.close
      _ <- F.delay(isClosed.set(true))
    yield closed.isRight
