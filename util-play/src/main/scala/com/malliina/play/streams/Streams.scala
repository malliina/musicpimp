package com.malliina.play.streams

import java.io.*
import java.nio.file.Path

import org.apache.pekko.stream.scaladsl.*
import org.apache.pekko.util.ByteString
import com.malliina.storage.{StorageInt, StorageSize}

import scala.concurrent.{ExecutionContext, Future}

object Streams extends Streams

trait Streams:

  /** http://stackoverflow.com/questions/12066993/uploading-file-as-stream-in-play-framework-2-0
    *
    * @return
    *   an [[InputStream]] and a [[Sink]] such that any bytes consumed by the Iteratee are made
    *   available to the InputStream
    */
  def joinedStream(
    inputBuffer: StorageSize = 10.megs
  )(implicit ec: ExecutionContext): (PipedInputStream, Sink[ByteString, Future[Long]]) =
    val outStream = new PipedOutputStream()
    val bufferSize = math.min(inputBuffer.toBytes.toInt, Int.MaxValue)
    val inStream = new PipedInputStream(outStream, bufferSize)
    val iteratee = closingStreamWriter(outStream)
    (inStream, iteratee)

  /** Builds a [[Sink]] that writes consumed bytes to all `outStreams`. The streams are closed when
    * the [[Sink]] is done.
    *
    * @return
    *   a [[Sink]] that writes to `outStream`
    */
  def closingStreamWriter(
    outStreams: OutputStream*
  )(implicit ec: ExecutionContext): Sink[ByteString, Future[Long]] =
    streamWriter(outStreams*).mapMaterializedValue(_.andThen:
      case _ => outStreams.foreach(_.close())
    )

  /** @return
    *   a [[Sink]] that writes any consumed bytes to `os`
    */
  def streamWriter(
    outStreams: OutputStream*
  )(implicit ec: ExecutionContext): Sink[ByteString, Future[Long]] =
    byteConsumer: bytes =>
      outStreams.foreach(_.write(bytes.asByteBuffer.array()))

  /** @param f
    * @return
    *   an iteratee that consumes bytes by applying `f` and returns the total number of bytes
    *   consumed
    */
  def byteConsumer(
    f: ByteString => Unit
  )(implicit ec: ExecutionContext): Sink[ByteString, Future[Long]] =
    Sink.fold[Long, ByteString](0): (count, bytes) =>
      f(bytes)
      count + bytes.length

  /** @return
    *   a [[Sink]] that writes any consumed bytes to `os`
    */
  def fromOutputStream(
    os: OutputStream
  )(implicit ec: ExecutionContext): Sink[ByteString, Future[OutputStream]] =
    Sink.fold[OutputStream, ByteString](os): (state, bytes) =>
      state.write(bytes.asByteBuffer.array())
      state

  /** @param file
    *   destination file
    * @return
    *   a [[Sink]] that writes bytes to `file`, keeping track of the number of bytes written
    */
  def fileWriter(file: Path) = FileIO.toPath(file)
