package com.malliina.play

import java.nio.file.{Files, Path}

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import com.malliina.storage.StorageLong
import play.api.Logger
import play.api.http.HeaderNames._
import play.api.http.{ContentTypes, FileMimeTypes}
import play.api.mvc.Results.{Ok, PartialContent}
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

object FileResults extends FileResults {
  private val log = Logger(getClass)
}

trait FileResults {
  def fileResult(path: Path, request: RequestHeader, fmts: FileMimeTypes)(
    implicit ec: ExecutionContext
  ): Result = {
    val range = ContentRanges.fromHeader(request, Files.size(path).bytes)
    range.toOption
      .map(range => rangedResult(path, range, request, fmts))
      .getOrElse(
        Ok.sendFile(path.toFile)(ec, fmts).withHeaders(ACCEPT_RANGES -> ContentRange.BYTES)
      )
  }

  def rangedResult(path: Path, range: ContentRange, request: RequestHeader, fmts: FileMimeTypes)(
    implicit ec: ExecutionContext
  ): Result = {
    val fileName = path.getFileName.toString
    val contentType = fmts.forFileName(fileName) getOrElse ContentTypes.BINARY
    val contentLength = range.contentLength.toLong
    val source: Source[ByteString, Future[IOResult]] = FileIO
      .fromPath(path)
      .drop(range.start.toLong)
      .take(contentLength)
    PartialContent
      .streamed(source, Option(contentLength), Option(contentType))
      .withHeaders(CONTENT_RANGE -> range.contentRange)
  }
}
