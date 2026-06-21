package com.malliina.util

import cats.effect.IO
import com.malliina.logback.fs2.{DefaultFS2IOAppender, LoggingComps}
import com.malliina.logstreams.client.FS2Appender

class MusicPimpAppender(comps: LoggingComps[IO]) extends DefaultFS2IOAppender[IO](comps):
  def this() = this(FS2Appender.unsafe.comps)
