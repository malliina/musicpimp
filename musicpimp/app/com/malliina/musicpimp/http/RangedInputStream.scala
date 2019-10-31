package com.malliina.musicpimp.http

import java.io.InputStream

import com.malliina.play.ContentRange

/**
  * @see http://stackoverflow.com/a/28119691/1863674
  */
class RangedInputStream(stream: InputStream, start: Int, rangeSize: Int) extends InputStream {
  def this(stream: InputStream, range: ContentRange) =
    this(stream, range.start, range.contentLength)

  private val NO_MORE_BYTES = -1
  val skipped = stream.skip(start.toLong)
  @volatile private var remaining = rangeSize

  override def read(b: Array[Byte], off: Int, len: Int): Int = {
    if (remaining > 0) {
      val bytesRead = stream.read(b, off, Math.min(remaining, len))
      if (bytesRead > 0) {
        remaining -= bytesRead
      }
      bytesRead
    } else {
      NO_MORE_BYTES
    }
  }

  override def read(): Int = {
    if (remaining > 0) {
      val byte = stream.read()
      if (byte >= 0) {
        remaining -= 1
      }
      byte
    } else {
      NO_MORE_BYTES
    }
  }

  override def available(): Int = Math.min(stream.available(), remaining)

  override def markSupported(): Boolean = false

  override def mark(readlimit: Int): Unit = ()

  override def close(): Unit = {
    stream.close()
  }
}
