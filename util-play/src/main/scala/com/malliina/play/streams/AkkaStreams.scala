package com.malliina.play.streams

import org.apache.pekko.stream.scaladsl.{Flow, SourceQueue}

object AkkaStreams:
  def offeringSink[T](queue: SourceQueue[T]) = Flow[T].mapAsync(1)(queue.offer)
