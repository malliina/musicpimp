package com.malliina.play

import java.nio.file.{Files, Path}

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import com.malliina.storage.StorageLong
import play.api.Logger
import play.api.http.HeaderNames._
import play.api.http.{ContentTypes, FileMimeTypes, HttpEntity, Status}
import play.api.mvc.{RequestHeader, ResponseHeader, Result, Results}

import scala.concurrent.{ExecutionContext, Future}

object FileResults extends FileResults {
  private val log = Logger(getClass)
}

trait FileResults {
  def fileResult(path: Path, request: RequestHeader, fmts: FileMimeTypes)(implicit ec: ExecutionContext): Result = {
    val range = ContentRanges.fromHeader(request, Files.size(path).bytes)
    range.toOption
      .map(range => rangedResult(path, range, request, fmts))
      .getOrElse(Results.Ok.sendFile(path.toFile)(ec, fmts).withHeaders(ACCEPT_RANGES -> ContentRange.BYTES))
  }

  def rangedResult(path: Path, range: ContentRange, request: RequestHeader, fmts: FileMimeTypes)(implicit ec: ExecutionContext): Result = {
    val fileName = path.getFileName.toString
    val contentType = fmts.forFileName(fileName) getOrElse ContentTypes.BINARY
    val contentLength = range.contentLength.toLong
    val responseHeader = ResponseHeader(Status.PARTIAL_CONTENT, Map(
      CONTENT_RANGE -> range.contentRange,
      CONTENT_LENGTH -> s"$contentLength",
      CONTENT_TYPE -> fmts.forFileName(fileName).getOrElse(ContentTypes.BINARY)))
    val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(path)
      .drop(range.start.toLong)
      .take(contentLength)
    //    val loggedSource: Source[ByteString, Future[IOResult]] = source.watchTermination() { (io, done) =>
    //      log.info(s"Watching streaming of $fileName with range ${request.headers.get(RANGE)}")
    //      io.onComplete(t => log.info(s"Completed IO for $fileName with range ${request.headers.get(RANGE)}"))
    //      done.onComplete(t => log.info(s"Done streaming $fileName with range ${request.headers.get(RANGE)}"))
    //      io
    //    }
    val streamedBody = HttpEntity.Streamed(source, Option(contentLength), Option(contentType))
    Result(responseHeader, streamedBody)
  }
}
