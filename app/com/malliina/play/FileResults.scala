package com.malliina.play

import java.nio.file.{Files, Path}

import akka.stream.scaladsl.FileIO
import com.malliina.storage.StorageLong
import play.api.http.HeaderNames._
import play.api.http.{ContentTypes, HttpEntity, Status}
import play.api.libs.MimeTypes
import play.api.mvc.{RequestHeader, ResponseHeader, Result, Results}

object FileResults extends FileResults

trait FileResults {
  def fileResult(path: Path, request: RequestHeader): Result = {
    ContentRange.fromHeader(request, Files.size(path).bytes).toOption
      .map(range => rangedResult(path, range))
      .getOrElse(Results.Ok.sendFile(path.toFile).withHeaders(ACCEPT_RANGES -> ContentRange.BYTES))
  }

  def rangedResult(path: Path, range: ContentRange): Result = {
    val fileName = path.getFileName.toString
    val contentType = MimeTypes.forFileName(fileName).getOrElse(ContentTypes.BINARY)
    val contentLength = range.contentLength.toLong
    val responseHeader = ResponseHeader(Status.PARTIAL_CONTENT, Map(
      CONTENT_RANGE -> range.contentRange,
      CONTENT_LENGTH -> s"$contentLength",
      CONTENT_TYPE -> MimeTypes.forFileName(fileName).getOrElse(ContentTypes.BINARY)))
    implicit val ec = play.api.libs.concurrent.Execution.defaultContext
    val source = FileIO.fromPath(path)
      .drop(range.start.toLong)
      .take(contentLength)
    val streamedBody = HttpEntity.Streamed(source, Option(contentLength), Option(contentType))
    Result(responseHeader, streamedBody)
  }
}
