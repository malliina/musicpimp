package com.malliina.play

import com.malliina.storage.StorageSize
import play.api.http.HeaderNames._
import play.api.mvc.RequestHeader

import scala.util.{Failure, Try}

object ContentRanges {
  def fromHeaderOrAll(request: RequestHeader, size: StorageSize): ContentRange =
    fromHeader(request, size) getOrElse ContentRange.all(size)

  def fromHeader(request: RequestHeader, size: StorageSize): Try[ContentRange] = {
    request.headers.get(RANGE)
      .map(range => ContentRange.fromHeader(range, size))
      .getOrElse(Failure(new IllegalArgumentException(s"Missing $RANGE header.")))
  }
}
