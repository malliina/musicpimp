package com.malliina.play

import java.nio.file.{Files, Path}

import com.malliina.storage.StorageLong
import play.api.http.HeaderNames._
import play.api.http.{ContentTypes, Status}
import play.api.libs.MimeTypes
import play.api.libs.iteratee.{Enumerator, Traversable}
import play.api.mvc.{RequestHeader, ResponseHeader, Result, Results}

trait FileResults {
  def fileResult(path: Path, request: RequestHeader): Result = {
    ContentRange.fromHeader(request, Files.size(path).bytes).toOption
      .map(range => rangedResult(path, range))
      .getOrElse(Results.Ok.sendFile(path.toFile).withHeaders(ACCEPT_RANGES -> ContentRange.BYTES))
  }

  def rangedResult(path: Path, range: ContentRange): Result = {
    val fileName = path.getFileName.toString
    val responseHeader = ResponseHeader(Status.PARTIAL_CONTENT, Map(
      CONTENT_RANGE -> range.contentRange,
      CONTENT_LENGTH -> s"${range.contentLength}",
      CONTENT_TYPE -> MimeTypes.forFileName(fileName).getOrElse(ContentTypes.BINARY)))
    implicit val ec = play.api.libs.concurrent.Execution.defaultContext
    Result(responseHeader, {
      val drop = Traversable.drop[Array[Byte]](range.start)
      val take = Traversable.take[Array[Byte]](range.contentLength)
      Enumerator.fromFile(path.toFile) through drop through take
    })
  }
}

object FileResults extends FileResults
