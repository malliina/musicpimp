package com.mle.play.streams

import java.io._
import play.api.libs.iteratee.{Done, Input, Cont, Iteratee}
import java.nio.file.Path
import scala.concurrent.ExecutionContext
import com.mle.storage.StorageSize
import com.mle.storage.StorageInt

/**
 *
 * @author mle
 */
trait Streams {
  /**
   * http://stackoverflow.com/questions/12066993/uploading-file-as-stream-in-play-framework-2-0
   *
   * @return an [[InputStream]] and an [[Iteratee]] such that any bytes consumed by the Iteratee are made available to the InputStream
   */
  def joinedStream(inputBuffer: StorageSize = 10.megs)(implicit ec: ExecutionContext): (InputStream, Iteratee[Array[Byte], Long]) = {
    val outStream = new PipedOutputStream()
    val bufferSize = math.min(inputBuffer.toBytes.toInt, Int.MaxValue)
    val inStream = new PipedInputStream(outStream, bufferSize)
    val iteratee = closingStreamWriter(outStream)
    (inStream, iteratee)
  }

  /**
   * @return an [[Iteratee]] that writes any consumed bytes to `os`
   */
  def streamWriter(outStreams: OutputStream*)(implicit ec: ExecutionContext): Iteratee[Array[Byte], Long] =
    byteConsumer(bytes => {
      outStreams.foreach(_.write(bytes))
    })

  /**
   * Builds an [[Iteratee]] that writes consumed bytes to all `outStreams`. The streams are closed when the [[Iteratee]] is done.
   *
   * @return an [[Iteratee]] that writes to `outStream`
   */
  def closingStreamWriter(outStreams: OutputStream*)(implicit ec: ExecutionContext): Iteratee[Array[Byte], Long] =
    streamWriter(outStreams: _*).map(bytes => {
      outStreams.foreach(_.close())
      bytes
    })

  /**
   * @param f
   * @return an iteratee that consumes bytes by applying `f` and returns the total number of bytes consumed
   */
  def byteConsumer(f: Array[Byte] => Unit)(implicit ec: ExecutionContext): Iteratee[Array[Byte], Long] =
    Iteratee.fold[Array[Byte], Long](0)((count, bytes) => {
      f(bytes)
      count + bytes.length
    })

  /**
   * @param file destination file
   * @return an [[Iteratee]] that writes bytes to `file`, keeping track of the number of bytes written
   */
  def fileWriter(file: Path): Iteratee[Array[Byte], Long] = {
    def fromStreamAcc(stream: OutputStream, bytesWritten: Long): Iteratee[Array[Byte], Long] = Cont {
      case e@Input.EOF =>
        stream.close()
        Done(bytesWritten, e)
      case Input.El(data) =>
        stream.write(data)
        fromStreamAcc(stream, bytesWritten + data.length)
      case Input.Empty =>
        fromStreamAcc(stream, bytesWritten)
    }
    val outputStream = new BufferedOutputStream(new FileOutputStream(file.toFile))
    fromStreamAcc(outputStream, 0)
  }

  def fileWriter2(file: Path)(implicit ec: ExecutionContext): Iteratee[Array[Byte], Long] = {
    val outputStream = new BufferedOutputStream(new FileOutputStream(file.toFile))
    closingStreamWriter(outputStream)
  }
}

object Streams extends Streams
