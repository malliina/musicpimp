package com.malliina.play

import java.io.FileInputStream
import java.nio.file.{Files, Path}

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Source, StreamConverters}
import akka.util.ByteString
import com.malliina.musicpimp.http.RangedInputStream
import com.malliina.play.FileResults.log
import com.malliina.play.http.RequestHeaderOps
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
    val size = Files.size(path).bytes
    ContentRanges
      .fromHeader(request, size)
      .toOption
      .map { range =>
        rangedResult(path, range, request, fmts)
      }
      .getOrElse {
        log.info(
          s"Serving all of '$path', $size, as response to ${request.describe}..."
        )
        Ok.sendFile(path.toFile)(ec, fmts).withHeaders(ACCEPT_RANGES -> ContentRange.BYTES)
      }
  }

  def rangedResult(
    path: Path,
    range: ContentRange,
    request: RequestHeader,
    fmts: FileMimeTypes
  )(implicit ec: ExecutionContext): Result = {
    val fileName = path.getFileName.toString
    val contentType = fmts.forFileName(fileName) getOrElse ContentTypes.BINARY
    val contentLength = range.contentLength.toLong
    // FileIO.fromPath().drop(...).take(...) does not seem to actually `.take()`, so I use my own range implementation
    val source: Source[ByteString, Future[IOResult]] = StreamConverters
      .fromInputStream(
        () =>
          new RangedInputStream(new FileInputStream(path.toFile), range.start, range.contentLength)
      )
    log.info(
      s"Serving range $range of '$path' as '$contentType' response to ${request.describe}..."
    )
    PartialContent
      .streamed(source, Option(contentLength), Option(contentType))
      .withHeaders(CONTENT_RANGE -> range.contentRange)
  }
}
