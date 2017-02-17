package com.malliina.pimpcloud.streams

import akka.actor.{ActorRef, PoisonPill}
import akka.stream.QueueOfferResult
import akka.util.ByteString
import com.malliina.musicpimp.audio.Track
import com.malliina.play.ContentRange

import scala.concurrent.Future

// Not used. TODO evaluate.
class ActorStream(target: ActorRef, val track: Track, val range: ContentRange) extends StreamEndpoint {
  override def send(bytes: ByteString): Future[QueueOfferResult] = {
    target ! bytes
    Future.successful(QueueOfferResult.Enqueued)
  }

  override def close(): Future[QueueOfferResult] = {
    target ! PoisonPill
    Future.successful(QueueOfferResult.Enqueued)
  }
}
