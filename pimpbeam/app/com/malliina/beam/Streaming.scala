package com.malliina.beam

import akka.NotUsed
import akka.stream.scaladsl.{Keep, Sink, Source, SourceQueue}
import akka.stream.{Materializer, OverflowStrategy}

object Streaming {
  val DefaultBufferSizeInElements = 10000000

  /** Builds a [[SourceQueue]] and a [[Source]], so that elements offered to the [[SourceQueue]]
    * will be emitted by the [[Source]].
    *
    * @param mat        materializer
    * @param bufferSize the buffer size, in element count
    * @tparam T type of element
    * @return a [[SourceQueue]] and a [[Source]]
    */
  def sourceQueue[T](mat: Materializer, bufferSize: Int = DefaultBufferSizeInElements): (SourceQueue[Option[T]], Source[T, NotUsed]) = {
    val source = Source.queue[Option[T]](bufferSize, OverflowStrategy.backpressure)
      .takeWhile(_.isDefined).map(_.get)
    val (queue, publisher) = source.toMat(Sink.asPublisher(fanout = false))(Keep.both).run()(mat)
    val src = Source.fromPublisher(publisher)
    (queue, src)
  }
}
