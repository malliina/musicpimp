package com.malliina.pimpcloud.ws

import akka.util.ByteString
import com.malliina.musicpimp.audio.Track
import com.malliina.play.ContentRange
import rx.lang.scala.Subject

case class StreamInfo(track: Track, range: ContentRange, stream: Subject[ByteString])
