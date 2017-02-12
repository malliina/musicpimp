package com.malliina.pimpcloud.streams

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.actor.ActorPublisher
import akka.util.ByteString

class ByteActor extends Actor with ActorPublisher[ByteString] with ActorLogging {
  override def receive: Receive = {
    case bytes: ByteString =>
      log.info(s"Got ${bytes.length} bytes")
      onNext(bytes)
  }
}

object ByteActor {
  def props() = Props(new ByteActor())
}
