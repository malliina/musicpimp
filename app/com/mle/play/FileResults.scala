package com.mle.play

import java.nio.file.{Files, Path}

import play.api.http.HeaderNames._
import play.api.http.{ContentTypes, Status}
import play.api.libs.MimeTypes
import play.api.libs.iteratee.{Enumerator, Traversable}
import play.api.mvc.{RequestHeader, ResponseHeader, Result, Results}

/**
 * @author Michael
 */
trait FileResults {
  def fileResult(path: Path, request: RequestHeader): Result = {
    val maybeRanged = for {
      rangeHeader <- request.headers.get(RANGE)
      parsedRange <- ContentRange.fromHeader(rangeHeader, Files.size(path).toInt).toOption
    } yield rangedResult(path, parsedRange)
    (maybeRanged getOrElse Results.Ok.sendFile(path.toFile)).withHeaders(ACCEPT_RANGES -> "bytes")
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
      val take = Traversable.take[Array[Byte]](range.endExclusive)
      Enumerator.fromFile(path.toFile) through drop through take
    })
  }
}

object FileResults extends FileResults
