package com.malliina.pimpcloud.streams

import com.malliina.musicpimp.audio.Track
import com.malliina.play.ContentRange
import org.apache.pekko.stream.QueueOfferResult
import org.apache.pekko.util.ByteString

import scala.concurrent.Future

trait StreamEndpoint:
  def track: Track

  def range: ContentRange

  def send(bytes: ByteString): Future[QueueOfferResult]

  def close(): Future[QueueOfferResult]

  def describe: String = s"${track.title} with range $range"
