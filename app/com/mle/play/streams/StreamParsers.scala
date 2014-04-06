package com.mle.play.streams

import play.api.mvc.BodyParsers.parse.Multipart._
import play.api.mvc.BodyParsers.parse
import play.api.libs.iteratee._
import com.mle.util.Log
import play.api.mvc.{BodyParser, MultipartFormData}
import java.io._
import play.api.mvc.MultipartFormData.FilePart
import scala.concurrent.ExecutionContext

/**
 *
 * @author mle
 */
trait StreamParsers extends Log {
  /**
   * Pushes the bytes to the supplied channel as they are received.
   *
   * @param dest channel to push to
   */
  def multiPartByteStreaming(dest: Concurrent.Channel[Array[Byte]])(implicit ec: ExecutionContext): BodyParser[MultipartFormData[Long]] =
    multiPartByteStreaming(bytes => dest push bytes)

  def multiPartByteStreaming(f: Array[Byte] => Unit)(implicit ec: ExecutionContext): BodyParser[MultipartFormData[Long]] =
    parse.multipartFormData(byteArrayPartConsumer(f))

  /**
   * Parses a multipart form-data upload in such a way that any parsed bytes are made available
   * to the returned [[InputStream]].
   *
   * @return
   */
  def multiPartStreamPiping()(implicit ec: ExecutionContext): (InputStream, BodyParser[MultipartFormData[Long]]) = {
    val (inStream, iteratee) = Streams.joinedStream()
    val parser = multiPartBodyParser(iteratee)
    (inStream, parser)
  }

  def multiPartBodyParser[T](iteratee: Iteratee[Array[Byte], T]): BodyParser[MultipartFormData[T]] =
    parse.multipartFormData(byteArrayPartHandler(iteratee))

  /**
   * Builds a part handler that applies the supplied function
   * to the array of bytes as they are received.
   *
   * @param f what to do with the bytes
   * @return a part handler memorizing the total number of bytes consumed
   */
  def byteArrayPartConsumer(f: Array[Byte] => Unit)(implicit ec: ExecutionContext): PartHandler[FilePart[Long]] =
    byteArrayPartHandler(Streams.byteConsumer(f))

  /**
   * Builds a part handler that uses the supplied iteratee to handle the bytes as they are received.
   *
   * @param in input byte handler
   * @tparam T eventual value produced by iteratee
   * @return
   */
  def byteArrayPartHandler[T](in: Iteratee[Array[Byte], T]): PartHandler[FilePart[T]] = {
    // note: seems this is shorthand for handleFilePart(fileInfo => fileInfo match { case { ... }})
    parse.Multipart.handleFilePart {
      case parse.Multipart.FileInfo(partName, fileName, contentType) =>
        in
    }
  }
}

object StreamParsers extends StreamParsers
