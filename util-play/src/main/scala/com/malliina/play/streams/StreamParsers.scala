package com.malliina.play.streams

import java.io.*

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.*
import org.apache.pekko.util.ByteString
import com.malliina.storage.StorageSize
import play.api.http.HttpErrorHandler
import play.api.libs.streams.Accumulator
import play.api.mvc.MultipartFormData.*
import play.api.mvc.{BodyParser, MultipartFormData}
import play.core.parsers.Multipart
import play.core.parsers.Multipart.{FileInfo, FilePartHandler}

import scala.concurrent.{ExecutionContext, Future}

trait StreamParsers:

  /** Pushes the bytes to the supplied channel as they are received.
    *
    * @param dest
    *   channel to push to
    */
  def multiPartChannelStreaming(
    dest: SourceQueue[ByteString],
    maxLength: StorageSize,
    errorHandler: HttpErrorHandler
  )(implicit mat: Materializer): BodyParser[MultipartFormData[Long]] =
    multiPartByteStreaming(
      bytes => dest.offer(bytes).map(_ => ())(mat.executionContext),
      maxLength,
      errorHandler
    )

  def multiPartByteStreaming(
    f: ByteString => Future[Unit],
    maxLength: StorageSize,
    errorHandler: HttpErrorHandler
  )(implicit mat: Materializer): BodyParser[MultipartFormData[Long]] =
    Multipart.multipartParser(maxLength.toBytes.toInt, true, byteArrayPartConsumer(f), errorHandler)

  /** Parses a multipart form-data upload in such a way that any parsed bytes are made available to
    * the returned [[InputStream]].
    *
    * @return
    */
  def multiPartStreamPiping(maxLength: StorageSize, errorHandler: HttpErrorHandler)(implicit
    mat: Materializer
  ): (InputStream, BodyParser[MultipartFormData[Long]]) =
    val (inStream, iteratee) = Streams.joinedStream()(mat.executionContext)
    val parser = multiPartBodyParser(iteratee, maxLength, errorHandler)
    (inStream, parser)

  def multiPartBodyParser[T](
    sink: Sink[ByteString, Future[T]],
    maxLength: StorageSize,
    errorHandler: HttpErrorHandler
  )(implicit mat: Materializer): BodyParser[MultipartFormData[T]] =
    Multipart.multipartParser(
      maxLength.toBytes.toInt,
      true,
      byteArrayPartHandler(sink)(mat.executionContext),
      errorHandler
    )

  /** Builds a part handler that applies the supplied function to the array of bytes as they are
    * received.
    *
    * @param f
    *   what to do with the bytes
    * @return
    *   a part handler memorizing the total number of bytes consumed
    */
  protected def byteArrayPartConsumer(
    f: ByteString => Future[Unit]
  )(implicit mat: Materializer): FilePartHandler[Long] =
    val byteCalculator: Sink[ByteString, Future[Long]] =
      Sink.fold[Long, ByteString](0)((acc, bytes) => acc + bytes.length)
    val asyncSink = Flow[ByteString]
      .mapAsync(1)(bytes => f(bytes).map(_ => bytes)(mat.executionContext))
      .toMat(byteCalculator)(Keep.right)
    byteArrayPartHandler(asyncSink)(mat.executionContext)

  /** Builds a part handler that uses the supplied sink to handle the bytes as they are received.
    *
    * @param sink
    *   input byte handler
    * @tparam T
    *   eventual value produced by iteratee
    * @return
    */
  private def byteArrayPartHandler[T](
    sink: Sink[ByteString, Future[T]]
  )(implicit ec: ExecutionContext): Multipart.FilePartHandler[T] =
    handleFilePart(_ => Accumulator(sink))

  protected def byteArrayPartHandler2[T](
    acc: Accumulator[ByteString, FilePart[T]]
  ): Multipart.FilePartHandler[T] = { case FileInfo(_, _, _, _) =>
    acc
  }

  /** Taken from Multipart.scala.
    */
  protected def handleFilePart[A](
    handler: FileInfo => Accumulator[ByteString, A]
  )(implicit ec: ExecutionContext): FilePartHandler[A] = (fi: FileInfo) =>
    val safeFileName = fi.fileName.split('\\').takeRight(1).mkString
    val partName = fi.partName
    val contentType = fi.contentType
    handler(FileInfo(partName, safeFileName, contentType))
      .map(a => FilePart(partName, safeFileName, contentType, a))

object StreamParsers extends StreamParsers {
  //  private val log = Logger(getClass)
}
