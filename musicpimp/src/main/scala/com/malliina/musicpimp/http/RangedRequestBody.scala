package com.malliina.musicpimp.http

import com.malliina.play.ContentRange
import com.malliina.util.Util
import okhttp3.{MediaType, RequestBody}
import okio.{BufferedSink, Okio}

import java.nio.file.Path

class RangedRequestBody(file: Path, range: ContentRange) extends RequestBody:
  override def contentType(): MediaType = null

  override def writeTo(bufferedSink: BufferedSink): Unit =
    Util.using(Okio.source(RangedInputStream(file, range))): stream =>
      bufferedSink.writeAll(stream)
