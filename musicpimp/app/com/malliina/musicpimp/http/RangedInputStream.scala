package com.malliina.musicpimp.http

import java.io.{FileInputStream, InputStream}
import java.nio.file.Path

import com.malliina.play.ContentRange

object RangedInputStream {
  def apply(file: Path, start: Int, rangeSize: Int): RangedInputStream =
    new RangedInputStream(new FileInputStream(file.toFile), start, rangeSize)

  def apply(file: Path, range: ContentRange): RangedInputStream =
    apply(file, range.start, range.contentLength)
}

/**
  * @see http://stackoverflow.com/a/28119691/1863674
  */
class RangedInputStream(stream: InputStream, start: Int, rangeSize: Int) extends InputStream {
  private val NO_MORE_BYTES = -1
  val skipped = stream.skip(start.toLong)
  @volatile private var remaining = rangeSize

  override def read(b: Array[Byte], off: Int, len: Int): Int =
    if (remaining > 0) {
      val bytesRead = stream.read(b, off, Math.min(remaining, len))
      if (bytesRead > 0) {
        remaining -= bytesRead
      }
      bytesRead
    } else {
      NO_MORE_BYTES
    }

  override def read(): Int =
    if (remaining > 0) {
      val byte = stream.read()
      if (byte >= 0) {
        remaining -= 1
      }
      byte
    } else {
      NO_MORE_BYTES
    }

  override def available(): Int = Math.min(stream.available(), remaining)
  override def markSupported(): Boolean = false
  override def mark(readlimit: Int): Unit = ()
  override def close(): Unit = stream.close()
}
