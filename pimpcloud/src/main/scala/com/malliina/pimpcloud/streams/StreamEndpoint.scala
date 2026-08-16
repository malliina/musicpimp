package com.malliina.pimpcloud.streams

import com.malliina.musicpimp.audio.Track
import com.malliina.play.ContentRange

trait StreamEndpoint[F[_]]:
  def track: Track
  def range: ContentRange
  def send(bytes: Seq[Byte]): F[Boolean]
  def close: F[Boolean]
  def describe: String = s"${track.title} with range $range"
