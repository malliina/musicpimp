package com.malliina.pimpcloud.streams

import java.util.concurrent.atomic.AtomicBoolean

import akka.stream.QueueOfferResult
import akka.stream.scaladsl.SourceQueue
import akka.util.ByteString
import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.models.CloudID
import com.malliina.play.ContentRange
import play.api.Logger

import scala.concurrent.Future

class ChannelInfo(
  val channel: SourceQueue[Option[ByteString]],
  serverID: CloudID,
  val track: Track,
  val range: ContentRange
) extends StreamEndpoint {
  val isClosed = new AtomicBoolean(false)

  def send(t: ByteString): Future[QueueOfferResult] = {
    if (!isClosed.get()) {
//      log info s"Offering ${t.length} bytes of $describe"
      channel.offer(Option(t))
    } else {
      //log.warn(s"Tried to send from server '$serverID' to a closed channel of track '${track.title}'.")
      Future.successful(QueueOfferResult.QueueClosed)
    }
  }

  // This won't work if the previous offer has not yet completed.
  // Is failure of this method call a memory leak?
  def close(): Future[QueueOfferResult] = {
    isClosed.set(true)
    // I think this is unnecessary
    channel.offer(None)
  }
}

object ChannelInfo {
  private val log = Logger(getClass)
}
