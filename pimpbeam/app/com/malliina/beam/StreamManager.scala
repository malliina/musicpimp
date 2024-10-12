package com.malliina.beam

import java.util.concurrent.atomic.AtomicBoolean

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.{Source, SourceQueue}
import org.apache.pekko.stream.{Materializer, QueueOfferResult}
import org.apache.pekko.util.ByteString

import scala.concurrent.Future

/** @see
  *   http://greweb.me/2012/08/zound-a-playframework-2-audio-streaming-experiment-using-iteratees/
  */
class StreamManager(val stream: Source[ByteString, ?], val channel: SourceQueue[Option[ByteString]])
  extends StreamEndpoint:
  private val isClosed = new AtomicBoolean(false)
  @volatile
  var isReceivingStream: Boolean = false

  def send(t: ByteString): Future[QueueOfferResult] =
    if !isClosed.get() then channel.offer(Option(t))
    else closed

  def close(): Future[QueueOfferResult] =
    val wasOpen = isClosed.compareAndSet(false, true)
    if wasOpen then channel.offer(None)
    else closed

  def closed = Future.successful(QueueOfferResult.QueueClosed)

object StreamManager:
  def empty(mat: Materializer) =
    val (queue, source) = Streaming.sourceQueue[ByteString](mat)

    apply(source, queue)

  def apply(stream: Source[ByteString, NotUsed], channel: SourceQueue[Option[ByteString]]) =
    new StreamManager(stream, channel)

trait StreamEndpoint:
  def send(bytes: ByteString): Future[QueueOfferResult]

  def close(): Future[QueueOfferResult]
